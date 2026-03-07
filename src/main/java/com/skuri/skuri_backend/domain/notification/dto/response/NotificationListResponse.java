package com.skuri.skuri_backend.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 목록 응답")
public record NotificationListResponse(
        @Schema(description = "알림 목록")
        List<NotificationResponse> content,
        @Schema(description = "현재 페이지", example = "0")
        int page,
        @Schema(description = "페이지 크기", example = "20")
        int size,
        @Schema(description = "총 알림 수", example = "42")
        long totalElements,
        @Schema(description = "총 페이지 수", example = "3")
        int totalPages,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious,
        @Schema(description = "전체 미읽음 수", example = "5")
        long unreadCount
) {
}
