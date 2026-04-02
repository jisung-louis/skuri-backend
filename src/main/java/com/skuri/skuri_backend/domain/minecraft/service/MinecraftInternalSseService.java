package com.skuri.skuri_backend.domain.minecraft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftWhitelistSnapshotResponse;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEvent;
import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftBridgeEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinecraftInternalSseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long SSE_RETRY_MILLIS = 3_000L;
    private static final String ENDPOINT = "/internal/minecraft/stream";

    private final MinecraftReadService minecraftReadService;
    private final MinecraftBridgeEventRepository minecraftBridgeEventRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, SseEmitter> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String lastEventId) {
        String emitterId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        subscribers.put(emitterId, emitter);
        registerLifecycle(emitterId, emitter);

        if (!replayFromLastEvent(emitterId, lastEventId)) {
            sendToOne(
                    emitterId,
                    null,
                    "WHITELIST_SNAPSHOT",
                    new MinecraftWhitelistSnapshotResponse(minecraftReadService.getWhitelistEntries())
            );
        }

        return emitter;
    }

    public void publishStoredEvent(MinecraftBridgeEvent event) {
        subscribers.keySet().forEach(emitterId -> sendToOne(emitterId, event.getEventId(), event.getEventType().name(), event.getPayload()));
    }

    public void publishHeartbeat() {
        if (subscribers.isEmpty()) {
            return;
        }
        subscribers.keySet().forEach(emitterId -> sendToOne(emitterId, null, "HEARTBEAT", Map.of("timestamp", Instant.now())));
    }

    private boolean replayFromLastEvent(String emitterId, String lastEventId) {
        if (lastEventId == null || lastEventId.isBlank()) {
            return false;
        }

        MinecraftBridgeEvent anchor = minecraftBridgeEventRepository.findByEventId(lastEventId).orElse(null);
        if (anchor == null || anchor.getCreatedAt() == null || anchor.getId() == null) {
            return false;
        }

        for (MinecraftBridgeEvent event : minecraftBridgeEventRepository
                .findReplayEventsAfter(anchor.getCreatedAt(), anchor.getId())) {
            sendToOne(emitterId, event.getEventId(), event.getEventType().name(), event.getPayload());
        }
        return true;
    }

    private void sendToOne(String emitterId, String eventId, String eventName, Object payload) {
        SseEmitter emitter = subscribers.get(emitterId);
        if (emitter == null) {
            return;
        }

        try {
            Object body = deserializePayload(payload);
            SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .name(eventName)
                    .reconnectTime(SSE_RETRY_MILLIS)
                    .data(body);
            if (eventId != null) {
                event.id(eventId);
            }
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            subscribers.remove(emitterId);
            log.debug("Minecraft internal SSE 전송 실패로 구독 해제: endpoint={}, emitterId={}, event={}", ENDPOINT, emitterId, eventName);
        }
    }

    Object deserializePayload(Object payload) throws IOException {
        if (!(payload instanceof String stringPayload)) {
            return payload;
        }
        return objectMapper.readValue(stringPayload, Object.class);
    }

    private void registerLifecycle(String emitterId, SseEmitter emitter) {
        emitter.onCompletion(() -> subscribers.remove(emitterId));
        emitter.onTimeout(() -> {
            subscribers.remove(emitterId);
            safeComplete(emitter);
        });
        emitter.onError(ex -> subscribers.remove(emitterId));
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            // no-op
        }
    }
}
