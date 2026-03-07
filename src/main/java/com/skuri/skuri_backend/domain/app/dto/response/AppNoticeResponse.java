package com.skuri.skuri_backend.domain.app.dto.response;

import com.skuri.skuri_backend.domain.app.entity.AppNoticeCategory;
import com.skuri.skuri_backend.domain.app.entity.AppNoticePriority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "앱 공지 응답")
public record AppNoticeResponse(
        @Schema(description = "앱 공지 ID", example = "app_notice_uuid")
        String id,
        @Schema(description = "제목", example = "서버 점검 안내")
        String title,
        @Schema(description = "본문", example = "2월 20일 새벽 2시~4시 서버 점검이 있습니다.")
        String content,
        @Schema(description = "카테고리", example = "MAINTENANCE")
        AppNoticeCategory category,
        @Schema(description = "우선순위", example = "HIGH")
        AppNoticePriority priority,
        @Schema(description = "이미지 URL 목록")
        List<String> imageUrls,
        @Schema(description = "행동 URL", nullable = true, example = "https://status.skuri.app")
        String actionUrl,
        @Schema(description = "게시 시각", example = "2026-02-20T00:00:00")
        LocalDateTime publishedAt,
        @Schema(description = "생성 시각", example = "2026-02-19T12:00:00")
        LocalDateTime createdAt,
        @Schema(description = "수정 시각", example = "2026-02-19T13:00:00")
        LocalDateTime updatedAt
) {
}
