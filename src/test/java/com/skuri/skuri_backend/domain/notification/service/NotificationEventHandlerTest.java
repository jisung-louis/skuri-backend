package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
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
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.taxiparty.entity.Location;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementAccountSnapshot;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private PartyRepository partyRepository;
    @Mock
    private JoinRequestRepository joinRequestRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;
    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private PostInteractionRepository postInteractionRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private NoticeRepository noticeRepository;
    @Mock
    private NoticeCommentRepository noticeCommentRepository;
    @Mock
    private AppNoticeRepository appNoticeRepository;
    @Mock
    private AcademicScheduleRepository academicScheduleRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private NotificationEventHandler notificationEventHandler;

    @Test
    void handlePartyCreated_partyNotifications켜진사용자만수신한다() {
        Party party = Party.create(
                "leader-1",
                Location.of("정문", null, null),
                Location.of("역", null, null),
                LocalDateTime.of(2026, 3, 8, 18, 30),
                4,
                List.of("역"),
                "상세"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIdsExcluding("leader-1"))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyCreated("party-1"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.PARTY_CREATED, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
        assertFalse(captor.getValue().inboxEnabled());
    }

    @Test
    void handleBoardCommentCreated_중복대상자는한번만수신한다() {
        Post post = Post.create("게시글", "내용", "target-1", "작성자", null, false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", "post-1");
        Comment parent = Comment.create(post, "부모", "target-1", "작성자", null, false, null, null, null);
        ReflectionTestUtils.setField(parent, "id", "comment-parent");
        Comment created = Comment.create(post, "새 댓글", "actor-1", "작성자", null, false, null, null, parent);
        ReflectionTestUtils.setField(created, "id", "comment-new");

        Member target = Member.create("target-1", "target-1@sungkyul.ac.kr", "작성자", LocalDateTime.now());
        Member bookmarker = Member.create("target-2", "target-2@sungkyul.ac.kr", "북마크", LocalDateTime.now());

        when(commentRepository.findActiveById("comment-new")).thenReturn(Optional.of(created));
        when(postInteractionRepository.findBookmarkedUserIdsByPostId("post-1")).thenReturn(List.of("target-1", "target-2"));
        when(memberRepository.findById("target-1")).thenReturn(Optional.of(target));
        when(memberRepository.findById("target-2")).thenReturn(Optional.of(bookmarker));

        notificationEventHandler.handle(new NotificationDomainEvent.BoardCommentCreated("comment-new"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService, times(2)).createInboxNotifications(captor.capture());
        List<NotificationDispatchRequest> requests = captor.getAllValues();

        assertEquals(2, requests.size());
        assertTrue(requests.stream().anyMatch(request ->
                request.recipientIds().contains("target-1") && request.title().equals("내 댓글에 답글이 달렸어요")
        ));
        assertTrue(requests.stream().anyMatch(request ->
                request.recipientIds().contains("target-2") && request.title().equals("북마크한 게시글에 새 댓글이 달렸어요")
        ));
    }

    @Test
    void handleAcademicScheduleReminder_옵션에따라수신대상이결정된다() {
        AcademicSchedule schedule = AcademicSchedule.create(
                "수강신청",
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 9),
                AcademicScheduleType.SINGLE,
                false,
                "설명"
        );
        ReflectionTestUtils.setField(schedule, "id", "schedule-1");

        when(academicScheduleRepository.findById("schedule-1")).thenReturn(Optional.of(schedule));
        when(memberRepository.findAcademicScheduleReminderRecipientIds(true, true))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.AcademicScheduleReminder(
                "schedule-1",
                NotificationDomainEvent.ReminderTiming.DAY_BEFORE
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        assertEquals(NotificationType.ACADEMIC_SCHEDULE, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
    }

    @Test
    void handleBoardCommentCreated_allNotifications가꺼져있으면댓글알림을받지않는다() {
        Post post = Post.create("게시글", "내용", "target-1", "작성자", null, false, PostCategory.GENERAL);
        ReflectionTestUtils.setField(post, "id", "post-1");
        Comment created = Comment.create(post, "새 댓글", "actor-1", "작성자", null, false, null, null, null);
        ReflectionTestUtils.setField(created, "id", "comment-new");

        Member postAuthor = Member.create("target-1", "target-1@sungkyul.ac.kr", "작성자", LocalDateTime.now());
        postAuthor.updateNotificationSetting(false, null, null, null, true, null, null, null, null, null, null);

        when(commentRepository.findActiveById("comment-new")).thenReturn(Optional.of(created));
        when(memberRepository.findById("target-1")).thenReturn(Optional.of(postAuthor));
        when(postInteractionRepository.findBookmarkedUserIdsByPostId("post-1")).thenReturn(List.of());

        notificationEventHandler.handle(new NotificationDomainEvent.BoardCommentCreated("comment-new"));

        verify(notificationService, times(0)).createInboxNotifications(org.mockito.ArgumentMatchers.any());
        verify(pushNotificationService, times(0)).send(org.mockito.ArgumentMatchers.any());
    }

    @ParameterizedTest(name = "[파티 CHAT_MESSAGE] {0}")
    @MethodSource("partySpecialMessages")
    void handleChatMessageCreated_파티특수메시지도_CHAT_MESSAGE알림을보낸다(
            ChatMessageType messageType,
            String text,
            String expectedTitle,
            String expectedBody
    ) {
        ChatRoom room = ChatRoom.createPartyRoom("party-1");
        ChatRoomMember actor = membership(room, "party:party-1", "leader-1", false);
        ChatRoomMember recipient = membership(room, "party:party-1", "member-1", false);
        ChatRoomMember mutedRecipient = membership(room, "party:party-1", "member-2", true);
        ChatMessage message = ChatMessage.create(
                "party:party-1",
                "leader-1",
                "리더",
                text,
                messageType,
                null,
                null
        );
        ReflectionTestUtils.setField(message, "id", "message-1");

        when(chatRoomRepository.findById("party:party-1")).thenReturn(Optional.of(room));
        when(chatMessageRepository.findById("message-1")).thenReturn(Optional.of(message));
        when(chatRoomMemberRepository.findById_ChatRoomId("party:party-1"))
                .thenReturn(List.of(actor, recipient, mutedRecipient));

        notificationEventHandler.handle(new NotificationDomainEvent.ChatMessageCreated("party:party-1", "message-1"));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.CHAT_MESSAGE, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
        assertEquals(expectedTitle, captor.getValue().title());
        assertEquals(expectedBody, captor.getValue().message());
        assertEquals("party:party-1", captor.getValue().data().chatRoomId());
        assertEquals(null, captor.getValue().data().partyId());
        assertFalse(captor.getValue().inboxEnabled());
    }

    @Test
    void handlePartyStatusChanged_ARRIVED알림은정산대상멤버에게만보낸다() {
        Party party = Party.create(
                "leader-1",
                Location.of("정문", null, null),
                Location.of("역", null, null),
                LocalDateTime.of(2026, 3, 8, 18, 30),
                4,
                List.of("역"),
                "상세"
        );
        ReflectionTestUtils.setField(party, "id", "party-1");
        party.addMember("member-1");
        party.addMember("member-2");
        party.arrive(
                14000,
                List.of("member-1"),
                SettlementAccountSnapshot.of("카카오뱅크", "3333-03-1234567", "홍길동", true)
        );

        when(partyRepository.findDetailById("party-1")).thenReturn(Optional.of(party));
        when(memberRepository.findPartyNotificationRecipientIds(List.of("member-1")))
                .thenReturn(List.of("member-1"));

        notificationEventHandler.handle(new NotificationDomainEvent.PartyStatusChanged(
                "party-1",
                PartyStatus.CLOSED,
                PartyStatus.ARRIVED
        ));

        ArgumentCaptor<NotificationDispatchRequest> captor = ArgumentCaptor.forClass(NotificationDispatchRequest.class);
        verify(notificationService).createInboxNotifications(captor.capture());
        verify(pushNotificationService).send(captor.getValue());
        assertEquals(NotificationType.PARTY_ARRIVED, captor.getValue().type());
        assertEquals(List.of("member-1"), captor.getValue().recipientIds().stream().toList());
    }

    private static Stream<Arguments> partySpecialMessages() {
        return Stream.of(
                Arguments.of(
                        ChatMessageType.ACCOUNT,
                        "계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)",
                        "리더님이 계좌 정보를 공유했어요",
                        "계좌 정보를 공유했어요. (카카오뱅크 3333-01-1234567)"
                ),
                Arguments.of(
                        ChatMessageType.SYSTEM,
                        "모집이 마감되었어요.",
                        "파티 안내 메시지",
                        "모집이 마감되었어요."
                ),
                Arguments.of(
                        ChatMessageType.ARRIVED,
                        "택시가 목적지에 도착했어요.",
                        "택시가 목적지에 도착했어요",
                        "택시가 목적지에 도착했어요."
                ),
                Arguments.of(
                        ChatMessageType.END,
                        "리더가 파티를 종료했어요.",
                        "파티가 종료되었어요",
                        "리더가 파티를 종료했어요."
                )
        );
    }

    private ChatRoomMember membership(ChatRoom room, String chatRoomId, String memberId, boolean muted) {
        ChatRoomMember membership = ChatRoomMember.create(room, memberId, LocalDateTime.now().minusMinutes(5));
        ReflectionTestUtils.setField(membership, "id", ChatRoomMemberId.of(chatRoomId, memberId));
        membership.updateMuted(muted);
        return membership;
    }
}
