package com.skuri.skuri_backend.domain.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "앱 공지 생성 응답")
public record AppNoticeCreateResponse(
        @Schema(description = "앱 공지 ID", example = "app_notice_uuid")
        String id,
        @Schema(description = "제목", example = "서버 점검 안내")
        String title,
        @Schema(description = "생성 시각", example = "2026-02-19T12:00:00")
        LocalDateTime createdAt
) {
}
