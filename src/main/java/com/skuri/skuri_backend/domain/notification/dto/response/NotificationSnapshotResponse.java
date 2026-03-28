package com.skuri.skuri_backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 SSE 초기 스냅샷")
public record NotificationSnapshotResponse(
        @Schema(description = "현재 미읽음 일반 알림 수 (APP_NOTICE 제외)", example = "5")
        long unreadCount
) {
}
