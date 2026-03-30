package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftOverviewResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerRemoveResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayersSnapshotResponse;
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
public class MinecraftPublicSseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;
    private static final long SSE_RETRY_MILLIS = 3_000L;
    private static final String ENDPOINT = "/v1/sse/minecraft";

    private final MinecraftReadService minecraftReadService;

    private final Map<String, SseEmitter> subscribers = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String memberId) {
        String emitterId = memberId + ":" + UUID.randomUUID();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        subscribers.put(emitterId, emitter);
        registerLifecycle(emitterId, emitter);
        sendToOne(emitterId, "SERVER_STATE_SNAPSHOT", minecraftReadService.getOverviewOrNull());
        sendToOne(emitterId, "PLAYERS_SNAPSHOT", new MinecraftPlayersSnapshotResponse(minecraftReadService.getPlayers(memberId)));
        return emitter;
    }

    public void publishServerStateUpdated(MinecraftOverviewResponse response) {
        broadcast("SERVER_STATE_UPDATED", response);
    }

    public void publishPlayersSnapshot(java.util.List<MinecraftPlayerResponse> players) {
        broadcast("PLAYERS_SNAPSHOT", new MinecraftPlayersSnapshotResponse(players));
    }

    public void publishPlayerUpsert(MinecraftPlayerResponse player) {
        broadcast("PLAYER_UPSERT", player);
    }

    public void publishPlayerRemove(String normalizedKey) {
        broadcast("PLAYER_REMOVE", new MinecraftPlayerRemoveResponse(normalizedKey));
    }

    public void publishHeartbeat() {
        if (subscribers.isEmpty()) {
            return;
        }
        broadcast("HEARTBEAT", Map.of("timestamp", Instant.now()));
    }

    private void broadcast(String eventName, Object payload) {
        subscribers.keySet().forEach(emitterId -> sendToOne(emitterId, eventName, payload));
    }

    private void sendToOne(String emitterId, String eventName, Object payload) {
        SseEmitter emitter = subscribers.get(emitterId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .id(String.valueOf(System.currentTimeMillis()))
                    .name(eventName)
                    .reconnectTime(SSE_RETRY_MILLIS)
                    .data(payload));
        } catch (IOException | IllegalStateException e) {
            subscribers.remove(emitterId);
            log.debug("Minecraft public SSE 전송 실패로 구독 해제: endpoint={}, emitterId={}, event={}", ENDPOINT, emitterId, eventName);
        }
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
