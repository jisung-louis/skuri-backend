package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeReadResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeUnreadCountResponse;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberAppNoticeController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class MemberAppNoticeControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppNoticeService appNoticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getUnreadCount_정상조회_200() throws Exception {
        mockValidToken();
        when(appNoticeService.getUnreadCount("firebase-uid"))
                .thenReturn(new AppNoticeUnreadCountResponse(2));

        mockMvc.perform(
                        get("/v1/members/me/app-notices/unread-count")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));
    }

    @Test
    void getUnreadCount_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me/app-notices/unread-count"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void markRead_정상처리_200() throws Exception {
        mockValidToken();
        when(appNoticeService.markRead("firebase-uid", "app-notice-1"))
                .thenReturn(new AppNoticeReadResponse(
                        "app-notice-1",
                        true,
                        LocalDateTime.of(2026, 3, 26, 14, 30)
                ));

        mockMvc.perform(
                        post("/v1/members/me/app-notices/app-notice-1/read")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appNoticeId").value("app-notice-1"))
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    void markRead_공지없음_404() throws Exception {
        mockValidToken();
        when(appNoticeService.markRead("firebase-uid", "missing"))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/members/me/app-notices/missing/read")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_NOT_FOUND"));
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
