package com.skuri.skuri_backend.domain.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

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
                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                sessionRegistry.unregisterSession(session.getId());
                super.afterConnectionClosed(session, closeStatus);
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                sessionRegistry.unregisterSession(session.getId());
                super.handleTransportError(session, exception);
            }
        };
    }
}
