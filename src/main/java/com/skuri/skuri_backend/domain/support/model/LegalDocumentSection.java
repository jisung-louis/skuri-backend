package com.skuri.skuri_backend.domain.support.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "법적 문서 본문 섹션")
public record LegalDocumentSection(
        @Schema(description = "섹션 ID", example = "article-01")
        String id,

        @Schema(description = "섹션 본문 문단 목록")
        List<String> paragraphs,

        @Schema(description = "섹션 제목", example = "제1조(목적)")
        String title
) {
}
