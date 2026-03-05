package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                        "성결대 전체 채팅방",
                        ChatRoomType.UNIVERSITY,
                        150,
                        new ChatRoomLastMessageResponse("TEXT", "안녕하세요", "홍길동", LocalDateTime.now()),
                        3,
                        true
                )));

        mockMvc.perform(
                        get("/v1/chat-rooms")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("room-1"))
                .andExpect(jsonPath("$.data[0].isJoined").value(true));
    }

    @Test
    void getChatRoom_정상조회_200() throws Exception {
        mockValidToken();
        when(chatService.getChatRoomDetail("firebase-uid", "room-1"))
                .thenReturn(new ChatRoomDetailResponse(
                        "room-1",
                        "성결대 전체 채팅방",
                        ChatRoomType.UNIVERSITY,
                        "설명",
                        true,
                        120,
                        true,
                        false,
                        LocalDateTime.now().minusMinutes(5),
                        2
                ));

        mockMvc.perform(
                        get("/v1/chat-rooms/room-1")
                                .header(AUTHORIZATION, "Bearer valid-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("room-1"));
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
                .thenReturn(new ChatReadUpdateResponse("room-1", LocalDateTime.of(2026, 3, 5, 21, 10, 0), false));

        mockMvc.perform(
                        patch("/v1/chat-rooms/room-1/read")
                                .header(AUTHORIZATION, "Bearer valid-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"lastReadAt\":\"2026-03-05T20:00:00\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.chatRoomId").value("room-1"))
                .andExpect(jsonPath("$.data.updated").value(false));
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
}
