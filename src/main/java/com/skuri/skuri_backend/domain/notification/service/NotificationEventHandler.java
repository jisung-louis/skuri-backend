package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.academic.entity.AcademicSchedule;
import com.skuri.skuri_backend.domain.academic.repository.AcademicScheduleRepository;
import com.skuri.skuri_backend.domain.app.entity.AppNotice;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import com.skuri.skuri_backend.domain.app.repository.AppNoticeRepository;
import com.skuri.skuri_backend.domain.board.entity.Comment;
import com.skuri.skuri_backend.domain.board.entity.Post;
import com.skuri.skuri_backend.domain.board.repository.CommentRepository;
import com.skuri.skuri_backend.domain.board.repository.PostInteractionRepository;
import com.skuri.skuri_backend.domain.board.repository.PostRepository;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessage;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoom;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomMember;
import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import com.skuri.skuri_backend.domain.chat.repository.ChatMessageRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomMemberRepository;
import com.skuri.skuri_backend.domain.chat.repository.ChatRoomRepository;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.entity.NotificationSetting;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.domain.notice.entity.Notice;
import com.skuri.skuri_backend.domain.notice.entity.NoticeComment;
import com.skuri.skuri_backend.domain.notice.repository.NoticeCommentRepository;
import com.skuri.skuri_backend.domain.notice.repository.NoticeRepository;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequest;
import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.Party;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import com.skuri.skuri_backend.domain.taxiparty.repository.JoinRequestRepository;
import com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final PartyRepository partyRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final PostRepository postRepository;
    private final PostInteractionRepository postInteractionRepository;
    private final CommentRepository commentRepository;
    private final NoticeRepository noticeRepository;
    private final NoticeCommentRepository noticeCommentRepository;
    private final AppNoticeRepository appNoticeRepository;
    private final AcademicScheduleRepository academicScheduleRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    public void handle(NotificationDomainEvent event) {
        switch (event) {
            case NotificationDomainEvent.PartyCreated created -> handlePartyCreated(created);
            case NotificationDomainEvent.PartyJoinRequestCreated created -> handleJoinRequestCreated(created);
            case NotificationDomainEvent.PartyJoinRequestProcessed processed -> handleJoinRequestProcessed(processed);
            case NotificationDomainEvent.PartyStatusChanged changed -> handlePartyStatusChanged(changed);
            case NotificationDomainEvent.PartySettlementCompleted completed -> handleSettlementCompleted(completed);
            case NotificationDomainEvent.PartyMemberKicked kicked -> handleMemberKicked(kicked);
            case NotificationDomainEvent.ChatMessageCreated chatMessageCreated -> handleChatMessageCreated(chatMessageCreated);
            case NotificationDomainEvent.BoardPostLiked liked -> handleBoardPostLiked(liked);
            case NotificationDomainEvent.BoardCommentCreated created -> handleBoardCommentCreated(created);
            case NotificationDomainEvent.NoticeCommentCreated created -> handleNoticeCommentCreated(created);
            case NotificationDomainEvent.NoticeCreated created -> handleNoticeCreated(created);
            case NotificationDomainEvent.AppNoticeCreated created -> handleAppNoticeCreated(created);
            case NotificationDomainEvent.AcademicScheduleReminder reminder -> handleAcademicScheduleReminder(reminder);
        }
    }

    private void handlePartyCreated(NotificationDomainEvent.PartyCreated event) {
        Party party = partyRepository.findDetailById(event.partyId()).orElse(null);
        if (party == null) {
            return;
        }

        List<String> recipients = memberRepository.findPartyNotificationRecipientIdsExcluding(party.getLeaderId());

        dispatch(NotificationDispatchRequest.of(
                NotificationType.PARTY_CREATED,
                recipients,
                formatPartyCreatedTitle(party),
                formatPartyCreatedMessage(party),
                NotificationData.ofParty(party.getId()),
                true,
                false
        ));
    }

    private void handleJoinRequestCreated(NotificationDomainEvent.PartyJoinRequestCreated event) {
        JoinRequest request = joinRequestRepository.findDetailById(event.requestId()).orElse(null);
        if (request == null) {
            return;
        }

        dispatch(NotificationDispatchRequest.of(
                NotificationType.PARTY_JOIN_REQUEST,
                findPartyRecipients(List.of(request.getLeaderId())),
                "동승 요청이 도착했어요",
                "앱에서 확인하고 수락/거절을 선택해주세요.",
                NotificationData.ofPartyRequest(request.getParty().getId(), request.getId()),
                true,
                true
        ));
    }

    private void handleJoinRequestProcessed(NotificationDomainEvent.PartyJoinRequestProcessed event) {
        JoinRequest request = joinRequestRepository.findDetailById(event.requestId()).orElse(null);
        if (request == null) {
            return;
        }

        if (event.status() == JoinRequestStatus.ACCEPTED) {
            dispatch(NotificationDispatchRequest.of(
                    NotificationType.PARTY_JOIN_ACCEPTED,
                    findPartyRecipients(List.of(request.getRequesterId())),
                    "동승 요청이 승인되었어요",
                    "파티에 합류하세요!",
                    NotificationData.ofPartyRequest(request.getParty().getId(), request.getId()),
                    true,
                    true
            ));
            return;
        }

        if (event.status() == JoinRequestStatus.DECLINED) {
            dispatch(NotificationDispatchRequest.of(
                    NotificationType.PARTY_JOIN_DECLINED,
                    findPartyRecipients(List.of(request.getRequesterId())),
                    "동승 요청이 거절되었어요",
                    "다른 파티를 찾아보세요.",
                    NotificationData.ofPartyRequest(request.getParty().getId(), request.getId()),
                    true,
                    true
            ));
        }
    }

    private void handlePartyStatusChanged(NotificationDomainEvent.PartyStatusChanged event) {
        Party party = partyRepository.findDetailById(event.partyId()).orElse(null);
        if (party == null) {
            return;
        }

        if (event.afterStatus() == PartyStatus.CLOSED && event.beforeStatus() == PartyStatus.OPEN) {
            dispatch(NotificationDispatchRequest.of(
                    NotificationType.PARTY_CLOSED,
                    findPartyRecipients(party.getNonLeaderMemberIds()),
                    "파티 모집이 마감되었어요",
                    "리더가 파티 모집을 마감했습니다.",
                    NotificationData.ofParty(party.getId()),
                    true,
                    false
            ));
            return;
        }

        if (event.afterStatus() == PartyStatus.ARRIVED && event.beforeStatus() != PartyStatus.ARRIVED) {
            dispatch(NotificationDispatchRequest.of(
                    NotificationType.PARTY_ARRIVED,
                    findPartyRecipients(party.getNonLeaderMemberIds()),
                    "택시가 목적지에 도착했어요",
                    "정산을 진행해주세요.",
                    NotificationData.ofParty(party.getId()),
                    true,
                    true
            ));
            return;
        }

        if (event.afterStatus() == PartyStatus.ENDED && event.beforeStatus() != PartyStatus.ENDED) {
            List<String> allMembers = party.getMemberIds();
            allMembers.forEach(memberId -> notificationService.deletePartyRelatedNotifications(memberId, party.getId()));

            String message = party.getEndReason() == com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason.WITHDRAWED
                    ? "리더 탈퇴로 파티가 종료되었습니다."
                    : "리더가 파티를 해체했습니다.";

            dispatch(NotificationDispatchRequest.of(
                    NotificationType.PARTY_ENDED,
                    findPartyRecipients(party.getNonLeaderMemberIds()),
                    "파티가 해체되었어요",
                    message,
                    NotificationData.ofParty(party.getId()),
                    true,
                    true
            ));
        }
    }

    private void handleSettlementCompleted(NotificationDomainEvent.PartySettlementCompleted event) {
        Party party = partyRepository.findDetailById(event.partyId()).orElse(null);
        if (party == null) {
            return;
        }

        dispatch(NotificationDispatchRequest.of(
                NotificationType.SETTLEMENT_COMPLETED,
                findPartyRecipients(party.getMemberIds()),
                "모든 정산이 완료되었어요",
                "동승 파티 종료 준비가 되었습니다.",
                NotificationData.ofParty(party.getId()),
                true,
                true
        ));
    }

    private void handleMemberKicked(NotificationDomainEvent.PartyMemberKicked event) {
        if (event.memberId() == null || event.memberId().isBlank()) {
            return;
        }

        notificationService.deletePartyRelatedNotifications(event.memberId(), event.partyId());

        dispatch(NotificationDispatchRequest.of(
                NotificationType.MEMBER_KICKED,
                findPartyRecipients(List.of(event.memberId())),
                "파티에서 강퇴되었어요",
                "리더가 당신을 파티에서 나가게 했습니다.",
                NotificationData.ofParty(event.partyId()),
                true,
                true
        ));
    }

    private void handleChatMessageCreated(NotificationDomainEvent.ChatMessageCreated event) {
        ChatRoom room = chatRoomRepository.findById(event.chatRoomId()).orElse(null);
        ChatMessage message = chatMessageRepository.findById(event.messageId()).orElse(null);
        if (room == null || message == null) {
            return;
        }

        if (room.isPublic()) {
            handlePublicChatMessage(room, message);
            return;
        }

        if (room.getType() == ChatRoomType.PARTY) {
            handlePartyChatMessage(room, message);
        }
    }

    private void handlePublicChatMessage(ChatRoom room, ChatMessage message) {
        if (message.getType() == ChatMessageType.SYSTEM && room.getType() != ChatRoomType.GAME) {
            return;
        }

        List<String> recipients = chatRoomMemberRepository.findById_ChatRoomId(room.getId()).stream()
                .filter(member -> !member.getMemberId().equals(message.getSenderId()))
                .filter(member -> !member.isMuted())
                .map(ChatRoomMember::getMemberId)
                .map(this::findMember)
                .filter(member -> member != null && settingOf(member).isAllNotifications())
                .map(Member::getId)
                .toList();

        dispatch(NotificationDispatchRequest.of(
                NotificationType.CHAT_MESSAGE,
                recipients,
                resolvePublicChatRoomTitle(room),
                formatChatMessageBody(message),
                NotificationData.ofChatRoom(room.getId()),
                true,
                false
        ));
    }

    private void handlePartyChatMessage(ChatRoom room, ChatMessage message) {
        if (message.getType() == ChatMessageType.SYSTEM || message.getType() == ChatMessageType.ACCOUNT) {
            return;
        }

        List<String> recipients = chatRoomMemberRepository.findById_ChatRoomId(room.getId()).stream()
                .filter(member -> !member.getMemberId().equals(message.getSenderId()))
                .filter(member -> !member.isMuted())
                .map(ChatRoomMember::getMemberId)
                .toList();

        dispatch(NotificationDispatchRequest.of(
                NotificationType.CHAT_MESSAGE,
                recipients,
                message.getSenderName() + "님의 메시지",
                preview(message.getText(), 50),
                NotificationData.ofChatRoom(room.getId()),
                true,
                false
        ));
    }

    private void handleBoardPostLiked(NotificationDomainEvent.BoardPostLiked event) {
        Post post = postRepository.findByIdAndDeletedFalse(event.postId()).orElse(null);
        if (post == null || post.getAuthorId().equals(event.actorId())) {
            return;
        }

        Member author = findMember(post.getAuthorId());
        if (!isBoardLikeNotificationAllowed(author)) {
            return;
        }

        dispatch(NotificationDispatchRequest.of(
                NotificationType.POST_LIKED,
                List.of(post.getAuthorId()),
                "누군가가 내 게시글에 좋아요를 눌렀어요",
                preview(post.getTitle(), 120),
                NotificationData.ofPost(post.getId()),
                true,
                true
        ));
    }

    private void handleBoardCommentCreated(NotificationDomainEvent.BoardCommentCreated event) {
        Comment comment = commentRepository.findActiveById(event.commentId()).orElse(null);
        if (comment == null) {
            return;
        }

        Post post = comment.getPost();
        String actorId = comment.getAuthorId();
        Set<String> consumed = new LinkedHashSet<>();

        String parentAuthorId = comment.getParent() != null ? comment.getParent().getAuthorId() : null;
        if (parentAuthorId != null && !parentAuthorId.equals(actorId)) {
            Member parentAuthor = findMember(parentAuthorId);
            if (isCommentNotificationAllowed(parentAuthor)) {
                dispatch(NotificationDispatchRequest.of(
                        NotificationType.COMMENT_CREATED,
                        List.of(parentAuthorId),
                        "내 댓글에 답글이 달렸어요",
                        preview(comment.getContent(), 120),
                        NotificationData.ofPostComment(post.getId(), comment.getId()),
                        true,
                        true
                ));
                consumed.add(parentAuthorId);
            }
        }

        String postAuthorId = post.getAuthorId();
        if (!postAuthorId.equals(actorId) && !consumed.contains(postAuthorId)) {
            Member postAuthor = findMember(postAuthorId);
            if (isCommentNotificationAllowed(postAuthor)) {
                dispatch(NotificationDispatchRequest.of(
                        NotificationType.COMMENT_CREATED,
                        List.of(postAuthorId),
                        "내 게시글에 댓글이 달렸어요",
                        preview(comment.getContent(), 120),
                        NotificationData.ofPostComment(post.getId(), comment.getId()),
                        true,
                        true
                ));
                consumed.add(postAuthorId);
            }
        }

        List<String> bookmarkRecipients = postInteractionRepository.findBookmarkedUserIdsByPostId(post.getId()).stream()
                .filter(memberId -> !memberId.equals(actorId))
                .filter(memberId -> !consumed.contains(memberId))
                .filter(memberId -> {
                    Member member = findMember(memberId);
                    return isBookmarkedCommentNotificationAllowed(member);
                })
                .toList();

        dispatch(NotificationDispatchRequest.of(
                NotificationType.COMMENT_CREATED,
                bookmarkRecipients,
                "북마크한 게시글에 새 댓글이 달렸어요",
                preview(comment.getContent(), 120),
                NotificationData.ofPostComment(post.getId(), comment.getId()),
                true,
                true
        ));
    }

    private void handleNoticeCommentCreated(NotificationDomainEvent.NoticeCommentCreated event) {
        NoticeComment comment = noticeCommentRepository.findById(event.commentId()).orElse(null);
        if (comment == null || comment.getParent() == null) {
            return;
        }

        String parentAuthorId = comment.getParent().getUserId();
        if (parentAuthorId.equals(comment.getUserId())) {
            return;
        }

        Member parentAuthor = findMember(parentAuthorId);
        if (!isCommentNotificationAllowed(parentAuthor)) {
            return;
        }

        dispatch(NotificationDispatchRequest.of(
                NotificationType.COMMENT_CREATED,
                List.of(parentAuthorId),
                "내 댓글에 답글이 달렸어요",
                preview(comment.getContent(), 120),
                NotificationData.ofNoticeComment(comment.getNotice().getId(), comment.getId()),
                true,
                true
        ));
    }

    private void handleNoticeCreated(NotificationDomainEvent.NoticeCreated event) {
        Notice notice = noticeRepository.findById(event.noticeId()).orElse(null);
        if (notice == null) {
            return;
        }

        String categoryKey = mapNoticeCategoryKey(notice.getCategory());
        List<String> recipients = memberRepository.findMembersWithNoticeNotificationsEnabled().stream()
                .filter(member -> isNoticeNotificationAllowed(member, categoryKey))
                .map(Member::getId)
                .toList();

        dispatch(NotificationDispatchRequest.of(
                NotificationType.NOTICE,
                recipients,
                "새 성결대 " + (notice.getCategory() == null ? "공지" : notice.getCategory() + " 공지"),
                preview(notice.getTitle(), 120),
                NotificationData.ofNotice(notice.getId()),
                true,
                true
        ));
    }

    private void handleAppNoticeCreated(NotificationDomainEvent.AppNoticeCreated event) {
        AppNotice appNotice = appNoticeRepository.findById(event.appNoticeId()).orElse(null);
        if (appNotice == null) {
            return;
        }

        boolean urgent = appNotice.getPriority() == AppNoticePriority.HIGH;
        List<String> recipients = urgent
                ? memberRepository.findAllMemberIds()
                : memberRepository.findSystemNotificationRecipientIds();

        dispatch(NotificationDispatchRequest.of(
                NotificationType.APP_NOTICE,
                recipients,
                appNotice.getTitle(),
                preview(appNotice.getContent(), 120),
                NotificationData.ofAppNotice(appNotice.getId()),
                true,
                true
        ));
    }

    private void handleAcademicScheduleReminder(NotificationDomainEvent.AcademicScheduleReminder event) {
        AcademicSchedule schedule = academicScheduleRepository.findById(event.academicScheduleId()).orElse(null);
        if (schedule == null) {
            return;
        }

        List<String> recipients = memberRepository.findAcademicScheduleReminderRecipientIds(
                event.timing() == NotificationDomainEvent.ReminderTiming.DAY_BEFORE,
                !schedule.isPrimary()
        );

        dispatch(NotificationDispatchRequest.of(
                NotificationType.ACADEMIC_SCHEDULE,
                recipients,
                "학사 일정 리마인더",
                buildAcademicScheduleMessage(schedule, event.timing()),
                NotificationData.ofAcademicSchedule(schedule.getId()),
                true,
                true
        ));
    }

    private void dispatch(NotificationDispatchRequest request) {
        if (request.recipientIds().isEmpty()) {
            return;
        }

        try {
            notificationService.createInboxNotifications(request);
        } catch (Exception e) {
            log.warn("알림 인박스 저장 실패: type={}, recipients={}, message={}", request.type(), request.recipientIds().size(), e.getMessage());
        }
        try {
            pushNotificationService.send(request);
        } catch (Exception e) {
            log.warn("푸시 알림 전송 실패: type={}, recipients={}, message={}", request.type(), request.recipientIds().size(), e.getMessage());
        }
    }

    private Member findMember(String memberId) {
        return memberRepository.findById(memberId).orElse(null);
    }

    private List<String> findPartyRecipients(Collection<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return List.of();
        }
        return memberRepository.findPartyNotificationRecipientIds(memberIds);
    }

    private NotificationSetting settingOf(Member member) {
        return member.getNotificationSetting() == null
                ? NotificationSetting.defaultSetting()
                : member.getNotificationSetting();
    }

    private boolean isPartyNotificationAllowed(Member member) {
        if (member == null) {
            return false;
        }

        NotificationSetting setting = settingOf(member);
        return setting.isAllNotifications() && setting.isPartyNotifications();
    }

    private boolean isBoardLikeNotificationAllowed(Member member) {
        if (member == null) {
            return false;
        }

        NotificationSetting setting = settingOf(member);
        return setting.isAllNotifications() && setting.isBoardLikeNotifications();
    }

    private boolean isCommentNotificationAllowed(Member member) {
        if (member == null) {
            return false;
        }

        NotificationSetting setting = settingOf(member);
        return setting.isAllNotifications() && setting.isCommentNotifications();
    }

    private boolean isBookmarkedCommentNotificationAllowed(Member member) {
        if (member == null) {
            return false;
        }

        NotificationSetting setting = settingOf(member);
        return setting.isAllNotifications()
                && setting.isCommentNotifications()
                && setting.isBookmarkedPostCommentNotifications();
    }

    private boolean isNoticeNotificationAllowed(Member member, String categoryKey) {
        NotificationSetting setting = settingOf(member);
        if (!setting.isAllNotifications() || !setting.isNoticeNotifications()) {
            return false;
        }

        if (categoryKey == null || categoryKey.isBlank()) {
            return true;
        }

        Boolean allowed = setting.getNoticeNotificationsDetail() == null
                ? null
                : setting.getNoticeNotificationsDetail().get(categoryKey);
        return allowed == null || allowed;
    }

    private String buildAcademicScheduleMessage(
            AcademicSchedule schedule,
            NotificationDomainEvent.ReminderTiming timing
    ) {
        if (timing == NotificationDomainEvent.ReminderTiming.DAY_BEFORE) {
            return schedule.getTitle() + " 일정이 내일 시작돼요.";
        }
        return schedule.getTitle() + " 일정이 오늘 시작돼요.";
    }

    private String formatPartyCreatedTitle(Party party) {
        return party.getDeparture().getName() + " -> " + party.getDestination().getName() + " 택시 파티 등장";
    }

    private String formatPartyCreatedMessage(Party party) {
        LocalDateTime departureTime = party.getDepartureTime();
        int hour = departureTime.getHour();
        int minute = departureTime.getMinute();
        String meridiem = hour < 12 ? "오전" : "오후";
        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
        return String.format(
                Locale.KOREAN,
                "%s %d시 %02d분에 %s에서 %s로 가는 파티가 등장했어요. 동승 요청 해보세요!",
                meridiem,
                displayHour,
                minute,
                party.getDeparture().getName(),
                party.getDestination().getName()
        );
    }

    private String resolvePublicChatRoomTitle(ChatRoom room) {
        return switch (room.getType()) {
            case UNIVERSITY -> "성결대 전체 채팅방";
            case DEPARTMENT -> room.getDepartment() == null ? "학과 채팅방" : room.getDepartment() + " 채팅방";
            default -> room.getName();
        };
    }

    private String formatChatMessageBody(ChatMessage message) {
        return (message.getSenderName() == null ? "익명" : message.getSenderName()) + ": " + preview(message.getText(), 50);
    }

    private String preview(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.replace('\n', ' ').trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String mapNoticeCategoryKey(String category) {
        if (category == null || category.isBlank()) {
            return "general";
        }

        return switch (category.trim()) {
            case "새소식" -> "news";
            case "학사" -> "academy";
            case "학생" -> "student";
            case "장학/등록/학자금" -> "scholarship";
            case "입학" -> "admission";
            case "취업/진로개발/창업" -> "career";
            case "공모/행사" -> "event";
            case "교육/글로벌" -> "education";
            case "입찰구매정보" -> "procurement";
            case "사회봉사센터" -> "volunteer";
            case "장애학생지원센터" -> "accessibility";
            case "생활관" -> "dormitory";
            case "비교과" -> "extracurricular";
            default -> "general";
        };
    }
}
