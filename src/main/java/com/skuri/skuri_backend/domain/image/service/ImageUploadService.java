package com.skuri.skuri_backend.domain.image.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.dto.request.ImageUploadContext;
import com.skuri.skuri_backend.domain.image.dto.response.ImageUploadResponse;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final StorageRepository storageRepository;
    private final ImageUploadProperties imageUploadProperties;

    public ImageUploadResponse upload(boolean admin, ImageUploadContext context, MultipartFile file) {
        validateAccess(context, admin);
        validateFile(file);
        validateSize(file.getSize());

        byte[] originalBytes = readBytes(file);
        DetectedImage detectedImage = detectImage(originalBytes);
        validateAllowedMimeType(detectedImage.format().mime());

        ThumbnailImage thumbnailImage = createThumbnail(detectedImage);
        String fileKey = UUID.randomUUID().toString();
        String basePath = buildBasePath(context);
        String originalPath = basePath + "/" + fileKey + "." + detectedImage.format().extension();
        String thumbnailPath = basePath + "/" + fileKey + "_thumb." + thumbnailImage.format().extension();

        try {
            StorageRepository.StoredObject originalObject =
                    storageRepository.store(originalPath, originalBytes, detectedImage.format().mime());
            StorageRepository.StoredObject thumbnailObject =
                    storageRepository.store(thumbnailPath, thumbnailImage.bytes(), thumbnailImage.format().mime());
            return new ImageUploadResponse(
                    originalObject.publicUrl(),
                    thumbnailObject.publicUrl(),
                    detectedImage.width(),
                    detectedImage.height(),
                    Math.toIntExact(file.getSize()),
                    detectedImage.format().mime()
            );
        } catch (RuntimeException e) {
            deleteQuietly(originalPath);
            deleteQuietly(thumbnailPath);
            throw mapUploadFailure(e);
        }
    }

    private void validateAccess(ImageUploadContext context, boolean admin) {
        if (context.adminOnly() && !admin) {
            throw new BusinessException(ErrorCode.ADMIN_REQUIRED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "file은 비어 있을 수 없습니다.");
        }
    }

    private void validateSize(long size) {
        if (size > imageUploadProperties.getMaxFileSizeBytes()) {
            throw new BusinessException(ErrorCode.IMAGE_TOO_LARGE);
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, "업로드 파일을 읽지 못했습니다.");
        }
    }

    private DetectedImage detectImage(byte[] originalBytes) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(originalBytes))) {
            if (imageInputStream == null) {
                throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                validateDimensions(width, height);
                BufferedImage bufferedImage = reader.read(0);
                if (bufferedImage == null) {
                    throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
                }
                ImageFormat format = ImageFormat.from(reader.getFormatName());
                return new DetectedImage(format, bufferedImage, width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
        }
    }

    private void validateDimensions(int width, int height) {
        long pixelCount = (long) width * height;
        if (width <= 0 || height <= 0) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
        }
        if (width > imageUploadProperties.getMaxWidth()
                || height > imageUploadProperties.getMaxHeight()
                || pixelCount > imageUploadProperties.getMaxPixelCount()) {
            throw new BusinessException(ErrorCode.IMAGE_DIMENSIONS_EXCEEDED);
        }
    }

    private void validateAllowedMimeType(String mimeType) {
        Set<String> allowedMimeTypes = imageUploadProperties.getAllowedMimeTypes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        if (!allowedMimeTypes.contains(mimeType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
        }
    }

    private ThumbnailImage createThumbnail(DetectedImage detectedImage) {
        ImageFormat thumbnailFormat = detectedImage.bufferedImage().getColorModel().hasAlpha()
                ? ImageFormat.PNG
                : ImageFormat.JPEG;
        double scale = detectedImage.width() <= imageUploadProperties.getThumbnailWidth()
                ? 1.0d
                : (double) imageUploadProperties.getThumbnailWidth() / detectedImage.width();

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Thumbnails.Builder<BufferedImage> builder = Thumbnails.of(detectedImage.bufferedImage())
                    .scale(scale)
                    .outputFormat(thumbnailFormat.outputFormatName());
            if (thumbnailFormat == ImageFormat.JPEG) {
                builder.outputQuality(imageUploadProperties.getThumbnailJpegQuality());
            }
            builder.toOutputStream(outputStream);
            return new ThumbnailImage(thumbnailFormat, outputStream.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, "썸네일 생성에 실패했습니다.");
        }
    }

    private String buildBasePath(ImageUploadContext context) {
        LocalDate today = LocalDate.now();
        return "%s/%04d/%02d/%02d".formatted(
                context.directoryName(),
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth()
        );
    }

    private void deleteQuietly(String relativePath) {
        try {
            storageRepository.delete(relativePath);
        } catch (RuntimeException ignored) {
            // 업로드 실패 시 orphan file 정리는 best-effort로 처리한다.
        }
    }

    private RuntimeException mapUploadFailure(RuntimeException e) {
        if (e instanceof BusinessException businessException) {
            return businessException;
        }
        if (e instanceof IllegalArgumentException illegalArgumentException) {
            return new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED, illegalArgumentException.getMessage());
        }
        if (e instanceof UncheckedIOException) {
            return new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
        return new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
    }

    private record DetectedImage(ImageFormat format, BufferedImage bufferedImage, int width, int height) {
    }

    private record ThumbnailImage(ImageFormat format, byte[] bytes) {
    }

    private enum ImageFormat {
        JPEG("image/jpeg", "jpg", "jpg"),
        PNG("image/png", "png", "png"),
        WEBP("image/webp", "webp", "webp");

        private final String mime;
        private final String extension;
        private final String outputFormatName;

        ImageFormat(String mime, String extension, String outputFormatName) {
            this.mime = mime;
            this.extension = extension;
            this.outputFormatName = outputFormatName;
        }

        public String mime() {
            return mime;
        }

        public String extension() {
            return extension;
        }

        public String outputFormatName() {
            return outputFormatName;
        }

        public static ImageFormat from(String formatName) {
            String normalized = formatName.toLowerCase(Locale.ROOT);
            return switch (normalized) {
                case "jpeg", "jpg" -> JPEG;
                case "png" -> PNG;
                case "webp" -> WEBP;
                default -> throw new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT);
            };
        }
    }
}
