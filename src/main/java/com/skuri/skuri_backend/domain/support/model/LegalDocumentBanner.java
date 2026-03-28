package com.skuri.skuri_backend.domain.support.model;

import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerIconKey;
import com.skuri.skuri_backend.domain.support.entity.LegalDocumentBannerTone;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "법적 문서 상단 배너")
public record LegalDocumentBanner(
        @Schema(description = "배너 아이콘 키", example = "document")
        LegalDocumentBannerIconKey iconKey,

        @Schema(description = "배너 라인 목록")
        List<LegalDocumentBannerLine> lines,

        @Schema(description = "배너 제목", example = "SKURI 이용약관")
        String title,

        @Schema(description = "배너 톤", example = "green")
        LegalDocumentBannerTone tone
) {
}
