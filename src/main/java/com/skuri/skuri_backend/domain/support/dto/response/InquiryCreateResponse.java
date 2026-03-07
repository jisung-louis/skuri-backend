package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "문의 생성 응답")
public record InquiryCreateResponse(
        @Schema(description = "문의 ID", example = "inquiry_uuid")
        String id,

        @Schema(description = "문의 상태", example = "PENDING")
        InquiryStatus status,

        @Schema(description = "생성 시각", example = "2026-02-03T12:00:00")
        LocalDateTime createdAt
) {
}
