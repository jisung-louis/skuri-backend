package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.entity.UserNotification;
import com.skuri.skuri_backend.domain.notification.exception.NotNotificationOwnerException;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import com.skuri.skuri_backend.domain.notification.repository.projection.UnreadCountProjection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void markRead_타인알림이면_NOT_NOTIFICATION_OWNER() {
        UserNotification notification = unreadNotification("notification-1", "other-user");
        when(userNotificationRepository.findById("notification-1")).thenReturn(Optional.of(notification));

        NotNotificationOwnerException exception = assertThrows(
                NotNotificationOwnerException.class,
                () -> notificationService.markRead("member-1", "notification-1")
        );

        assertEquals(ErrorCode.NOT_NOTIFICATION_OWNER, exception.getErrorCode());
    }

    @Test
    void markRead_실제미읽음수를기준으로이벤트를발행한다() {
        UserNotification notification = unreadNotification("notification-1", "member-1");
        when(userNotificationRepository.findById("notification-1")).thenReturn(Optional.of(notification));
        when(userNotificationRepository.countByUserIdAndReadFalse("member-1")).thenReturn(2L);

        notificationService.markRead("member-1", "notification-1");

        assertTrue(notification.isRead());
        verify(notificationSseService).publishUnreadCountChanged("member-1", 2L);
    }

    @Test
    void markAllRead_전체읽음처리후_미읽음수이벤트발행() {
        UserNotification first = unreadNotification("notification-1", "member-1");
        UserNotification second = unreadNotification("notification-2", "member-1");
        when(userNotificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc("member-1"))
                .thenReturn(List.of(first, second));
        when(userNotificationRepository.countByUserIdAndReadFalse("member-1")).thenReturn(0L);

        NotificationReadAllResponse response = notificationService.markAllRead("member-1");

        assertTrue(first.isRead());
        assertTrue(second.isRead());
        assertEquals(2, response.updatedCount());
        assertEquals(0, response.unreadCount());
        verify(notificationSseService).publishUnreadCountChanged("member-1", 0L);
    }

    @Test
    void delete_미읽음알림삭제시_미읽음수이벤트발행() {
        UserNotification notification = unreadNotification("notification-1", "member-1");
        when(userNotificationRepository.findById("notification-1")).thenReturn(Optional.of(notification));
        when(userNotificationRepository.countByUserIdAndReadFalse("member-1")).thenReturn(0L);

        notificationService.delete("member-1", "notification-1");

        verify(userNotificationRepository).delete(notification);
        verify(notificationSseService).publishUnreadCountChanged("member-1", 0L);
    }

    @Test
    void createInboxNotifications_수신자별알림과미읽음이벤트발행() {
        NotificationDispatchRequest request = NotificationDispatchRequest.of(
                NotificationType.COMMENT_CREATED,
                List.of("member-1", "member-2"),
                "내 게시글에 댓글이 달렸어요",
                "새 댓글",
                NotificationData.ofPostComment("post-1", "comment-1"),
                true,
                true
        );
        when(userNotificationRepository.saveAll(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<UserNotification> notifications = invocation.getArgument(0);
            ReflectionTestUtils.setField(notifications.get(0), "id", "notification-1");
            ReflectionTestUtils.setField(notifications.get(0), "createdAt", LocalDateTime.of(2026, 3, 8, 9, 0));
            ReflectionTestUtils.setField(notifications.get(1), "id", "notification-2");
            ReflectionTestUtils.setField(notifications.get(1), "createdAt", LocalDateTime.of(2026, 3, 8, 9, 1));
            return notifications;
        });
        when(userNotificationRepository.countUnreadByUserIds(anyCollection()))
                .thenReturn(List.of(
                        unreadCount("member-1", 1L),
                        unreadCount("member-2", 2L)
                ));

        notificationService.createInboxNotifications(request);

        InOrder inOrder = inOrder(userNotificationRepository, notificationSseService);
        inOrder.verify(userNotificationRepository).saveAll(anyList());
        inOrder.verify(userNotificationRepository).countUnreadByUserIds(anyCollection());
        verify(notificationSseService, times(2)).publishNotification(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(notificationSseService).publishUnreadCountChanged("member-1", 1L);
        verify(notificationSseService).publishUnreadCountChanged("member-2", 2L);
    }

    @Test
    void deletePartyRelatedNotifications_파티알림만조회하고미읽음수이벤트를갱신한다() {
        UserNotification partyUnread = unreadNotification("notification-1", "member-1");
        UserNotification partyRead = readNotification("notification-2", "member-1");
        UserNotification unrelated = unreadCommentNotification("notification-3", "member-1");

        when(userNotificationRepository.findByUserIdAndTypeInOrderByCreatedAtDesc(eq("member-1"), anyList()))
                .thenReturn(List.of(partyUnread, partyRead, unrelated));
        when(userNotificationRepository.countByUserIdAndReadFalse("member-1")).thenReturn(1L);

        notificationService.deletePartyRelatedNotifications("member-1", "party-1");

        ArgumentCaptor<List<UserNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(userNotificationRepository).deleteAllInBatch(captor.capture());
        assertEquals(List.of(partyUnread, partyRead), captor.getValue());
        verify(notificationSseService).publishUnreadCountChanged("member-1", 1L);
    }

    private UserNotification unreadNotification(String id, String userId) {
        UserNotification notification = UserNotification.create(
                userId,
                NotificationType.PARTY_JOIN_ACCEPTED,
                "동승 요청이 승인되었어요",
                "파티에 합류하세요!",
                NotificationData.ofPartyRequest("party-1", "request-1")
        );
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 3, 8, 9, 0));
        return notification;
    }

    private UserNotification readNotification(String id, String userId) {
        UserNotification notification = unreadNotification(id, userId);
        notification.markRead(LocalDateTime.of(2026, 3, 8, 10, 0));
        return notification;
    }

    private UserNotification unreadCommentNotification(String id, String userId) {
        UserNotification notification = UserNotification.create(
                userId,
                NotificationType.COMMENT_CREATED,
                "내 게시글에 댓글이 달렸어요",
                "새 댓글",
                NotificationData.ofPostComment("post-1", "comment-1")
        );
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 3, 8, 9, 5));
        return notification;
    }

    private UnreadCountProjection unreadCount(String userId, long unreadCount) {
        return new UnreadCountProjection() {
            @Override
            public String getUserId() {
                return userId;
            }

            @Override
            public long getUnreadCount() {
                return unreadCount;
            }
        };
    }
}
