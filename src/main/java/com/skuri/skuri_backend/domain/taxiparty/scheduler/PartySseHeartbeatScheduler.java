package com.skuri.skuri_backend.domain.taxiparty.scheduler;

import com.skuri.skuri_backend.domain.taxiparty.service.JoinRequestSseService;
import com.skuri.skuri_backend.domain.taxiparty.service.PartySseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartySseHeartbeatScheduler {

    private final PartySseService partySseService;
    private final JoinRequestSseService joinRequestSseService;

    @Scheduled(fixedDelay = 30_000L, initialDelay = 30_000L)
    public void publishHeartbeat() {
        partySseService.publishHeartbeat();
        joinRequestSseService.publishHeartbeat();
    }
}
