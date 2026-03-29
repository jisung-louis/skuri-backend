package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardActivityResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardRecentItemResponse;
import com.skuri.skuri_backend.domain.admin.dashboard.dto.response.AdminDashboardSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiDashboardSchemas {

    private OpenApiDashboardSchemas() {
    }

    @Schema(name = "AdminDashboardSummaryApiResponse", description = "공통 API 응답 포맷")
    public record AdminDashboardSummaryApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminDashboardSummaryResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AdminDashboardActivityApiResponse", description = "공통 API 응답 포맷")
    public record AdminDashboardActivityApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminDashboardActivityResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "AdminDashboardRecentItemListApiResponse", description = "공통 API 응답 포맷")
    public record AdminDashboardRecentItemListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<AdminDashboardRecentItemResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
