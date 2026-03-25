package com.skuri.skuri_backend.infra.auth.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import static org.assertj.core.api.Assertions.assertThat;

class SseDisconnectRequestSupportTest {

    @Test
    void 끊긴SseErrorDispatch면_true() {
        MockHttpServletRequest request = errorDispatchRequest(
                "/v1/sse/members/me/join-requests",
                new AsyncRequestNotUsableException("Response not usable after response errors.")
        );

        assertThat(SseDisconnectRequestSupport.isDisconnectedSseErrorDispatch(request)).isTrue();
    }

    @Test
    void brokenPipe메시지도_끊긴SseErrorDispatch로인식한다() {
        MockHttpServletRequest request = errorDispatchRequest(
                "/v1/sse/notifications",
                new IllegalStateException("Broken pipe")
        );

        assertThat(SseDisconnectRequestSupport.isDisconnectedSseErrorDispatch(request)).isTrue();
    }

    @Test
    void 일반ApiErrorDispatch면_false() {
        MockHttpServletRequest request = errorDispatchRequest(
                "/v1/members/me",
                new AsyncRequestNotUsableException("Response not usable after response errors.")
        );

        assertThat(SseDisconnectRequestSupport.isDisconnectedSseErrorDispatch(request)).isFalse();
    }

    @Test
    void 연결끊김원인이없는SseErrorDispatch면_false() {
        MockHttpServletRequest request = errorDispatchRequest(
                "/v1/sse/parties",
                new IllegalStateException("boom")
        );

        assertThat(SseDisconnectRequestSupport.isDisconnectedSseErrorDispatch(request)).isFalse();
    }

    @Test
    void request디스패치가아니면_false() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/sse/parties");
        request.setDispatcherType(DispatcherType.REQUEST);

        assertThat(SseDisconnectRequestSupport.isDisconnectedSseErrorDispatch(request)).isFalse();
    }

    private MockHttpServletRequest errorDispatchRequest(String originalRequestUri, Throwable throwable) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/error");
        request.setDispatcherType(DispatcherType.ERROR);
        request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, originalRequestUri);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, throwable);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, throwable.getMessage());
        return request;
    }
}
