package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.notification.dto.response.NotificationListResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationReadAllResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationResponse;
import com.skuri.skuri_backend.domain.notification.dto.response.NotificationUnreadCountResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public final class OpenApiNotificationSchemas {

    private OpenApiNotificationSchemas() {
    }

    @Schema(name = "NotificationListApiResponse", description = "공통 API 응답 포맷")
    public record NotificationListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            NotificationListResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "NotificationUnreadCountApiResponse", description = "공통 API 응답 포맷")
    public record NotificationUnreadCountApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            NotificationUnreadCountResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "NotificationApiResponse", description = "공통 API 응답 포맷")
    public record NotificationApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            NotificationResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "NotificationReadAllApiResponse", description = "공통 API 응답 포맷")
    public record NotificationReadAllApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            NotificationReadAllResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
