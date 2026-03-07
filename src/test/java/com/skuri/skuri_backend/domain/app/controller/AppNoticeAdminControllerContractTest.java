package com.skuri.skuri_backend.domain.app.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.app.dto.request.CreateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.request.UpdateAppNoticeRequest;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeCreateResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.service.AppNoticeService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppNoticeAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AppNoticeAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppNoticeService appNoticeService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void createAppNotice_관리자정상요청_201() throws Exception {
        mockToken("admin-token", true);
        when(appNoticeService.createAppNotice(any(CreateAppNoticeRequest.class)))
                .thenReturn(new AppNoticeCreateResponse("app-notice-1", "서버 점검 안내", LocalDateTime.of(2026, 2, 19, 12, 0)));

        mockMvc.perform(
                        post("/v1/admin/app-notices")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "서버 점검 안내",
                                          "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
                                          "category": "MAINTENANCE",
                                          "priority": "HIGH",
                                          "imageUrls": [],
                                          "actionUrl": null,
                                          "publishedAt": "2026-02-20T00:00:00"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("app-notice-1"));
    }

    @Test
    void createAppNotice_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        post("/v1/admin/app-notices")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "서버 점검 안내",
                                          "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
                                          "category": "MAINTENANCE",
                                          "priority": "HIGH",
                                          "imageUrls": [],
                                          "actionUrl": null,
                                          "publishedAt": "2026-02-20T00:00:00"
                                        }
                                        """)
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void createAppNotice_요청검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        post("/v1/admin/app-notices")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "",
                                          "content": "",
                                          "category": null,
                                          "priority": null,
                                          "publishedAt": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void updateAppNotice_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(appNoticeService.updateAppNotice(eq("app-notice-1"), any(UpdateAppNoticeRequest.class)))
                .thenReturn(appNoticeResponse());

        mockMvc.perform(
                        patch("/v1/admin/app-notices/app-notice-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "title": "서버 점검 안내 (수정)",
                                          "priority": "HIGH"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("app-notice-1"));
    }

    @Test
    void updateAppNotice_없는공지_404() throws Exception {
        mockToken("admin-token", true);
        when(appNoticeService.updateAppNotice(eq("app-notice-1"), any(UpdateAppNoticeRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.APP_NOTICE_NOT_FOUND));

        mockMvc.perform(
                        patch("/v1/admin/app-notices/app-notice-1")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("APP_NOTICE_NOT_FOUND"));
    }

    @Test
    void deleteAppNotice_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(delete("/v1/admin/app-notices/app-notice-1").header(AUTHORIZATION, "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void mockToken(String token, boolean admin) {
        String uid = admin ? "admin-uid" : "user-uid";
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        admin ? "관리자" : "일반유저",
                        "https://example.com/profile.jpg"
                ));
        if (!admin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member member = Member.create(uid, uid + "@sungkyul.ac.kr", "관리자", LocalDateTime.now());
        ReflectionTestUtils.setField(member, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(member));
    }

    private AppNoticeResponse appNoticeResponse() {
        return new AppNoticeResponse(
                "app-notice-1",
                "서버 점검 안내",
                "점검 시간이 변경되었습니다.",
                AppNoticeCategory.MAINTENANCE,
                AppNoticePriority.HIGH,
                List.of(),
                null,
                LocalDateTime.of(2026, 2, 20, 0, 0),
                LocalDateTime.of(2026, 2, 19, 12, 0),
                LocalDateTime.of(2026, 2, 19, 12, 30)
        );
    }
}
