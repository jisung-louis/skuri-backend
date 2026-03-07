package com.skuri.skuri_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지 읽음 처리 응답")
public record NoticeReadResponse(
        @Schema(description = "공지 ID", example = "notice_id")
        String noticeId,
        @Schema(description = "읽음 여부", example = "true")
        boolean isRead,
        @Schema(description = "읽음 시각", example = "2026-02-01T12:34:56")
        LocalDateTime readAt
) {
}
