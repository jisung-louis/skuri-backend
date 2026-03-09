package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationSnapshotResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import com.skuri.skuri_backend.domain.notification.repository.UserNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long SSE_RETRY_MILLIS = 3_000L;

    private final UserNotificationRepository userNotificationRepository;

    private final Map<String, SseSubscriber> subscribers = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public SseEmitter subscribe(String memberId) {
        String emitterId = memberId + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        subscribers.put(emitterId, new SseSubscriber(memberId, emitter));

        emitter.onCompletion(() -> subscribers.remove(emitterId));
        emitter.onTimeout(() -> {
            subscribers.remove(emitterId);
            emitter.complete();
        });
        emitter.onError(ex -> {
            subscribers.remove(emitterId);
            emitter.completeWithError(ex);
        });

        sendToOne(
                emitterId,
                "SNAPSHOT",
                new NotificationSnapshotResponse(userNotificationRepository.countByUserIdAndReadFalse(memberId))
        );
        return emitter;
    }

    public void publishNotification(String memberId, NotificationResponse response) {
        broadcastToMember(memberId, "NOTIFICATION", response);
    }

    public void publishUnreadCountChanged(String memberId, long unreadCount) {
        broadcastToMember(memberId, "UNREAD_COUNT_CHANGED", new NotificationUnreadCountResponse(unreadCount));
    }

    public void publishHeartbeat() {
        if (subscribers.isEmpty()) {
            return;
        }

        broadcast("HEARTBEAT", Map.of("timestamp", LocalDateTime.now()));
    }

    public void closeSubscriptionsForMember(String memberId) {
        subscribers.forEach((emitterId, subscriber) -> {
            if (!memberId.equals(subscriber.memberId())) {
                return;
            }
            subscribers.remove(emitterId);
            try {
                subscriber.emitter().complete();
            } catch (Exception ignored) {
                // no-op
            }
        });
    }

    private void broadcast(String eventType, Object payload) {
        subscribers.keySet().forEach(emitterId -> sendToOne(emitterId, eventType, payload));
    }

    private void broadcastToMember(String memberId, String eventType, Object payload) {
        if (memberId == null || subscribers.isEmpty()) {
            return;
        }

        subscribers.forEach((emitterId, subscriber) -> {
            if (memberId.equals(subscriber.memberId())) {
                sendToOne(emitterId, eventType, payload);
            }
        });
    }

    private void sendToOne(String emitterId, String eventType, Object payload) {
        SseSubscriber subscriber = subscribers.get(emitterId);
        if (subscriber == null) {
            return;
        }

        try {
            subscriber.emitter().send(
                    SseEmitter.event()
                            .id(String.valueOf(System.currentTimeMillis()))
                            .name(eventType)
                            .reconnectTime(SSE_RETRY_MILLIS)
                            .data(payload)
            );
        } catch (IOException | IllegalStateException e) {
            subscribers.remove(emitterId);
            try {
                subscriber.emitter().complete();
            } catch (Exception ignored) {
                // no-op
            }
            log.debug("알림 SSE 전송 실패로 구독 해제: emitterId={}, eventType={}", emitterId, eventType);
        }
    }

    private record SseSubscriber(
            String memberId,
            SseEmitter emitter
    ) {
    }
}
