package com.skuri.skuri_backend.infra.auth;

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
        "media.storage.url-prefix=uploads"
})
class SecurityInfraCustomUploadPrefixIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppVersionService appVersionService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void uploadsPath_prefix에슬래시가없어도_기동되고허용된다() throws Exception {
        mockMvc.perform(get("/uploads/test.jpg"))
                .andExpect(status().isNotFound());
    }
}
