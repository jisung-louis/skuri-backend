package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "신고 생성 응답")
public record ReportCreateResponse(
        @Schema(description = "신고 ID", example = "report_uuid")
        String id,

        @Schema(description = "신고 상태", example = "PENDING")
        ReportStatus status,

        @Schema(description = "생성 시각", example = "2026-03-05T12:10:00")
        LocalDateTime createdAt
) {
}
