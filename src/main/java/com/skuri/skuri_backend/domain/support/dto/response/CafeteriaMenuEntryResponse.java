package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "학식 메뉴 메타데이터 응답")
public record CafeteriaMenuEntryResponse(
        @Schema(description = "주간 기준 안정적인 메뉴 ID. 구조를 파싱하지 말고 그대로 사용해야 합니다.", example = "2026-W06.rollNoodles.c4973864db4f8815")
        String id,

        @Schema(description = "메뉴명", example = "존슨부대찌개")
        String title,

        @Schema(description = "보조 태그 목록")
        List<CafeteriaMenuBadgeResponse> badges,

        @Schema(description = "좋아요 수", example = "178")
        int likeCount,

        @Schema(description = "싫어요 수", example = "22")
        int dislikeCount,

        @Schema(description = "현재 로그인한 사용자의 반응 상태", nullable = true, implementation = CafeteriaMenuReactionType.class)
        CafeteriaMenuReactionType myReaction
) {
}
