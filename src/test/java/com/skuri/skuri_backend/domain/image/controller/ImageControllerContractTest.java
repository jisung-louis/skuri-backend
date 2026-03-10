package com.skuri.skuri_backend.domain.image.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.image.dto.request.ImageUploadContext;
import com.skuri.skuri_backend.domain.image.dto.response.ImageUploadResponse;
import com.skuri.skuri_backend.domain.image.service.ImageUploadService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImageController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ImageControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageUploadService imageUploadService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void postImages_정상업로드_200_응답스키마검증() throws Exception {
        mockValidToken();
        when(imageUploadService.upload(eq(false), eq(ImageUploadContext.POST_IMAGE), any()))
                .thenReturn(new ImageUploadResponse(
                        "https://cdn.skuri.app/uploads/posts/2026/03/10/image.jpg",
                        "https://cdn.skuri.app/uploads/posts/2026/03/10/image_thumb.jpg",
                        800,
                        600,
                        245123,
                        "image/jpeg"
                ));

        mockMvc.perform(
                        multipart("/v1/images")
                                .file(new MockMultipartFile("file", "image.jpg", "image/jpeg", "test-image".getBytes()))
                                .param("context", "POST_IMAGE")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://cdn.skuri.app/uploads/posts/2026/03/10/image.jpg"))
                .andExpect(jsonPath("$.data.thumbUrl").value("https://cdn.skuri.app/uploads/posts/2026/03/10/image_thumb.jpg"))
                .andExpect(jsonPath("$.data.width").value(800))
                .andExpect(jsonPath("$.data.height").value(600))
                .andExpect(jsonPath("$.data.size").value(245123))
                .andExpect(jsonPath("$.data.mime").value("image/jpeg"));
    }

    @Test
    void postImages_미인증요청_401() throws Exception {
        mockMvc.perform(
                        multipart("/v1/images")
                                .file(new MockMultipartFile("file", "image.jpg", "image/jpeg", "test-image".getBytes()))
                                .param("context", "POST_IMAGE")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void postImages_APP_NOTICE_IMAGE_비관리자_403() throws Exception {
        mockValidToken();
        when(imageUploadService.upload(eq(false), eq(ImageUploadContext.APP_NOTICE_IMAGE), any()))
                .thenThrow(new BusinessException(ErrorCode.ADMIN_REQUIRED));

        mockMvc.perform(
                        multipart("/v1/images")
                                .file(new MockMultipartFile("file", "image.jpg", "image/jpeg", "test-image".getBytes()))
                                .param("context", "APP_NOTICE_IMAGE")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void postImages_잘못된형식_415() throws Exception {
        mockValidToken();
        when(imageUploadService.upload(eq(false), eq(ImageUploadContext.POST_IMAGE), any()))
                .thenThrow(new BusinessException(ErrorCode.IMAGE_INVALID_FORMAT));

        mockMvc.perform(
                        multipart("/v1/images")
                                .file(new MockMultipartFile("file", "image.txt", "text/plain", "not-image".getBytes()))
                                .param("context", "POST_IMAGE")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("IMAGE_INVALID_FORMAT"));
    }

    @Test
    void postImages_해상도초과_422() throws Exception {
        mockValidToken();
        when(imageUploadService.upload(eq(false), eq(ImageUploadContext.POST_IMAGE), any()))
                .thenThrow(new BusinessException(ErrorCode.IMAGE_DIMENSIONS_EXCEEDED));

                mockMvc.perform(
                        multipart("/v1/images")
                                .file(new MockMultipartFile("file", "image.png", "image/png", "test-image".getBytes()))
                                .param("context", "POST_IMAGE")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.errorCode").value("IMAGE_DIMENSIONS_EXCEEDED"));
    }

    @Test
    void postImages_잘못된context_400() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        multipart("/v1/images")
                                .file(new MockMultipartFile("file", "image.jpg", "image/jpeg", "test-image".getBytes()))
                                .param("context", "UNKNOWN_CONTEXT")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

        verifyNoInteractions(imageUploadService);
    }

    private void mockValidToken() {
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
}
