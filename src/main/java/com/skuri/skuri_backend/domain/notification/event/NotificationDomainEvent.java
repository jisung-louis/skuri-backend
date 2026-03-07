package com.skuri.skuri_backend.domain.notification.event;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;

public sealed interface NotificationDomainEvent permits
        NotificationDomainEvent.PartyCreated,
        NotificationDomainEvent.PartyJoinRequestCreated,
        NotificationDomainEvent.PartyJoinRequestProcessed,
        NotificationDomainEvent.PartyStatusChanged,
        NotificationDomainEvent.PartySettlementCompleted,
        NotificationDomainEvent.PartyMemberKicked,
        NotificationDomainEvent.ChatMessageCreated,
        NotificationDomainEvent.BoardPostLiked,
        NotificationDomainEvent.BoardCommentCreated,
        NotificationDomainEvent.NoticeCommentCreated,
        NotificationDomainEvent.NoticeCreated,
        NotificationDomainEvent.AppNoticeCreated,
        NotificationDomainEvent.AcademicScheduleReminder {

    record PartyCreated(String partyId) implements NotificationDomainEvent {
    }

    record PartyJoinRequestCreated(String requestId) implements NotificationDomainEvent {
    }

    record PartyJoinRequestProcessed(String requestId, JoinRequestStatus status) implements NotificationDomainEvent {
    }

    record PartyStatusChanged(String partyId, PartyStatus beforeStatus, PartyStatus afterStatus) implements NotificationDomainEvent {
    }

    record PartySettlementCompleted(String partyId) implements NotificationDomainEvent {
    }

    record PartyMemberKicked(String partyId, String memberId) implements NotificationDomainEvent {
    }

    record ChatMessageCreated(String chatRoomId, String messageId) implements NotificationDomainEvent {
    }

    record BoardPostLiked(String postId, String actorId) implements NotificationDomainEvent {
    }

    record BoardCommentCreated(String commentId) implements NotificationDomainEvent {
    }

    record NoticeCommentCreated(String commentId) implements NotificationDomainEvent {
    }

    record NoticeCreated(String noticeId) implements NotificationDomainEvent {
    }

    record AppNoticeCreated(String appNoticeId) implements NotificationDomainEvent {
    }

    record AcademicScheduleReminder(String academicScheduleId, ReminderTiming timing) implements NotificationDomainEvent {
    }

    enum ReminderTiming {
        DAY_OF,
        DAY_BEFORE
    }
}
