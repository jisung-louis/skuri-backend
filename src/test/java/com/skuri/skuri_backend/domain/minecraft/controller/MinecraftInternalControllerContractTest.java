package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftBridgeService;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftInternalSseService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MinecraftInternalController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class,
        MinecraftControllerTestConfig.class
})
@TestPropertySource(properties = "minecraft.bridge.shared-secret=test-secret")
class MinecraftInternalControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MinecraftBridgeService minecraftBridgeService;

    @MockitoBean
    private MinecraftInternalSseService minecraftInternalSseService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void postChatMessage_유효한Secret_200() throws Exception {
        mockMvc.perform(post("/internal/minecraft/chat/messages")
                        .header("X-Skuri-Minecraft-Secret", "test-secret")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "9fa37c63-2c5a-4d1d-8a28-55b72750e79d",
                                  "eventType": "CHAT",
                                  "senderName": "skuriPlayer",
                                  "minecraftUuid": "8667ba71b85a4004af54457a9734eed7",
                                  "edition": "JAVA",
                                  "text": "안녕하세요!",
                                  "occurredAt": "2026-03-30T13:20:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(minecraftBridgeService).handleIncomingChatMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void postChatMessage_잘못된Secret_403() throws Exception {
        mockMvc.perform(post("/internal/minecraft/chat/messages")
                        .header("X-Skuri-Minecraft-Secret", "wrong-secret")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId": "9fa37c63-2c5a-4d1d-8a28-55b72750e79d",
                                  "eventType": "CHAT",
                                  "senderName": "skuriPlayer",
                                  "minecraftUuid": "8667ba71b85a4004af54457a9734eed7",
                                  "edition": "JAVA",
                                  "text": "안녕하세요!",
                                  "occurredAt": "2026-03-30T13:20:00Z"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("MINECRAFT_SECRET_INVALID"));
    }

    @Test
    void stream_유효한Secret_200() throws Exception {
        SseEmitter emitter = new SseEmitter(5_000L);
        when(minecraftInternalSseService.subscribe(null)).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get("/internal/minecraft/stream")
                        .header("X-Skuri-Minecraft-Secret", "test-secret"))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("WHITELIST_SNAPSHOT").data("{\"players\":[]}"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
                .andExpect(content().string(containsString("event:WHITELIST_SNAPSHOT")));
    }
}
