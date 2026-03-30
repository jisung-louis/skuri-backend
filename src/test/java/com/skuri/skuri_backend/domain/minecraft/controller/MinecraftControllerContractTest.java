package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftOverviewResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftReadService;
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

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MinecraftController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class,
        MinecraftControllerTestConfig.class
})
class MinecraftControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MinecraftReadService minecraftReadService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getOverview_정상조회_200() throws Exception {
        mockValidToken();
        when(minecraftReadService.getOverview("firebase-uid")).thenReturn(new MinecraftOverviewResponse(
                "public:game:minecraft",
                true,
                12,
                50,
                "1.21.1",
                "mc.skuri.app",
                "https://map.skuri.app",
                Instant.parse("2026-03-30T13:20:00Z")
        ));

        mockMvc.perform(get("/v1/minecraft/overview").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value("public:game:minecraft"))
                .andExpect(jsonPath("$.data.online").value(true));
    }

    @Test
    void getOverview_서버상태없음_503() throws Exception {
        mockValidToken();
        when(minecraftReadService.getOverview("firebase-uid"))
                .thenThrow(new BusinessException(ErrorCode.MINECRAFT_SERVER_UNAVAILABLE));

        mockMvc.perform(get("/v1/minecraft/overview").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("MINECRAFT_SERVER_UNAVAILABLE"));
    }

    @Test
    void getPlayers_정상조회_200() throws Exception {
        mockValidToken();
        when(minecraftReadService.getPlayers("firebase-uid")).thenReturn(List.of(new MinecraftPlayerResponse(
                "account-1",
                "member-1",
                MinecraftAccountRole.SELF,
                MinecraftEdition.JAVA,
                "skuriPlayer",
                "8667ba71b85a4004af54457a9734eed7",
                "8667ba71b85a4004af54457a9734eed7",
                null,
                true,
                Instant.parse("2026-03-30T13:18:00Z")
        )));

        mockMvc.perform(get("/v1/minecraft/players").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gameName").value("skuriPlayer"))
                .andExpect(jsonPath("$.data[0].online").value(true));
    }

    @Test
    void getOverview_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/minecraft/overview"))
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
