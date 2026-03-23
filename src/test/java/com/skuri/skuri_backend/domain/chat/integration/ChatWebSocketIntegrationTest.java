package com.skuri.skuri_backend.domain.chat.integration;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenClaims;
import com.skuri.skuri_backend.infra.auth.firebase.FirebaseTokenVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@ActiveProfiles("test")
class ChatWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        Transport webSocketTransport = new WebSocketTransport(new StandardWebSocketClient());
        SockJsClient sockJsClient = new SockJsClient(List.of(webSocketTransport));
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        chatMessageRepository.deleteAllInBatch();
        chatRoomMemberRepository.deleteAllInBatch();
        chatRoomRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();

        Member member = Member.create(
                "ws-member",
                "ws-member@sungkyul.ac.kr",
                "웹소켓테스터",
                LocalDateTime.now().minusDays(1)
        );
        memberRepository.save(member);

        ChatRoom room = ChatRoom.create(
                "room-ws",
                "웹소켓 테스트방",
                ChatRoomType.CUSTOM,
                null,
                "통합 테스트용 채팅방",
                "ws-member",
                true,
                null
        );
        chatRoomRepository.save(room);

        ChatRoomMember roomMember = ChatRoomMember.create(room, "ws-member", LocalDateTime.now().minusMinutes(10));
        chatRoomMemberRepository.save(roomMember);
        room.increaseMemberCount();
        chatRoomRepository.save(room);

        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "ws-member",
                        "ws-member@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "웹소켓테스터",
                        "https://example.com/photo.jpg"
                ));
        when(firebaseTokenVerifier.verify("invalid-token"))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    void websocket_연결후_메시지송수신_성공() throws Exception {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");

        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/chat/room-ws", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        session.send("/app/chat/room-ws", Map.of("type", "TEXT", "text", "웹소켓 전송 테스트"));

        Map<String, Object> payload = received.poll(5, TimeUnit.SECONDS);
        assertNotNull(payload);
        assertEquals("TEXT", payload.get("type"));
        assertEquals("웹소켓 전송 테스트", payload.get("text"));

        session.disconnect();
    }

    @Test
    void native_websocket_연결후_메시지송수신_성공() throws Exception {
        WebSocketStompClient nativeStompClient = new WebSocketStompClient(new StandardWebSocketClient());
        nativeStompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try {
            String url = "ws://localhost:" + port + "/ws-native";
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add("Authorization", "Bearer valid-token");

            StompSession session = nativeStompClient
                    .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS);

            LinkedBlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
            session.subscribe("/topic/chat/room-ws", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.offer((Map<String, Object>) payload);
                }
            });
            Thread.sleep(300);

            session.send("/app/chat/room-ws", Map.of("type", "TEXT", "text", "네이티브 웹소켓 전송 테스트"));

            Map<String, Object> payload = received.poll(5, TimeUnit.SECONDS);
            assertNotNull(payload);
            assertEquals("TEXT", payload.get("type"));
            assertEquals("네이티브 웹소켓 전송 테스트", payload.get("text"));

            session.disconnect();
        } finally {
            nativeStompClient.stop();
        }
    }

    @Test
    void websocket_메시지전송예외는_표준에러포맷으로수신() throws Exception {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");

        StompSession session = stompClient
                .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        LinkedBlockingQueue<Map<String, Object>> errors = new LinkedBlockingQueue<>();
        session.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                errors.offer((Map<String, Object>) payload);
            }
        });
        Thread.sleep(300);

        session.send("/app/chat/room-ws", Map.of("type", "SYSTEM", "text", "금지 타입"));

        Map<String, Object> errorPayload = errors.poll(5, TimeUnit.SECONDS);
        assertNotNull(errorPayload);
        assertEquals(false, errorPayload.get("success"));
        assertEquals("INVALID_REQUEST", errorPayload.get("errorCode"));

        session.disconnect();
    }

    @Test
    void websocket_인증실패토큰이면_연결거부() {
        String url = "http://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer invalid-token");

        assertThrows(ExecutionException.class, () ->
                stompClient
                        .connectAsync(url, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS)
        );
    }
}
