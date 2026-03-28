package com.skuri.skuri_backend.domain.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "댓글 좋아요 상태 응답")
public record CommentLikeResponse(
        @Schema(description = "댓글 ID", example = "comment_uuid")
        String commentId,
        @Schema(description = "좋아요 여부", example = "true")
        boolean isLiked,
        @Schema(description = "좋아요 수", example = "3")
        int likeCount
) {
}
