package com.skuri.skuri_backend.domain.taxiparty.service;

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
class PartySseServiceTest {

    @Mock
    private PartySseSnapshotService partySseSnapshotService;

    @Test
    void subscribeParties_호출시_스냅샷준비수행() {
        PartySseService partySseService = new PartySseService(partySseSnapshotService);
        when(partySseSnapshotService.createSnapshotPayload()).thenReturn(Map.of("parties", java.util.List.of()));

        SseEmitter emitter = partySseService.subscribeParties("member-1");

        assertNotNull(emitter);
        verify(partySseSnapshotService).createSnapshotPayload();
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishHeartbeat_전송실패시_구독해제() throws Exception {
        PartySseService partySseService = new PartySseService(partySseSnapshotService);
        Map<String, Object> subscribers = (Map<String, Object>) ReflectionTestUtils.getField(partySseService, "subscribers");
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

        partySseService.publishHeartbeat();

        assertTrue(subscribers.isEmpty());
        assertEquals(0, completeCount.get());
    }

    private Object createSubscriber(String memberId, SseEmitter emitter) throws Exception {
        Class<?> subscriberClass = Class.forName("com.skuri.skuri_backend.domain.taxiparty.service.PartySseService$SseSubscriber");
        Constructor<?> constructor = subscriberClass.getDeclaredConstructor(String.class, SseEmitter.class);
        constructor.setAccessible(true);
        return constructor.newInstance(memberId, emitter);
    }
}
