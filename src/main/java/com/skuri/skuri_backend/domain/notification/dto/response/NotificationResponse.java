package com.skuri.skuri_backend.domain.notification.dto.response;

import com.skuri.skuri_backend.domain.notification.entity.NotificationType;
import com.skuri.skuri_backend.domain.notification.model.NotificationData;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "notification-uuid")
        String id,
        @Schema(description = "알림 타입", example = "PARTY_JOIN_ACCEPTED")
        NotificationType type,
        @Schema(description = "알림 제목", example = "동승 요청이 승인되었어요")
        String title,
        @Schema(description = "알림 메시지", example = "파티에 합류하세요!")
        String message,
        @Schema(description = "추가 데이터")
        NotificationData data,
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        @Schema(description = "생성 시각", example = "2026-03-08T09:00:00")
        LocalDateTime createdAt
) {
}
