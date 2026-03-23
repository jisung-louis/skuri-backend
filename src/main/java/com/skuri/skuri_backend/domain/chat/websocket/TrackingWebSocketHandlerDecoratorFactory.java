package com.skuri.skuri_backend.domain.chat.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingWebSocketHandlerDecoratorFactory implements WebSocketHandlerDecoratorFactory {

    private final ChatWebSocketSessionRegistry sessionRegistry;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                sessionRegistry.registerTransportSession(session);
                log.info(
                        "Chat WebSocket transport opened: sessionId={}, uri={}, remoteAddress={}, acceptedProtocol={}, hasAuthorizationHeader={}",
                        session.getId(),
                        session.getUri(),
                        session.getRemoteAddress(),
                        session.getAcceptedProtocol(),
                        session.getHandshakeHeaders().getFirst("Authorization") != null
                );
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessionRegistry.unregisterSession(session.getId());
                log.info(
                        "Chat WebSocket transport closed: sessionId={}, uri={}, statusCode={}, reason={}",
                        session.getId(),
                        session.getUri(),
                        closeStatus.getCode(),
                        closeStatus.getReason()
                );
                super.afterConnectionClosed(session, closeStatus);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                sessionRegistry.unregisterSession(session.getId());
                log.warn(
                        "Chat WebSocket transport error: sessionId={}, uri={}, message={}",
                        session != null ? session.getId() : null,
                        session != null ? session.getUri() : null,
                        exception.getMessage(),
                        exception
                );
                super.handleTransportError(session, exception);
            }
        };
    }
}
