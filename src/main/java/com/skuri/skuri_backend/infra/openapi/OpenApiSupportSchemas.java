package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.common.dto.PageResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AdminInquiryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AdminReportResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionAdminUpdateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.AppVersionResponse;
import com.skuri.skuri_backend.domain.support.dto.response.CafeteriaMenuResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryCreateResponse;
import com.skuri.skuri_backend.domain.support.dto.response.InquiryResponse;
import com.skuri.skuri_backend.domain.support.dto.response.ReportCreateResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiSupportSchemas {

    private OpenApiSupportSchemas() {
    }

    @Schema(name = "SupportAppVersionApiResponse", description = "공통 API 응답 포맷")
    public record AppVersionApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AppVersionResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportAppVersionAdminUpdateApiResponse", description = "공통 API 응답 포맷")
    public record AppVersionAdminUpdateApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AppVersionAdminUpdateResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportCafeteriaMenuApiResponse", description = "공통 API 응답 포맷")
    public record CafeteriaMenuApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            CafeteriaMenuResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportInquiryCreateApiResponse", description = "공통 API 응답 포맷")
    public record InquiryCreateApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            InquiryCreateResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportInquiryListApiResponse", description = "공통 API 응답 포맷")
    public record InquiryListApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            List<InquiryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportAdminInquiryPageApiResponse", description = "공통 API 응답 포맷")
    public record AdminInquiryPageApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PageResponse<AdminInquiryResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportAdminInquiryApiResponse", description = "공통 API 응답 포맷")
    public record AdminInquiryApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminInquiryResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportReportCreateApiResponse", description = "공통 API 응답 포맷")
    public record ReportCreateApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            ReportCreateResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportAdminReportPageApiResponse", description = "공통 API 응답 포맷")
    public record AdminReportPageApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            PageResponse<AdminReportResponse> data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "SupportAdminReportApiResponse", description = "공통 API 응답 포맷")
    public record AdminReportApiResponse(
            @Schema(description = "요청 성공 여부")
            boolean success,
            @Schema(description = "성공 시 응답 데이터", nullable = true)
            AdminReportResponse data,
            @Schema(description = "에러 메시지", nullable = true)
            String message,
            @Schema(description = "에러 코드", nullable = true)
            String errorCode,
            @Schema(description = "에러 발생 시각", nullable = true)
            LocalDateTime timestamp
    ) {
    }
}
