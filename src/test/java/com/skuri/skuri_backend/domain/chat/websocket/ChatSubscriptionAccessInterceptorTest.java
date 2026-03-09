package com.skuri.skuri_backend.domain.chat.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatSubscriptionAccessInterceptorTest {

    private ChatWebSocketSessionRegistry sessionRegistry;
    private ChatSubscriptionAccessInterceptor interceptor;

    @BeforeEach
    void setUp() {
        sessionRegistry = new ChatWebSocketSessionRegistry();
        interceptor = new ChatSubscriptionAccessInterceptor(sessionRegistry);
    }

    @Test
    void revoked회원세션이면_topicChat메시지를차단한다() {
        sessionRegistry.registerAuthenticatedSession("withdrawn-member", "session-1");
        sessionRegistry.revokeMember("withdrawn-member");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setSessionId("session-1");
        accessor.setDestination("/topic/chat/room-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertNull(result);
    }

    @Test
    void 일반세션이면_topicChat메시지를통과시킨다() {
        sessionRegistry.registerAuthenticatedSession("active-member", "session-2");

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setSessionId("session-2");
        accessor.setDestination("/topic/chat/room-1");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, null);

        assertNotNull(result);
    }
}
