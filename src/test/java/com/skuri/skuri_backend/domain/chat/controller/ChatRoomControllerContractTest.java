package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessagePageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomLastMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSettingsResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatService;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatRoomController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ChatRoomControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @Test
    void getChatRooms_정상조회_200() throws Exception {
        mockValidToken();
        when(chatService.getChatRooms("firebase-uid", null, null))
                .thenReturn(List.of(new ChatRoomSummaryResponse(
                        "room-1",
                        ChatRoomType.UNIVERSITY,
                        "성결대학교 전체 채팅방",
                        "성결대학교 전체 채팅방입니다.",
                        true,
                        150,
                        true,
                        3,
                        new ChatRoomLastMessageResponse("TEXT", "안녕하세요", "홍길동", LocalDateTime.now()),
                        LocalDateTime.now(),
                        false
                )));

        mockMvc.perform(
                        get("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("room-1"))
                .andExpect(jsonPath("$.data[0].joined").value(true))
                .andExpect(jsonPath("$.data[0].isPublic").value(true));
    }

    @Test
    void createChatRoom_정상생성_201() throws Exception {
        mockValidToken();
        when(chatService.createChatRoom(eq("firebase-uid"), any(CreateChatRoomRequest.class)))
                .thenReturn(new ChatRoomDetailResponse(
                        "room-1",
                        ChatRoomType.CUSTOM,
                        "시험기간 밤샘 메이트",
                        "설명",
                        true,
                        1,
                        true,
                        0,
                        null,
                        null,
                        false,
                        null
                ));

        mockMvc.perform(
                        post("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "시험기간 밤샘 메이트",
                                          "description": "기말고사 기간 같이 공부할 사람들 모여요."
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.joined").value(true))
                .andExpect(jsonPath("$.data.type").value("CUSTOM"));
    }

    @Test
    void createChatRoom_이름없음_422() throws Exception {
        mockValidToken();

        mockMvc.perform(
                        post("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "description": "기말고사 기간 같이 공부할 사람들 모여요."
                                        }
                                        """)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(chatService);
    }

    @Test
    void createChatRoom_회원없음_404() throws Exception {
        mockValidToken();
        when(chatService.createChatRoom(eq("firebase-uid"), any(CreateChatRoomRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "시험기간 밤샘 메이트",
                                          "description": "기말고사 기간 같이 공부할 사람들 모여요."
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void getChatRoom_정상조회_200() throws Exception {
        mockValidToken();
        when(chatService.getChatRoomDetail("firebase-uid", "room-1"))
                .thenReturn(roomDetailResponse(false));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("room-1"))
                .andExpect(jsonPath("$.data.joined").value(false))
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    void joinChatRoom_정상참여_200() throws Exception {
        mockValidToken();
        when(chatService.joinChatRoom("firebase-uid", "room-1"))
                .thenReturn(roomDetailResponse(true));

        mockMvc.perform(
                        post("/v1/chat-rooms/room-1/join")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joined").value(true));
    }

    @Test
    void joinChatRoom_이미참여중이면_409() throws Exception {
        mockValidToken();
        when(chatService.joinChatRoom("firebase-uid", "room-1"))
                .thenThrow(new BusinessException(ErrorCode.ALREADY_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        post("/v1/chat-rooms/room-1/join")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_CHAT_ROOM_MEMBER"));
    }

    @Test
    void joinChatRoom_회원없음_404() throws Exception {
        mockValidToken();
        when(chatService.joinChatRoom("firebase-uid", "room-1"))
                .thenThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(
                        post("/v1/chat-rooms/room-1/join")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void leaveChatRoom_정상나가기_200() throws Exception {
        mockValidToken();
        when(chatService.leaveChatRoom("firebase-uid", "room-1"))
                .thenReturn(roomDetailResponse(false));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joined").value(false));
    }

    @Test
    void leaveChatRoom_멤버아니면_403() throws Exception {
        mockValidToken();
        when(chatService.leaveChatRoom("firebase-uid", "room-1"))
                .thenThrow(new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        delete("/v1/chat-rooms/room-1/members/me")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_CHAT_ROOM_MEMBER"));
    }

    @Test
    void getChatRoom_비공개방비멤버_403() throws Exception {
        mockValidToken();
        when(chatService.getChatRoomDetail("firebase-uid", "room-private"))
                .thenThrow(new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-private")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_CHAT_ROOM_MEMBER"));
    }

    @Test
    void getMessages_커서쌍불일치_422() throws Exception {
        mockValidToken();
        when(chatService.getMessages(eq("firebase-uid"), eq("room-1"), any(), eq("message-1"), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "cursorCreatedAt와 cursorId는 함께 전달해야 합니다."));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-1/messages")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .param("cursorId", "message-1")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void markAsRead_정상요청_200() throws Exception {
        mockValidToken();
        when(chatService.markAsRead(eq("firebase-uid"), eq("room-1"), any()))
                .thenReturn(new ChatReadUpdateResponse("room-1", Instant.parse("2026-03-05T12:10:00Z"), true));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/read")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"lastReadAt\":\"2026-03-05T12:10:00Z\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value("room-1"))
                .andExpect(jsonPath("$.data.lastReadAt").value("2026-03-05T12:10:00Z"))
                .andExpect(jsonPath("$.data.updated").value(true));
    }

    @Test
    void updateSettings_멤버아님_403() throws Exception {
        mockValidToken();
        when(chatService.updateSettings("firebase-uid", "room-1", true))
                .thenThrow(new BusinessException(ErrorCode.NOT_CHAT_ROOM_MEMBER));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/settings")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"muted\":true}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_CHAT_ROOM_MEMBER"));
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(get("/v1/chat-rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(chatService);
    }

    private void mockValidToken() {
        when(firebaseTokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
    }

    private ChatRoomDetailResponse roomDetailResponse(boolean joined) {
        return new ChatRoomDetailResponse(
                "room-1",
                ChatRoomType.UNIVERSITY,
                "성결대학교 전체 채팅방",
                "설명",
                true,
                120,
                joined,
                joined ? 2 : 0,
                new ChatRoomLastMessageResponse("TEXT", "안녕하세요", "홍길동", LocalDateTime.now().minusMinutes(1)),
                LocalDateTime.now().minusMinutes(1),
                false,
                joined ? Instant.parse("2026-03-05T12:05:00Z") : null
        );
    }
}
