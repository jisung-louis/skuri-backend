package com.skuri.skuri_backend.domain.image.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImageUploadBoardFlowIntegrationTest {

    private static final Path STORAGE_DIR = createStorageDir();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("media.storage.base-dir", () -> STORAGE_DIR.toString());
        registry.add("media.storage.public-base-url", () -> "http://localhost/uploads");
        registry.add("media.storage.url-prefix", () -> "/uploads");
    }

    @BeforeEach
    void setUp() throws IOException {
        postRepository.deleteAll();
        memberRepository.deleteAll();
        clearStorage();

        Member member = Member.create("firebase-uid", "user@sungkyul.ac.kr", "홍길동", LocalDateTime.now());
        member.updateProfile("스쿠리유저", "20201234", "컴퓨터공학과", null);
        memberRepository.save(member);

        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    @Test
    void 업로드한이미지를_게시글생성에연결할수있다() throws Exception {
        byte[] imageBytes = createImageBytes();

        String uploadResponseBody = mockMvc.perform(
                        multipart("/v1/images")
                                .file("file", imageBytes)
                                .param("context", "POST_IMAGE")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").exists())
                .andExpect(jsonPath("$.data.thumbUrl").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode uploadData = objectMapper.readTree(uploadResponseBody).path("data");
        String url = uploadData.path("url").asText();
        String thumbUrl = uploadData.path("thumbUrl").asText();
        int width = uploadData.path("width").asInt();
        int height = uploadData.path("height").asInt();
        int size = uploadData.path("size").asInt();
        String mime = uploadData.path("mime").asText();

        mockMvc.perform(get(URI.create(url).getPath()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/v1/posts")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "이미지 업로드 연동 게시글",
                                          "content": "업로드 결과를 그대로 사용합니다.",
                                          "category": "GENERAL",
                                          "isAnonymous": false,
                                          "images": [
                                            {
                                              "url": "%s",
                                              "thumbUrl": "%s",
                                              "width": %d,
                                              "height": %d,
                                              "size": %d,
                                              "mime": "%s"
                                            }
                                          ]
                                        }
                                        """.formatted(url, thumbUrl, width, height, size, mime))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.images[0].url").value(url))
                .andExpect(jsonPath("$.data.images[0].thumbUrl").value(thumbUrl))
                .andExpect(jsonPath("$.data.images[0].mime").value(mime));

        String savedPostId = postRepository.findAll().stream().map(Post::getId).findFirst().orElseThrow();
        Post savedPost = postRepository.findActiveDetailById(savedPostId).orElseThrow();
        assertEquals(1, savedPost.getImages().size());
        assertEquals(url, savedPost.getImages().get(0).getUrl());
        assertEquals(thumbUrl, savedPost.getImages().get(0).getThumbUrl());
        assertTrue(Files.exists(STORAGE_DIR.resolve(URI.create(url).getPath().replaceFirst("^/uploads/", ""))));
    }

    private static Path createStorageDir() {
        try {
            return Files.createTempDirectory("skuri-image-upload-it");
        } catch (IOException e) {
            throw new IllegalStateException("test storage directory를 생성하지 못했습니다.", e);
        }
    }

    private void clearStorage() throws IOException {
        if (!Files.exists(STORAGE_DIR)) {
            Files.createDirectories(STORAGE_DIR);
            return;
        }
        try (var stream = Files.walk(STORAGE_DIR)) {
            stream.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(STORAGE_DIR))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new IllegalStateException("test storage cleanup에 실패했습니다.", e);
                        }
                    });
        }
        Files.createDirectories(STORAGE_DIR);
    }

    private byte[] createImageBytes() throws IOException {
        BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, new Color((x * 9) % 255, (y * 7) % 255, (x + y) % 255).getRGB());
            }
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }
}
