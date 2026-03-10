package com.skuri.skuri_backend.infra.auth;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.support.controller.AppVersionController;
import com.skuri.skuri_backend.domain.support.service.AppVersionService;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.auth.config.SecurityConfig;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseAuthenticationFilter;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AppVersionController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
@TestPropertySource(properties = {
        "app.openapi.enabled=true",
        "media.storage.provider=FIREBASE",
        "media.storage.url-prefix=/uploads"
})
class SecurityInfraFirebaseStoragePathIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppVersionService appVersionService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void uploadsPath_FIREBASEprovider에서는_인증없이는허용되지않는다() throws Exception {
        mockMvc.perform(get("/uploads/test.jpg"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadsPath_FIREBASEprovider에서는_잘못된Bearer면토큰검증후_401() throws Exception {
        when(firebaseTokenVerifier.verify("invalid-token"))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        mockMvc.perform(get("/uploads/test.jpg")
                        .header(AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verify(firebaseTokenVerifier).verify("invalid-token");
    }
}
