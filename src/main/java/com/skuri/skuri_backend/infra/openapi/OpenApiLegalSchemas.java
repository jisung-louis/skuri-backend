package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentAdminSummaryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentDeleteResponse;
import com.skuri.skuri_backend.domain.support.dto.response.LegalDocumentResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiLegalSchemas {

    private OpenApiLegalSchemas() {
    }

    @Schema(name = "LegalDocumentApiResponse", description = "공통 API 응답 포맷")
    public record LegalDocumentApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            LegalDocumentResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "LegalDocumentAdminApiResponse", description = "공통 API 응답 포맷")
    public record LegalDocumentAdminApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            LegalDocumentAdminResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "LegalDocumentAdminSummaryListApiResponse", description = "공통 API 응답 포맷")
    public record LegalDocumentAdminSummaryListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<LegalDocumentAdminSummaryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "LegalDocumentDeleteApiResponse", description = "공통 API 응답 포맷")
    public record LegalDocumentDeleteApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            LegalDocumentDeleteResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
