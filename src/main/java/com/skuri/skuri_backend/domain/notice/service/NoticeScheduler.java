package com.skuri.skuri_backend.domain.notice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NoticeScheduler {

    private final NoticeSyncService noticeSyncService;

    @Scheduled(cron = "0 */10 8-19 * * MON-FRI", zone = "Asia/Seoul")
    public void syncNotices() {
        noticeSyncService.syncScheduled();
    }
}
