package com.skuri.skuri_backend.domain.minecraft.controller;

import com.skuri.skuri_backend.domain.minecraft.config.MinecraftInternalSecretFilter;
import com.skuri.skuri_backend.domain.minecraft.service.MinecraftPublicSseService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MinecraftSseController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class,
        MinecraftInternalSecretFilter.class,
        MinecraftControllerTestConfig.class
})
class MinecraftSseControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MinecraftPublicSseService minecraftPublicSseService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void subscribe_정상연결_200() throws Exception {
        mockValidToken();
        SseEmitter emitter = new SseEmitter(5_000L);
        when(minecraftPublicSseService.subscribe("firebase-uid")).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get("/v1/sse/minecraft").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("SERVER_STATE_SNAPSHOT").data("{\"online\":true}"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
                .andExpect(content().string(containsString("event:SERVER_STATE_SNAPSHOT")));
    }

    @Test
    void subscribe_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/sse/minecraft"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void subscribe_async응답사용불가예외발생시_204() throws Exception {
        mockValidToken();
        SseEmitter emitter = new SseEmitter(5_000L);
        when(minecraftPublicSseService.subscribe("firebase-uid")).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get("/v1/sse/minecraft").header(AUTHORIZATION, "Bearer valid-token"))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.completeWithError(new AsyncRequestNotUsableException("Response not usable after response errors."));

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());

        verify(minecraftPublicSseService, times(1)).subscribe("firebase-uid");
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
