package com.skuri.skuri_backend.domain.notification.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationListResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import com.skuri.skuri_backend.domain.notification.service.FcmTokenService;
import com.skuri.skuri_backend.domain.notification.service.NotificationService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {NotificationController.class, FcmTokenController.class})
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class NotificationControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private FcmTokenService fcmTokenService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getNotifications_정상조회_200() throws Exception {
        mockValidToken();
        when(notificationService.getNotifications("firebase-uid", false, 0, 20))
                .thenReturn(new NotificationListResponse(
                        List.of(new NotificationResponse(
                                "notification-1",
                                NotificationType.PARTY_JOIN_ACCEPTED,
                                "동승 요청이 승인되었어요",
                                "파티에 합류하세요!",
                                NotificationData.ofPartyRequest("party-1", "request-1"),
                                false,
                                LocalDateTime.of(2026, 3, 8, 9, 0)
                        )),
                        0,
                        20,
                        1,
                        1,
                        false,
                        false,
                        5
                ));

        mockMvc.perform(
                        get("/v1/notifications")
                                .queryParam("page", "0")
                                .queryParam("size", "20")
                                .queryParam("unreadOnly", "false")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("PARTY_JOIN_ACCEPTED"))
                .andExpect(jsonPath("$.data.content[0].data.partyId").value("party-1"))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    void getNotifications_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getUnreadCount_정상조회_200() throws Exception {
        mockValidToken();
        when(notificationService.getUnreadCount("firebase-uid"))
                .thenReturn(new NotificationUnreadCountResponse(3));

        mockMvc.perform(
                        get("/v1/notifications/unread-count")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
    }

    @Test
    void markRead_타인알림이면_403() throws Exception {
        mockValidToken();
        when(notificationService.markRead("firebase-uid", "notification-1"))
                .thenThrow(new BusinessException(ErrorCode.NOT_NOTIFICATION_OWNER));

        mockMvc.perform(
                        post("/v1/notifications/notification-1/read")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_NOTIFICATION_OWNER"));
    }

    @Test
    void markAllRead_정상처리_200() throws Exception {
        mockValidToken();
        when(notificationService.markAllRead("firebase-uid"))
                .thenReturn(new NotificationReadAllResponse(2, 0));

        mockMvc.perform(
                        post("/v1/notifications/read-all")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(2))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void deleteNotification_알림없음_404() throws Exception {
        mockValidToken();
        doThrow(new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND))
                .when(notificationService).delete("firebase-uid", "notification-404");

        mockMvc.perform(
                        delete("/v1/notifications/notification-404")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    void registerFcmToken_정상처리_200() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/members/me/fcm-tokens")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "token": "dXZlbnQ6ZmNtLXRva2Vu",
                                          "platform": "ios"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(fcmTokenService).register("firebase-uid", "dXZlbnQ6ZmNtLXRva2Vu", "ios");
    }

    @Test
    void registerFcmToken_유효성실패_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/members/me/fcm-tokens")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "token": "",
                                          "platform": "web"
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void deleteFcmToken_토큰없음_401() throws Exception {
        mockMvc.perform(
                        delete("/v1/members/me/fcm-tokens")
                                .contentType("application/json")
                                .content("""
                                        {
                                          "token": "dXZlbnQ6ZmNtLXRva2Vu"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
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
