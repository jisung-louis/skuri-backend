package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationSnapshotResponse;
import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSseSnapshotServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @Test
    void createSnapshotResponse_미읽음개수를조회한다() {
        NotificationSseSnapshotService snapshotService = new NotificationSseSnapshotService(userNotificationRepository);
        when(userNotificationRepository.countByUserIdAndReadFalseAndTypeNot("member-1", NotificationType.APP_NOTICE))
                .thenReturn(2L);

        NotificationSnapshotResponse response = snapshotService.createSnapshotResponse("member-1");

        assertEquals(2L, response.unreadCount());
        verify(userNotificationRepository).countByUserIdAndReadFalseAndTypeNot("member-1", NotificationType.APP_NOTICE);
    }
}
