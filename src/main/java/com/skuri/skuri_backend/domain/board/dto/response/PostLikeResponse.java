package com.skuri.skuri_backend.domain.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "좋아요 상태 응답")
public record PostLikeResponse(
        @Schema(description = "좋아요 여부", example = "true")
        boolean isLiked,
        @Schema(description = "좋아요 수", example = "11")
        int likeCount
) {
}
