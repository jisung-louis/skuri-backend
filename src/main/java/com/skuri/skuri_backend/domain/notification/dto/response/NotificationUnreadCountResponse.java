package com.skuri.skuri_backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미읽음 알림 수 응답")
public record NotificationUnreadCountResponse(
        @Schema(description = "미읽음 알림 수", example = "5")
        long count
) {
}
