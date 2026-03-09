package com.skuri.skuri_backend.domain.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ChatSubscriptionAccessInterceptor implements ChannelInterceptor {

    private static final String TOPIC_CHAT_DESTINATION_PREFIX = "/topic/chat/";

    private final ChatWebSocketSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (accessor.getMessageType() != SimpMessageType.MESSAGE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (!StringUtils.hasText(destination) || !destination.startsWith(TOPIC_CHAT_DESTINATION_PREFIX)) {
            return message;
        }

        String sessionId = accessor.getSessionId();
        if (!StringUtils.hasText(sessionId)) {
            return message;
        }

        return sessionRegistry.isRevokedSession(sessionId) ? null : message;
    }
}
