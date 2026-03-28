package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.model.LegalDocumentBanner;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "관리자 법적 문서 상세 응답")
public record LegalDocumentAdminResponse(
        @Schema(description = "문서 키", example = "termsOfUse")
        String id,

        @Schema(description = "문서 제목", example = "이용약관")
        String title,

        @Schema(description = "상단 배너")
        LegalDocumentBanner banner,

        @Schema(description = "본문 섹션 목록")
        List<LegalDocumentSection> sections,

        @Schema(description = "하단 안내 문구")
        List<String> footerLines,

        @Schema(description = "공개 노출 여부", example = "true")
        boolean isActive,

        @Schema(description = "생성 시각", example = "2026-03-28T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-03-28T10:00:00")
        LocalDateTime updatedAt
) {
}
