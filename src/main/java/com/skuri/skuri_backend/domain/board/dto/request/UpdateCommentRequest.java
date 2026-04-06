package com.skuri.skuri_backend.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "댓글 수정 요청")
public record UpdateCommentRequest(
        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 1000, message = "content는 1000자 이하여야 합니다.")
        @Schema(description = "수정할 댓글 본문", example = "수정된 댓글 내용")
        String content,
        @Schema(description = "익명 여부. 생략하면 기존 값을 유지합니다.", example = "true", nullable = true)
        Boolean isAnonymous
) {
    public UpdateCommentRequest(String content) {
        this(content, null);
    }
}
