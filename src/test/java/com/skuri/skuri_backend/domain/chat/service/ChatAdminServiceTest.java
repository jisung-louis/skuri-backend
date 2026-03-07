package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.AdminCreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.AdminCreateChatRoomResponse;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAdminServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatAdminService chatAdminService;

    @Test
    void createPublicChatRoom_정상요청_생성성공() {
        AdminCreateChatRoomRequest request = new AdminCreateChatRoomRequest(
                "성결대 전체 채팅방",
                ChatRoomType.UNIVERSITY,
                "성결대학교 학생들의 소통 공간",
                true
        );
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminCreateChatRoomResponse response = chatAdminService.createPublicChatRoom("admin-uid", request);

        assertEquals("성결대 전체 채팅방", response.name());
        assertEquals(ChatRoomType.UNIVERSITY, response.type());
        verify(chatRoomRepository, times(2)).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    void createPublicChatRoom_party타입_예외() {
        AdminCreateChatRoomRequest request = new AdminCreateChatRoomRequest(
                "파티방",
                ChatRoomType.PARTY,
                null,
                true
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatAdminService.createPublicChatRoom("admin-uid", request)
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void createPublicChatRoom_isPublicFalse_예외() {
        AdminCreateChatRoomRequest request = new AdminCreateChatRoomRequest(
                "테스트방",
                ChatRoomType.CUSTOM,
                null,
                false
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatAdminService.createPublicChatRoom("admin-uid", request)
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void deletePublicChatRoom_정상요청_삭제성공() {
        ChatRoom room = ChatRoom.create(
                "room-public",
                "공개방",
                ChatRoomType.UNIVERSITY,
                null,
                null,
                "admin-uid",
                true,
                null
        );
        when(chatRoomRepository.findById("room-public")).thenReturn(Optional.of(room));

        chatAdminService.deletePublicChatRoom("room-public");

        verify(chatMessageRepository).deleteByChatRoomId("room-public");
        verify(chatRoomMemberRepository).deleteById_ChatRoomId("room-public");
        verify(chatRoomRepository).delete(room);
    }

    @Test
    void deletePublicChatRoom_비공개방_예외() {
        ChatRoom room = ChatRoom.create(
                "room-private",
                "비공개방",
                ChatRoomType.CUSTOM,
                null,
                null,
                "admin-uid",
                false,
                null
        );
        when(chatRoomRepository.findById("room-private")).thenReturn(Optional.of(room));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatAdminService.deletePublicChatRoom("room-private")
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(chatMessageRepository, never()).deleteByChatRoomId("room-private");
    }

    @Test
    void deletePublicChatRoom_파티방_예외() {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatAdminService.deletePublicChatRoom("party:party-1")
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(chatRoomRepository, never()).delete(room);
    }

    @Test
    void deletePublicChatRoom_미존재_예외() {
        when(chatRoomRepository.findById("unknown")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatAdminService.deletePublicChatRoom("unknown")
        );

        assertEquals(ErrorCode.CHAT_ROOM_NOT_FOUND, exception.getErrorCode());
    }
}
