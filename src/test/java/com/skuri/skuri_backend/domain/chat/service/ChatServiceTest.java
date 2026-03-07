package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.entity.ChatAccountData;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMemberId;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PartyMessageService partyMessageService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private AfterCommitApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatService chatService;

    @Test
    void getChatRooms_비공개방은_멤버에게만노출() {
        ChatRoom publicRoom = ChatRoom.create("room-public", "공개", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoom privateRoom = ChatRoom.create("room-private", "비공개", ChatRoomType.CUSTOM, null, null, null, false, null);
        ChatRoomMember membership = ChatRoomMember.create(publicRoom, "member-1", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(membership, "id", ChatRoomMemberId.of("room-public", "member-1"));

        when(chatRoomRepository.findAll()).thenReturn(List.of(publicRoom, privateRoom));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of(membership));

        List<?> rooms = chatService.getChatRooms("member-1", null, null);

        assertEquals(1, rooms.size());
    }

    @Test
    void getChatRoomDetail_비공개방비멤버면_예외() {
        ChatRoom privateRoom = ChatRoom.create("room-private", "비공개", ChatRoomType.CUSTOM, null, null, null, false, null);
        when(chatRoomRepository.findById("room-private")).thenReturn(Optional.of(privateRoom));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-private", "member-1"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.getChatRoomDetail("member-1", "room-private")
        );
        assertEquals(ErrorCode.NOT_CHAT_ROOM_MEMBER, exception.getErrorCode());
    }

    @Test
    void markAsRead_과거시각요청이면_단조증가유지() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoomMember roomMember = ChatRoomMember.create(room, "member-1", LocalDateTime.now().minusHours(2));
        roomMember.advanceLastReadAt(LocalDateTime.of(2026, 3, 5, 21, 0, 0));

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));

        ChatReadUpdateResponse response = chatService.markAsRead(
                "member-1",
                "room-1",
                LocalDateTime.of(2026, 3, 5, 20, 59, 0)
        );

        assertFalse(response.updated());
        assertEquals(LocalDateTime.of(2026, 3, 5, 21, 0, 0), response.lastReadAt());
    }

    @Test
    void markAsRead_미래시각요청이면_마지막메시지시각으로보정한다() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 21, 30, 0));
        ChatRoomMember roomMember = ChatRoomMember.create(room, "member-1", LocalDateTime.now().minusHours(2));

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatReadUpdateResponse response = chatService.markAsRead(
                "member-1",
                "room-1",
                LocalDateTime.of(2099, 3, 5, 21, 0, 0)
        );

        assertEquals(LocalDateTime.of(2026, 3, 5, 21, 30, 0), response.lastReadAt());
        assertTrue(response.updated());
    }

    @Test
    void sendMessage_파티ACCOUNT타입이면_특수페이로드저장및브로드캐스트() {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember roomMember = ChatRoomMember.create(room, "member-1", LocalDateTime.now().minusHours(1));
        Member sender = Member.create("member-1", "member-1@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(1));

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("party:party-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(sender));
        when(partyMessageService.buildSpecialPayload(eq("party:party-1"), eq("member-1"), eq(ChatMessageType.ACCOUNT)))
                .thenReturn(new PartySpecialMessagePayload(
                        "카카오뱅크/3333-01-1234567",
                        new ChatAccountData("카카오뱅크", "3333-01-1234567", "홍길동"),
                        null
                ));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 21, 10, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(roomMember));

        ChatMessageResponse response = chatService.sendMessage(
                "party:party-1",
                "member-1",
                new SendChatMessageRequest(ChatMessageType.ACCOUNT, null, null, null)
        );

        assertEquals(ChatMessageType.ACCOUNT, response.type());
        assertNotNull(response.accountData());
        assertEquals("카카오뱅크", response.accountData().bankName());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/party:party-1"), any(ChatMessageResponse.class));
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void createPartyChatRoom_파티멤버수만큼_chatRoomMember생성() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        party.addMember("member-2");
        ReflectionTestUtils.setField(party, "id", "party-1");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of());

        chatService.createPartyChatRoom(party);

        verify(chatRoomRepository, times(2)).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    void syncPartyChatRoomMembers_파티탈퇴멤버는_채팅멤버에서제거된다() {
        Party party = Party.create(
                "leader-1",
                Location.of("성결대학교", 37.38, 126.93),
                Location.of("안양역", 37.40, 126.92),
                LocalDateTime.now().plusHours(2),
                4,
                List.of("빠른출발"),
                "테스트"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");

        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember leaderMember = ChatRoomMember.create(room, "leader-1", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(leaderMember, "id", ChatRoomMemberId.of("party:party-1", "leader-1"));
        ChatRoomMember removedMember = ChatRoomMember.create(room, "member-2", LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(removedMember, "id", ChatRoomMemberId.of("party:party-1", "member-2"));

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(leaderMember, removedMember));

        chatService.syncPartyChatRoomMembers(party);

        verify(chatRoomMemberRepository).delete(removedMember);
        verify(chatRoomRepository).save(room);
        assertEquals(1, room.getMemberCount());
    }
}
