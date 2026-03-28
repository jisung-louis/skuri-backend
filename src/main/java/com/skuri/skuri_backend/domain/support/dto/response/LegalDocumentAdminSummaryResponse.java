package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 법적 문서 목록 요약 응답")
public record LegalDocumentAdminSummaryResponse(
        @Schema(description = "문서 키", example = "termsOfUse")
        String id,

        @Schema(description = "문서 제목", example = "이용약관")
        String title,

        @Schema(description = "공개 노출 여부", example = "true")
        boolean isActive,

        @Schema(description = "최종 수정 시각", example = "2026-03-28T10:00:00")
        LocalDateTime updatedAt
) {
}
