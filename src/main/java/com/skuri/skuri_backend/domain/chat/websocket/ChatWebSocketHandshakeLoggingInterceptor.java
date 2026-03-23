package com.skuri.skuri_backend.domain.chat.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChatWebSocketHandshakeLoggingInterceptor implements HandshakeInterceptor {

    private static final String WEBSOCKET_PROTOCOL_HEADER = "Sec-WebSocket-Protocol";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        HttpHeaders headers = request.getHeaders();
        log.info(
                "Chat WebSocket handshake start: uri={}, origin={}, remoteAddress={}, userAgent={}, protocol={}, hasAuthorizationHeader={}",
                request.getURI(),
                headers.getOrigin(),
                request.getRemoteAddress(),
                trimHeader(headers.getFirst(HttpHeaders.USER_AGENT)),
                joinHeaderValues(headers.get(WEBSOCKET_PROTOCOL_HEADER)),
                StringUtils.hasText(headers.getFirst(HttpHeaders.AUTHORIZATION))
        );
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        if (exception != null) {
            log.warn(
                    "Chat WebSocket handshake failed: uri={}, origin={}, message={}",
                    request.getURI(),
                    request.getHeaders().getOrigin(),
                    exception.getMessage(),
                    exception
            );
            return;
        }

        log.info(
                "Chat WebSocket handshake completed: uri={}, origin={}",
                request.getURI(),
                request.getHeaders().getOrigin()
        );
    }

    private String joinHeaderValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }

    private String trimHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (value.length() <= 120) {
            return value;
        }
        return value.substring(0, 120) + "...";
    }
}
