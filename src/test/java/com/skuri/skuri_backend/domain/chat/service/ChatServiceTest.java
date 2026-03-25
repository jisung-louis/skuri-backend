package com.skuri.skuri_backend.domain.chat.service;

import com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisher;
import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.chat.dto.request.CreateChatRoomRequest;
import com.skuri.skuri_backend.domain.chat.dto.request.SendChatMessageRequest;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatMessageResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatReadUpdateResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomDetailResponse;
import com.skuri.skuri_backend.domain.chat.dto.response.ChatRoomSummaryResponse;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
    void getChatRooms_다른학과공개방은_목록에서숨긴다() {
        Member member = activeMember("member-1", "컴퓨터공학과");
        ChatRoom universityRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoom departmentRoom = ChatRoom.create("public:department:cs", "컴퓨터공학과 채팅방", ChatRoomType.DEPARTMENT, "컴퓨터공학과", null, null, true, null);
        ChatRoom otherDepartmentRoom = ChatRoom.create("public:department:law", "법학과 채팅방", ChatRoomType.DEPARTMENT, "법학과", null, null, true, null);

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(chatRoomRepository.findAll()).thenReturn(List.of(universityRoom, departmentRoom, otherDepartmentRoom));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of());

        List<ChatRoomSummaryResponse> rooms = chatService.getChatRooms("member-1", null, null);

        assertEquals(2, rooms.size());
        assertEquals(List.of("public:university", "public:department:cs"), rooms.stream().map(ChatRoomSummaryResponse::id).toList());
        assertFalse(rooms.get(0).joined());
    }

    @Test
    void getChatRoomDetail_미참여공개방도_조회할수있다() {
        ChatRoom publicRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, "설명", null, true, null);
        ReflectionTestUtils.setField(publicRoom, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 21, 10, 0));
        ReflectionTestUtils.setField(publicRoom, "lastMessageText", "안녕하세요");
        ReflectionTestUtils.setField(publicRoom, "lastMessageSenderName", "홍길동");
        ReflectionTestUtils.setField(publicRoom, "lastMessageType", ChatMessageType.TEXT);

        when(chatRoomRepository.findById("public:university")).thenReturn(Optional.of(publicRoom));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("public:university", "member-1"))
                .thenReturn(Optional.empty());

        ChatRoomDetailResponse response = chatService.getChatRoomDetail("member-1", "public:university");

        assertFalse(response.joined());
        assertEquals(0, response.unreadCount());
        assertNotNull(response.lastMessage());
        assertEquals(LocalDateTime.of(2026, 3, 5, 21, 10, 0), response.lastMessageAt());
    }

    @Test
    void getMessages_미참여공개방이면_NOT_CHAT_ROOM_MEMBER() {
        ChatRoom publicRoom = ChatRoom.create("public:university", "성결대학교 전체 채팅방", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        when(chatRoomRepository.findById("public:university")).thenReturn(Optional.of(publicRoom));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("public:university", "member-1"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.getMessages("member-1", "public:university", null, null, 50)
        );

        assertEquals(ErrorCode.NOT_CHAT_ROOM_MEMBER, exception.getErrorCode());
    }

    @Test
    void createChatRoom_생성자는_즉시참여상태가된다() {
        AtomicReference<ChatRoomMember> savedMemberRef = new AtomicReference<>();
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(activeMember("member-1", "컴퓨터공학과")));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> {
            ChatRoomMember member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", ChatRoomMemberId.of(member.getChatRoom().getId(), member.getMemberId()));
            savedMemberRef.set(member);
            return member;
        });
        when(chatRoomMemberRepository.findById_ChatRoomId(anyString())).thenAnswer(invocation -> List.of(savedMemberRef.get()));

        ChatRoomDetailResponse response = chatService.createChatRoom(
                "member-1",
                new CreateChatRoomRequest("시험기간 밤샘 메이트", "기말고사 기간 같이 공부할 사람들 모여요.")
        );

        assertEquals(ChatRoomType.CUSTOM, response.type());
        assertTrue(response.joined());
        assertEquals(1, response.memberCount());
        assertEquals("시험기간 밤샘 메이트", response.name());
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void createChatRoom_활성회원이없으면_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.createChatRoom(
                        "member-1",
                        new CreateChatRoomRequest("시험기간 밤샘 메이트", "기말고사 기간 같이 공부할 사람들 모여요.")
                )
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void joinChatRoom_기존메시지가있으면_초기unread를0으로시작한다() {
        ChatRoom room = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "memberCount", 10);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 22, 0, 0));
        AtomicReference<ChatRoomMember> savedMemberRef = new AtomicReference<>();

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(activeMember("member-1", "컴퓨터공학과")));
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1")).thenReturn(Optional.empty());
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> {
            ChatRoomMember member = invocation.getArgument(0);
            ReflectionTestUtils.setField(member, "id", ChatRoomMemberId.of("room-1", "member-1"));
            savedMemberRef.set(member);
            return member;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("room-1")).thenAnswer(invocation -> List.of(savedMemberRef.get()));

        ChatRoomDetailResponse response = chatService.joinChatRoom("member-1", "room-1");

        assertTrue(response.joined());
        assertEquals(0, response.unreadCount());
        assertEquals(toInstant(LocalDateTime.of(2026, 3, 5, 22, 0, 0)), response.lastReadAt());
        assertEquals(11, response.memberCount());
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void joinChatRoom_활성회원이없으면_MEMBER_NOT_FOUND() {
        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> chatService.joinChatRoom("member-1", "room-1")
        );

        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void leaveChatRoom_멤버십을제거하고_removed이벤트를보낸다() {
        ChatRoom room = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "memberCount", 2);
        ChatRoomMember membership = membership(room, "room-1", "member-1");
        ChatRoomMember remainingMember = membership(room, "room-1", "member-2");

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1")).thenReturn(Optional.of(membership));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("room-1")).thenReturn(List.of(remainingMember));

        ChatRoomDetailResponse response = chatService.leaveChatRoom("member-1", "room-1");

        assertFalse(response.joined());
        assertNull(response.lastReadAt());
        assertEquals(1, response.memberCount());
        verify(chatRoomMemberRepository).delete(membership);
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/chat-rooms"), any());
    }

    @Test
    void removeMemberFromDepartmentChatRooms_학과방멤버십만정리한다() {
        ChatRoom departmentRoom = ChatRoom.create("public:department:cs", "컴퓨터공학과 채팅방", ChatRoomType.DEPARTMENT, "컴퓨터공학과", null, null, true, null);
        ReflectionTestUtils.setField(departmentRoom, "memberCount", 1);
        ChatRoom customRoom = ChatRoom.create("room-1", "시험기간 밤샘 메이트", ChatRoomType.CUSTOM, null, null, null, true, null);
        ReflectionTestUtils.setField(customRoom, "memberCount", 1);
        ChatRoomMember departmentMembership = membership(departmentRoom, "public:department:cs", "member-1");
        ChatRoomMember customMembership = membership(customRoom, "room-1", "member-1");

        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of(departmentMembership, customMembership));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("public:department:cs")).thenReturn(List.of());

        chatService.removeMemberFromDepartmentChatRooms("member-1");

        verify(chatRoomMemberRepository).delete(departmentMembership);
        verify(chatRoomMemberRepository, times(1)).delete(any(ChatRoomMember.class));
    }

    @Test
    void markAsRead_과거시각요청이면_단조증가유지() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ChatRoomMember roomMember = membership(room, "room-1", "member-1");
        roomMember.advanceLastReadAt(LocalDateTime.of(2026, 3, 5, 21, 0, 0));

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));

        ChatReadUpdateResponse response = chatService.markAsRead(
                "member-1",
                "room-1",
                toInstant(LocalDateTime.of(2026, 3, 5, 20, 59, 0))
        );

        assertFalse(response.updated());
        assertEquals(toInstant(LocalDateTime.of(2026, 3, 5, 21, 0, 0)), response.lastReadAt());
    }

    @Test
    void markAsRead_미래시각요청이면_마지막메시지시각으로보정한다() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", LocalDateTime.of(2026, 3, 5, 21, 30, 0));
        ChatRoomMember roomMember = membership(room, "room-1", "member-1");

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatReadUpdateResponse response = chatService.markAsRead(
                "member-1",
                "room-1",
                Instant.parse("2099-03-05T12:00:00Z")
        );

        assertEquals(toInstant(LocalDateTime.of(2026, 3, 5, 21, 30, 0)), response.lastReadAt());
        assertTrue(response.updated());
    }

    @Test
    void markAsRead_UTC요청후_상세와목록재조회에도_unread가복원되지않는다() {
        ChatRoom room = ChatRoom.create("room-1", "테스트", ChatRoomType.UNIVERSITY, null, null, null, true, null);
        LocalDateTime lastMessageAt = LocalDateTime.of(2026, 3, 5, 21, 30, 0);
        ReflectionTestUtils.setField(room, "lastMessageTimestamp", lastMessageAt);
        ReflectionTestUtils.setField(room, "messageCount", 5);
        ChatRoomMember roomMember = membership(room, "room-1", "member-1");
        Member member = activeMember("member-1", "컴퓨터공학과");

        when(memberRepository.findActiveById("member-1")).thenReturn(Optional.of(member));
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomRepository.findAll()).thenReturn(List.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("room-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(chatRoomMemberRepository.findById_MemberId("member-1")).thenReturn(List.of(roomMember));
        when(chatRoomMemberRepository.save(any(ChatRoomMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.countByChatRoomIdAndCreatedAtAfter("room-1", lastMessageAt)).thenReturn(0L);

        Instant readAt = Instant.parse("2026-03-05T12:30:00Z");

        ChatReadUpdateResponse updateResponse = chatService.markAsRead("member-1", "room-1", readAt);
        ChatRoomDetailResponse detailResponse = chatService.getChatRoomDetail("member-1", "room-1");
        List<ChatRoomSummaryResponse> summaryResponses = chatService.getChatRooms("member-1", null, null);

        assertTrue(updateResponse.updated());
        assertEquals(readAt, updateResponse.lastReadAt());
        assertEquals(0, detailResponse.unreadCount());
        assertEquals(readAt, detailResponse.lastReadAt());
        assertEquals(0, summaryResponses.get(0).unreadCount());
    }

    @Test
    void sendMessage_파티ACCOUNT타입이면_특수페이로드저장및브로드캐스트() {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember roomMember = membership(room, "party:party-1", "member-1");
        Member sender = Member.create("member-1", "member-1@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(1));
        SendChatMessageRequest request = new SendChatMessageRequest(
                ChatMessageType.ACCOUNT,
                null,
                null,
                new SendChatMessageRequest.AccountPayload(
                        "카카오뱅크",
                        "3333-01-1234567",
                        "홍길동",
                        false,
                        false
                )
        );

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findById_ChatRoomIdAndId_MemberId("party:party-1", "member-1"))
                .thenReturn(Optional.of(roomMember));
        when(memberRepository.findById("member-1")).thenReturn(Optional.of(sender));
        when(partyMessageService.buildClientPayload(eq("party:party-1"), eq("member-1"), eq(request)))
                .thenReturn(new PartySpecialMessagePayload(
                        "계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)",
                        new ChatAccountData("카카오뱅크", "3333-01-1234567", "홍길동", false),
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
                request
        );

        assertEquals(ChatMessageType.ACCOUNT, response.type());
        assertNotNull(response.accountData());
        assertEquals("카카오뱅크", response.accountData().bankName());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/party:party-1"), any(ChatMessageResponse.class));
        verify(messagingTemplate).convertAndSendToUser(eq("member-1"), eq("/queue/chat-rooms"), any());
    }

    @Test
    void createPartySystemMessage_저장후_파티채팅히스토리와브로드캐스트에사용한다() {
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
        ChatRoomMember leaderMember = membership(room, "party:party-1", "leader-1");
        Member leader = Member.create("leader-1", "leader-1@sungkyul.ac.kr", "파티 리더", LocalDateTime.now().minusDays(1));

        when(memberRepository.findById("leader-1")).thenReturn(Optional.of(leader));
        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", "message-system-1");
            ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 3, 5, 21, 10, 0));
            return message;
        });
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(leaderMember));

        ChatMessageResponse response = chatService.createPartySystemMessage(party, "leader-1", "모집이 마감되었어요.");

        assertEquals(ChatMessageType.SYSTEM, response.type());
        assertEquals("모집이 마감되었어요.", response.text());
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/party:party-1"), any(ChatMessageResponse.class));
        verify(messagingTemplate).convertAndSendToUser(eq("leader-1"), eq("/queue/chat-rooms"), any());
        verify(eventPublisher).publish(argThat(event ->
                event instanceof com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent.ChatMessageCreated created
                        && created.chatRoomId().equals("party:party-1")
                        && created.messageId().equals("message-system-1")
        ));
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
        ChatRoomMember leaderMember = membership(room, "party:party-1", "leader-1");
        ChatRoomMember removedMember = membership(room, "party:party-1", "member-2");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1")).thenReturn(List.of(leaderMember, removedMember));

        chatService.syncPartyChatRoomMembers(party);

        verify(chatRoomMemberRepository).delete(removedMember);
        verify(chatRoomRepository).save(room);
        assertEquals(1, room.getMemberCount());
    }

    private Member activeMember(String memberId, String department) {
        Member member = Member.create(memberId, memberId + "@sungkyul.ac.kr", "홍길동", LocalDateTime.now().minusDays(1));
        member.updateProfile(null, null, department, null);
        return member;
    }

    private ChatRoomMember membership(ChatRoom room, String chatRoomId, String memberId) {
        ChatRoomMember membership = ChatRoomMember.create(room, memberId, LocalDateTime.now().minusHours(1));
        ReflectionTestUtils.setField(membership, "id", ChatRoomMemberId.of(chatRoomId, memberId));
        return membership;
    }

    private Instant toInstant(LocalDateTime value) {
        return value.atZone(ZoneId.of("Asia/Seoul")).toInstant();
    }
}
