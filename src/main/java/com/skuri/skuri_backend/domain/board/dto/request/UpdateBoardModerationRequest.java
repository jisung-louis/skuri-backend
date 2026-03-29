package com.skuri.skuri_backend.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "게시글/댓글 moderation 상태 변경 요청")
public record UpdateBoardModerationRequest(
        @NotBlank(message = "status는 필수입니다.")
        @Schema(
                description = "변경할 moderation 상태",
                example = "HIDDEN",
                allowableValues = {"VISIBLE", "HIDDEN", "DELETED"}
        )
        String status
) {
}
