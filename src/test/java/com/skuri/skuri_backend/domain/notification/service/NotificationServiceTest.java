package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.entity.UserNotification;
import com.skuri.skuri_backend.domain.notification.exception.NotNotificationOwnerException;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
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
        when(userNotificationRepository.countByUserIdAndReadFalse("member-1")).thenReturn(1L);
        when(userNotificationRepository.countByUserIdAndReadFalse("member-2")).thenReturn(2L);

        notificationService.createInboxNotifications(request);

        verify(notificationSseService, times(2)).publishNotification(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        verify(notificationSseService).publishUnreadCountChanged("member-1", 1L);
        verify(notificationSseService).publishUnreadCountChanged("member-2", 2L);
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
}
