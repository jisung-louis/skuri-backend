package com.skuri.skuri_backend.domain.notification.scheduler;

import com.skuri.skuri_backend.domain.notification.service.NotificationSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationSseHeartbeatScheduler {

    private final NotificationSseService notificationSseService;

    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void publishHeartbeat() {
        notificationSseService.publishHeartbeat();
    }
}
