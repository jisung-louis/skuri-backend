package com.skuri.skuri_backend.domain.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Schema(description = "학식 메뉴 메타데이터 요청")
public record CafeteriaMenuEntryRequest(
        @NotBlank(message = "menuEntries.title은 필수입니다.")
        @Schema(description = "메뉴명", example = "존슨부대찌개")
        String title,

        @Schema(description = "보조 태그 목록", nullable = true)
        List<@Valid CafeteriaMenuBadgeRequest> badges,

        @Schema(description = "좋아요 수. deprecated 필드이며 실제 사용자 반응 집계가 사용됩니다. 전달해도 저장 시 반영되지 않습니다.", example = "178", nullable = true, deprecated = true)
        Integer likeCount,

        @Schema(description = "싫어요 수. deprecated 필드이며 실제 사용자 반응 집계가 사용됩니다. 전달해도 저장 시 반영되지 않습니다.", example = "22", nullable = true, deprecated = true)
        Integer dislikeCount
) {
}
