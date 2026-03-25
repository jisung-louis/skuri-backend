package com.skuri.skuri_backend.domain.notification.service;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationSnapshotResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSseServiceTest {

    @Mock
    private NotificationSseSnapshotService notificationSseSnapshotService;

    @Test
    void subscribe_호출시_스냅샷준비수행() {
        NotificationSseService notificationSseService = new NotificationSseService(notificationSseSnapshotService);
        when(notificationSseSnapshotService.createSnapshotResponse("member-1"))
                .thenReturn(new NotificationSnapshotResponse(3L));

        SseEmitter emitter = notificationSseService.subscribe("member-1");

        assertNotNull(emitter);
        verify(notificationSseSnapshotService).createSnapshotResponse("member-1");
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishHeartbeat_전송실패시_complete없이_구독해제한다() throws Exception {
        NotificationSseService notificationSseService = new NotificationSseService(notificationSseSnapshotService);
        Map<String, Object> subscribers = (Map<String, Object>) ReflectionTestUtils.getField(notificationSseService, "subscribers");
        AtomicInteger completeCount = new AtomicInteger();

        SseEmitter failingEmitter = new SseEmitter() {
            @Override
            public synchronized void send(SseEventBuilder builder) throws IOException {
                throw new IOException("send failed");
            }

            @Override
            public synchronized void complete() {
                completeCount.incrementAndGet();
            }
        };

        subscribers.put("member-1:emitter-1", createSubscriber("member-1", failingEmitter));

        notificationSseService.publishHeartbeat();

        assertTrue(subscribers.isEmpty());
        assertEquals(0, completeCount.get());
    }

    private Object createSubscriber(String memberId, SseEmitter emitter) throws Exception {
        Class<?> subscriberClass = Class.forName("com.skuri.skuri_backend.domain.notification.service.NotificationSseService$SseSubscriber");
        Constructor<?> constructor = subscriberClass.getDeclaredConstructor(String.class, SseEmitter.class);
        constructor.setAccessible(true);
        return constructor.newInstance(memberId, emitter);
    }
}
