package com.skuri.skuri_backend.infra.auth;

import com.skuri.skuri_backend.domain.app.controller.AppNoticeController;
import com.skuri.skuri_backend.domain.app.controller.AppVersionController;
import com.skuri.skuri_backend.domain.member.controller.MemberController;
import com.skuri.skuri_backend.domain.member.dto.response.MemberMeResponse;
import com.skuri.skuri_backend.domain.member.dto.response.MemberNotificationSettingResponse;
import com.skuri.skuri_backend.domain.member.service.MemberService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        MemberController.class,
        AppVersionController.class,
        AppNoticeController.class
})
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void 보호Api_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_membersById_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/members/target-uid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void 보호Api_도메인불일치_403() throws Exception {
        when(firebaseTokenVerifier.verify("invalid-domain-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@gmail.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer invalid-domain-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_DOMAIN_RESTRICTED"));
    }

    @Test
    void 보호Api_정상토큰_접근가능() throws Exception {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
        when(memberService.getMyProfile("firebase-uid"))
                .thenReturn(new MemberMeResponse(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "홍길동",
                        "20201234",
                        "컴퓨터공학과",
                        "https://example.com/profile.jpg",
                        "홍길동",
                        false,
                        null,
                        new MemberNotificationSettingResponse(
                                true,
                                true,
                                true,
                                true,
                                true,
                                true,
                                Map.of("news", true)
                        ),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(
                        get("/v1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("firebase-uid"));

        verify(firebaseTokenVerifier).verify("valid-token");
    }

    @Test
    void 공개Api_인증없이_접근가능() throws Exception {
        mockMvc.perform(get("/v1/app-notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/v1/app-versions/ios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.platform").value("ios"));
    }
}
