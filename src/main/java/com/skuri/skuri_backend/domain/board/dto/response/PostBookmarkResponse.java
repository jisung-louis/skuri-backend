package com.skuri.skuri_backend.domain.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "북마크 상태 응답")
public record PostBookmarkResponse(
        @Schema(description = "북마크 여부", example = "true")
        boolean isBookmarked,
        @Schema(description = "북마크 수", example = "4")
        int bookmarkCount
) {
}
