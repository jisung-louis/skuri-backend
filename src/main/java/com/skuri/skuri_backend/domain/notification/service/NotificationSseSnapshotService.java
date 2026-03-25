package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationSnapshotResponse;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSseSnapshotService {

    private final UserNotificationRepository userNotificationRepository;

    public NotificationSnapshotResponse createSnapshotResponse(String memberId) {
        return new NotificationSnapshotResponse(userNotificationRepository.countByUserIdAndReadFalse(memberId));
    }
}
