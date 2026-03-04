package com.skuri.skuri_backend.domain.taxiparty.scheduler;

import com.skuri.skuri_backend.domain.taxiparty.service.PartyTimeoutBatchResult;
import com.skuri.skuri_backend.domain.taxiparty.service.PartyTimeoutBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyTimeoutScheduler {

    private final PartyTimeoutBatchService partyTimeoutBatchService;

    @Scheduled(cron = "0 0 */4 * * *", zone = "Asia/Seoul")
    public void endExpiredParties() {
        PartyTimeoutBatchResult result = partyTimeoutBatchService.endExpiredParties();
        if (result.targetCount() > 0) {
            log.info(
                    "파티 자동 종료 배치 완료: target={}, ended={}, conflicted={}, skipped={}",
                    result.targetCount(),
                    result.endedCount(),
                    result.conflictedCount(),
                    result.skippedCount()
            );
        }
    }
}
