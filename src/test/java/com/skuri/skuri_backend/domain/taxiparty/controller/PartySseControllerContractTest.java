package com.skuri.skuri_backend.domain.taxiparty.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartySseService;
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

@WebMvcTest(controllers = PartySseController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class PartySseControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PartySseService partySseService;

    @MockitoBean
    private JoinRequestSseService joinRequestSseService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void subscribeParties_정상연결_200() throws Exception {
        mockValidToken();
        SseEmitter emitter = new SseEmitter(5_000L);
        when(partySseService.subscribeParties("firebase-uid")).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(
                        get("/v1/sse/parties")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("SNAPSHOT").data("{\"parties\":[]}"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
                .andExpect(content().string(containsString("event:SNAPSHOT")));
    }

    @Test
    void subscribeParties_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/sse/parties"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void subscribeParties_서비스권한예외_403() throws Exception {
        mockValidToken();
        when(partySseService.subscribeParties("firebase-uid"))
                .thenThrow(new BusinessException(ErrorCode.NOT_PARTY_MEMBER));

        mockMvc.perform(
                        get("/v1/sse/parties")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_PARTY_MEMBER"));
    }

    @Test
    void subscribeParties_async응답사용불가예외발생시_204_및_재구독없음() throws Exception {
        mockValidToken();
        SseEmitter emitter = new SseEmitter(5_000L);
        when(partySseService.subscribeParties("firebase-uid")).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(
                        get("/v1/sse/parties")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.completeWithError(new AsyncRequestNotUsableException("Response not usable after response errors."));

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isNoContent());

        verify(partySseService, times(1)).subscribeParties("firebase-uid");
    }

    @Test
    void subscribePartyJoinRequests_정상연결_200() throws Exception {
        mockValidToken();
        SseEmitter emitter = new SseEmitter(5_000L);
        when(joinRequestSseService.subscribePartyJoinRequests("firebase-uid", "party-1")).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(
                        get("/v1/sse/parties/party-1/join-requests")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("SNAPSHOT").data("{\"partyId\":\"party-1\",\"requests\":[]}"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
                .andExpect(content().string(containsString("event:SNAPSHOT")));
    }

    @Test
    void subscribePartyJoinRequests_리더아님_403() throws Exception {
        mockValidToken();
        when(joinRequestSseService.subscribePartyJoinRequests("firebase-uid", "party-1"))
                .thenThrow(new BusinessException(ErrorCode.NOT_PARTY_LEADER));

        mockMvc.perform(
                        get("/v1/sse/parties/party-1/join-requests")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_PARTY_LEADER"));
    }

    @Test
    void subscribePartyJoinRequests_파티없음_404() throws Exception {
        mockValidToken();
        when(joinRequestSseService.subscribePartyJoinRequests("firebase-uid", "party-1"))
                .thenThrow(new BusinessException(ErrorCode.PARTY_NOT_FOUND));

        mockMvc.perform(
                        get("/v1/sse/parties/party-1/join-requests")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("PARTY_NOT_FOUND"));
    }

    @Test
    void subscribePartyJoinRequests_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/sse/parties/party-1/join-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void subscribeMyJoinRequests_정상연결_200() throws Exception {
        mockValidToken();
        SseEmitter emitter = new SseEmitter(5_000L);
        when(joinRequestSseService.subscribeMyJoinRequests("firebase-uid", JoinRequestStatus.PENDING)).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(
                        get("/v1/sse/members/me/join-requests")
                                .queryParam("status", "PENDING")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.send(SseEmitter.event().name("SNAPSHOT").data("{\"requests\":[]}"));
        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM_VALUE))
                .andExpect(content().string(containsString("event:SNAPSHOT")));
    }

    @Test
    void subscribeMyJoinRequests_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/sse/members/me/join-requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void subscribeMyJoinRequests_status파라미터오류_400() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        get("/v1/sse/members/me/join-requests")
                                .queryParam("status", "INVALID_VALUE")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
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
