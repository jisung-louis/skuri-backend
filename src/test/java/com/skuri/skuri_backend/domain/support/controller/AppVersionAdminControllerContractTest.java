package com.skuri.skuri_backend.domain.support.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionAdminUpdateResponse;
import com.skuri.skuri_backend.domain.support.service.AppVersionService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppVersionAdminController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class AppVersionAdminControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppVersionService appVersionService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void upsertAppVersion_관리자정상요청_200() throws Exception {
        mockToken("admin-token", true);
        when(appVersionService.upsertAppVersion(org.mockito.ArgumentMatchers.eq("ios"), any()))
                .thenReturn(new AppVersionAdminUpdateResponse(
                        "ios",
                        "1.6.0",
                        true,
                        LocalDateTime.of(2026, 2, 19, 12, 0)
                ));

        mockMvc.perform(
                        put("/v1/admin/app-versions/ios")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "minimumVersion": "1.6.0",
                                          "forceUpdate": true,
                                          "title": "필수 업데이트 안내",
                                          "message": "안정성 개선을 위한 필수 업데이트입니다.",
                                          "showButton": true,
                                          "buttonText": "업데이트",
                                          "buttonUrl": "https://apps.apple.com/..."
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.platform").value("ios"));
    }

    @Test
    void upsertAppVersion_비관리자요청_403() throws Exception {
        mockToken("user-token", false);

        mockMvc.perform(
                        put("/v1/admin/app-versions/ios")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequest())
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void upsertAppVersion_잘못된플랫폼_400() throws Exception {
        mockToken("admin-token", true);
        when(appVersionService.upsertAppVersion(org.mockito.ArgumentMatchers.eq("windows"), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "지원하지 않는 platform입니다."));

        mockMvc.perform(
                        put("/v1/admin/app-versions/windows")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content(validRequest())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void upsertAppVersion_검증실패_422() throws Exception {
        mockToken("admin-token", true);

        mockMvc.perform(
                        put("/v1/admin/app-versions/ios")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "minimumVersion": "",
                                          "forceUpdate": true,
                                          "title": "필수 업데이트 안내",
                                          "message": "안정성 개선을 위한 필수 업데이트입니다.",
                                          "showButton": true,
                                          "buttonText": "",
                                          "buttonUrl": null
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private String validRequest() {
        return """
                {
                  "minimumVersion": "1.6.0",
                  "forceUpdate": true,
                  "title": "필수 업데이트 안내",
                  "message": "안정성 개선을 위한 필수 업데이트입니다.",
                  "showButton": true,
                  "buttonText": "업데이트",
                  "buttonUrl": "https://apps.apple.com/..."
                }
                """;
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
}
