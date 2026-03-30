package com.skuri.skuri_backend.domain.minecraft.scheduler;

import com.skuri.skuri_backend.domain.minecraft.service.MinecraftInternalSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MinecraftInternalSseHeartbeatScheduler {

    private final MinecraftInternalSseService minecraftInternalSseService;

    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void publishHeartbeat() {
        minecraftInternalSseService.publishHeartbeat();
    }
}
