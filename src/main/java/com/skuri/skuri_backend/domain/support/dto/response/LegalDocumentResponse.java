package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.model.LegalDocumentBanner;
import com.skuri.skuri_backend.domain.support.model.LegalDocumentSection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "법적 문서 공개 응답")
public record LegalDocumentResponse(
        @Schema(description = "문서 키", example = "termsOfUse")
        String id,

        @Schema(description = "문서 제목", example = "이용약관")
        String title,

        @Schema(description = "상단 배너")
        LegalDocumentBanner banner,

        @Schema(description = "본문 섹션 목록")
        List<LegalDocumentSection> sections,

        @Schema(description = "하단 안내 문구")
        List<String> footerLines
) {
}
