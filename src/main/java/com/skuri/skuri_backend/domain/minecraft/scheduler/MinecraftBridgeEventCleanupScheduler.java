package com.skuri.skuri_backend.domain.minecraft.scheduler;

import com.skuri.skuri_backend.domain.minecraft.repository.MinecraftBridgeEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class MinecraftBridgeEventCleanupScheduler {

    private final MinecraftBridgeEventRepository minecraftBridgeEventRepository;

    @Scheduled(fixedDelay = 60L * 60L * 1000L, initialDelay = 60L * 60L * 1000L)
    public void cleanupExpiredEvents() {
        minecraftBridgeEventRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
