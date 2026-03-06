package com.skuri.skuri_backend.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 작성 요청")
public record CreateCommentRequest(
        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 1000, message = "content는 1000자 이하여야 합니다.")
        @Schema(description = "댓글 본문", example = "좋은 정보 감사합니다!")
        String content,

        @Schema(description = "익명 여부", example = "true")
        boolean isAnonymous,

        @Schema(description = "부모 댓글 ID(대댓글 작성 시)", nullable = true, example = "comment_uuid")
        String parentId
) {
}
