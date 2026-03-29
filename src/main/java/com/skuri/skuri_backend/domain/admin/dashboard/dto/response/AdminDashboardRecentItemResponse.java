package com.skuri.skuri_backend.domain.admin.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 대시보드 최근 항목 응답")
public record AdminDashboardRecentItemResponse(
        @Schema(description = "항목 타입")
        AdminDashboardRecentItemType type,
        @Schema(description = "원본 도메인 ID", example = "inquiry-1")
        String id,
        @Schema(description = "항목 제목", example = "계정 문의")
        String title,
        @Schema(description = "항목 부제", example = "PENDING · member-1")
        String subtitle,
        @Schema(description = "항목 상태 문자열", example = "PENDING")
        String status,
        @Schema(description = "생성 시각", example = "2026-03-29T17:00:00")
        LocalDateTime createdAt
) {
}
