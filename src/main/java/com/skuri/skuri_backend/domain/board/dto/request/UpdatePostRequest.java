package com.skuri.skuri_backend.domain.board.dto.request;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "게시글 수정 요청")
public record UpdatePostRequest(
        @Size(min = 1, max = 200, message = "title은 1자 이상 200자 이하여야 합니다.")
        @Schema(description = "게시글 제목", nullable = true, example = "수정된 게시글 제목")
        String title,

        @Size(min = 1, max = 5000, message = "content는 1자 이상 5000자 이하여야 합니다.")
        @Schema(description = "게시글 본문", nullable = true, example = "수정된 본문 내용")
        String content,

        @Schema(description = "게시판 카테고리", nullable = true, example = "QUESTION")
        PostCategory category
) {
}
