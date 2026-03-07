package com.skuri.skuri_backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "전체 읽음 처리 결과")
public record NotificationReadAllResponse(
        @Schema(description = "이번 요청으로 읽음 처리된 알림 수", example = "3")
        long updatedCount,
        @Schema(description = "남은 미읽음 알림 수", example = "0")
        long unreadCount
) {
}
