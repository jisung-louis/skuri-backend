package com.skuri.skuri_backend.domain.chat.controller;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.service.ChatAdminService;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatAdminRoomController.class)
@Import({
        SecurityConfig.class,
        FirebaseAuthenticationFilter.class,
        ApiAuthenticationEntryPoint.class,
        ApiAccessDeniedHandler.class
})
class ChatAdminRoomControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatAdminService chatAdminService;

    @MockitoBean
    private FirebaseTokenVerifier firebaseTokenVerifier;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void createPublicChatRoom_관리자요청_201() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.createPublicChatRoom(eq("admin-uid"), any()))
                .thenReturn(new AdminCreateChatRoomResponse(
                        "room:1",
                        "성결대 전체 채팅방",
                        ChatRoomType.UNIVERSITY
                ));

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"성결대 전체 채팅방\",\"type\":\"UNIVERSITY\",\"description\":\"설명\",\"isPublic\":true}")
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("room:1"));
    }

    @Test
    void createPublicChatRoom_비관리자요청_403_ADMIN_REQUIRED() throws Exception {
        mockToken("user-token", "user-uid", false);

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer user-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"일반 채팅방\",\"type\":\"CUSTOM\",\"isPublic\":true}")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ADMIN_REQUIRED"));
    }

    @Test
    void createPublicChatRoom_요청검증실패_422() throws Exception {
        mockToken("admin-token", "admin-uid", true);

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"\",\"type\":\"UNIVERSITY\",\"isPublic\":true}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void createPublicChatRoom_party타입요청_400() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        when(chatAdminService.createPublicChatRoom(eq("admin-uid"), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "PARTY 타입 채팅방은 파티 생성 시 자동 생성됩니다."));

        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .header(AUTHORIZATION, "Bearer admin-token")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"파티 채팅방\",\"type\":\"PARTY\",\"isPublic\":true}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void deletePublicChatRoom_채팅방없음_404() throws Exception {
        mockToken("admin-token", "admin-uid", true);
        doThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND))
                .when(chatAdminService)
                .deletePublicChatRoom("unknown-room");

        mockMvc.perform(
                        delete("/v1/admin/chat-rooms/unknown-room")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CHAT_ROOM_NOT_FOUND"));
    }

    @Test
    void deletePublicChatRoom_관리자요청_200() throws Exception {
        mockToken("admin-token", "admin-uid", true);

        mockMvc.perform(
                        delete("/v1/admin/chat-rooms/room-public")
                                .header(AUTHORIZATION, "Bearer admin-token")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 보호API_토큰없음_401() throws Exception {
        mockMvc.perform(
                        post("/v1/admin/chat-rooms")
                                .contentType(APPLICATION_JSON)
                                .content("{\"name\":\"성결대 전체 채팅방\",\"type\":\"UNIVERSITY\",\"isPublic\":true}")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(chatAdminService);
    }

    private void mockToken(String token, String uid, boolean isAdmin) {
        when(firebaseTokenVerifier.verify(token))
                .thenReturn(new FirebaseTokenClaims(
                        uid,
                        uid + "@sungkyul.ac.kr",
                        "google.com",
                        "provider-id",
                        "테스터",
                        "https://example.com/profile.jpg"
                ));
        if (!isAdmin) {
            when(memberRepository.findById(uid)).thenReturn(Optional.empty());
            return;
        }
        Member admin = Member.create(
                uid,
                uid + "@sungkyul.ac.kr",
                "관리자",
                LocalDateTime.now().minusDays(1)
        );
        ReflectionTestUtils.setField(admin, "isAdmin", true);
        when(memberRepository.findById(uid)).thenReturn(Optional.of(admin));
    }
}
