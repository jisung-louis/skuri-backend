package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.dto.request.ImageUploadContext;
import com.skuri.skuri_backend.domain.image.dto.response.ImageUploadResponse;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import com.skuri.skuri_backend.infra.storage.LocalStorageRepository;
import com.skuri.skuri_backend.infra.storage.MediaStorageProperties;
import com.skuri.skuri_backend.infra.storage.StorageProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageUploadServiceTest {

    @TempDir
    Path tempDir;

    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() {
        MediaStorageProperties mediaStorageProperties = new MediaStorageProperties();
        mediaStorageProperties.setProvider(StorageProviderType.LOCAL);
        mediaStorageProperties.setBaseDir(tempDir.toString());
        mediaStorageProperties.setPublicBaseUrl("https://cdn.skuri.app/uploads");
        mediaStorageProperties.setUrlPrefix("/uploads");

        ImageUploadProperties imageUploadProperties = new ImageUploadProperties();
        imageUploadProperties.setMaxFileSizeBytes(10 * 1024 * 1024);
        imageUploadProperties.setThumbnailWidth(300);
        imageUploadProperties.setThumbnailJpegQuality(0.8d);

        StorageRepository storageRepository = new LocalStorageRepository(mediaStorageProperties);
        imageUploadService = new ImageUploadService(storageRepository, imageUploadProperties, mediaStorageProperties);
    }

    @Test
    void upload_정상업로드_원본썸네일과메타데이터를반환한다() throws Exception {
        byte[] imageBytes = createImageBytes("jpg", 800, 600, BufferedImage.TYPE_INT_RGB);
        MockMultipartFile file = new MockMultipartFile("file", "sample.jpg", "image/jpeg", imageBytes);

        ImageUploadResponse response = imageUploadService.upload(false, ImageUploadContext.POST_IMAGE, file);

        assertEquals(800, response.width());
        assertEquals(600, response.height());
        assertEquals(imageBytes.length, response.size());
        assertEquals("image/jpeg", response.mime());
        assertTrue(response.url().contains("/posts/"));
        assertTrue(response.thumbUrl().contains("/posts/"));

        Path originalPath = toStoredPath(response.url());
        Path thumbPath = toStoredPath(response.thumbUrl());
        assertTrue(Files.exists(originalPath));
        assertTrue(Files.exists(thumbPath));

        BufferedImage thumb = ImageIO.read(thumbPath.toFile());
        assertNotNull(thumb);
        assertTrue(thumb.getWidth() <= 300);
    }

    @Test
    void upload_APP_NOTICE_IMAGE_비관리자면_ADMIN_REQUIRED() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.jpg", "image/jpeg", createImageBytes("jpg", 100, 100, BufferedImage.TYPE_INT_RGB));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> imageUploadService.upload(false, ImageUploadContext.APP_NOTICE_IMAGE, file)
        );

        assertEquals(ErrorCode.ADMIN_REQUIRED, exception.getErrorCode());
    }

    @Test
    void upload_잘못된형식이면_IMAGE_INVALID_FORMAT() {
        MockMultipartFile file = new MockMultipartFile("file", "sample.txt", "text/plain", "not-image".getBytes());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> imageUploadService.upload(false, ImageUploadContext.POST_IMAGE, file)
        );

        assertEquals(ErrorCode.IMAGE_INVALID_FORMAT, exception.getErrorCode());
    }

    @Test
    void upload_크기초과면_IMAGE_TOO_LARGE() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "huge.jpg",
                "image/jpeg",
                new byte[(10 * 1024 * 1024) + 1]
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> imageUploadService.upload(false, ImageUploadContext.POST_IMAGE, file)
        );

        assertEquals(ErrorCode.IMAGE_TOO_LARGE, exception.getErrorCode());
    }

    @ParameterizedTest
    @CsvSource({
            "POST_IMAGE,false,/posts/",
            "CHAT_IMAGE,false,/chat/",
            "PROFILE_IMAGE,false,/profiles/",
            "APP_NOTICE_IMAGE,true,/app-notices/"
    })
    void upload_context별_경로prefix를적용한다(ImageUploadContext context, boolean admin, String expectedSegment) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "sample.png", "image/png", createImageBytes("png", 120, 120, BufferedImage.TYPE_INT_ARGB));

        ImageUploadResponse response = imageUploadService.upload(admin, context, file);

        assertTrue(response.url().contains(expectedSegment));
        assertTrue(response.thumbUrl().contains(expectedSegment));
    }

    private Path toStoredPath(String url) {
        String path = URI.create(url).getPath();
        String relativePath = path.replaceFirst("^/uploads/", "");
        return tempDir.resolve(relativePath);
    }

    private byte[] createImageBytes(String formatName, int width, int height, int imageType) throws IOException {
        BufferedImage image = new BufferedImage(width, height, imageType);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, new Color((x * 17) % 255, (y * 23) % 255, (x + y) % 255, 255).getRGB());
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, outputStream);
        return outputStream.toByteArray();
    }
}
