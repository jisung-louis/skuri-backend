package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "학식 메뉴 메타데이터 응답")
public record CafeteriaMenuEntryResponse(
        @Schema(description = "메뉴 항목 ID", example = "2026-02-03-rollNoodles-존슨부대찌개")
        String id,

        @Schema(description = "메뉴명", example = "존슨부대찌개")
        String title,

        @Schema(description = "보조 태그 목록")
        List<CafeteriaMenuBadgeResponse> badges,

        @Schema(description = "좋아요 수", example = "178")
        int likeCount,

        @Schema(description = "싫어요 수", example = "22")
        int dislikeCount
) {
}
