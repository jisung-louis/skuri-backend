package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.event.NotificationDomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainEventNotificationListener {

    private final NotificationEventHandler notificationEventHandler;

    @EventListener
    public void onNotificationDomainEvent(NotificationDomainEvent event) {
        try {
            notificationEventHandler.handle(event);
        } catch (Exception e) {
            log.warn("도메인 알림 처리 실패: event={}, message={}", event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
