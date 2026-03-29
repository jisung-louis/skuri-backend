package com.skuri.skuri_backend.domain.board.dto.response;

import com.skuri.skuri_backend.domain.board.constant.BoardModerationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글/댓글 moderation 상태 변경 응답")
public record BoardModerationResponse(
        @Schema(description = "대상 ID", example = "post_uuid")
        String id,
        @Schema(description = "현재 moderation 상태", example = "HIDDEN")
        BoardModerationStatus moderationStatus
) {
}
