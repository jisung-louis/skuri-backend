package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학식 메뉴 반응 저장 결과 응답")
public record CafeteriaMenuReactionResponse(
        @Schema(description = "주간 기준 안정적인 메뉴 ID", example = "2026-W08.rollNoodles.c4973864db4f8815")
        String menuId,

        @Schema(description = "현재 사용자 반응 상태", nullable = true, implementation = CafeteriaMenuReactionType.class)
        CafeteriaMenuReactionType myReaction,

        @Schema(description = "현재 좋아요 수", example = "12")
        int likeCount,

        @Schema(description = "현재 싫어요 수", example = "3")
        int dislikeCount
) {
}
