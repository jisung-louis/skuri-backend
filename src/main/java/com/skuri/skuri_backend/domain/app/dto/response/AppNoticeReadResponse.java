package com.skuri.skuri_backend.domain.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "앱 공지 읽음 처리 응답")
public record AppNoticeReadResponse(
        @Schema(description = "앱 공지 ID", example = "app_notice_uuid")
        String appNoticeId,
        @Schema(description = "읽음 여부", example = "true")
        boolean isRead,
        @Schema(description = "읽음 시각", example = "2026-03-26T14:30:00")
        LocalDateTime readAt
) {
}
