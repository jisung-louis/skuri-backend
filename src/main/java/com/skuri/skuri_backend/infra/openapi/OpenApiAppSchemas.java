package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeCreateResponse;
import com.skuri.skuri_backend.domain.app.dto.response.AppNoticeResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiAppSchemas {

    private OpenApiAppSchemas() {
    }

    @Schema(name = "AppNoticeListApiResponse", description = "공통 API 응답 포맷")
    public record AppNoticeListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<AppNoticeResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AppNoticeApiResponse", description = "공통 API 응답 포맷")
    public record AppNoticeApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AppNoticeResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AppNoticeCreateApiResponse", description = "공통 API 응답 포맷")
    public record AppNoticeCreateApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AppNoticeCreateResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
