package com.skuri.skuri_backend.domain.board.dto.request;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 수정 요청")
public record UpdatePostRequest(
        @Size(min = 1, max = 200, message = "title은 1자 이상 200자 이하여야 합니다.")
        @Schema(description = "게시글 제목", nullable = true, example = "수정된 게시글 제목")
        String title,

        @Size(min = 1, max = 5000, message = "content는 1자 이상 5000자 이하여야 합니다.")
        @Schema(description = "게시글 본문", nullable = true, example = "수정된 본문 내용")
        String content,

        @Schema(description = "게시판 카테고리", nullable = true, example = "QUESTION")
        PostCategory category,

        @Schema(description = "익명 여부. 값을 전달하면 게시글 익명 상태를 변경하고, 생략하면 기존 값을 유지합니다.", nullable = true, example = "true")
        Boolean isAnonymous,

        @Size(max = 10, message = "images는 최대 10개까지 첨부할 수 있습니다.")
        @Valid
        @Schema(
                description = "첨부 이미지 목록. 필드를 전달하면 전체 이미지 목록을 이 순서대로 교체하고, 빈 배열이면 이미지를 모두 제거합니다. 생략 또는 null이면 기존 이미지를 유지합니다. 배열 원소는 null일 수 없습니다.",
                nullable = true
        )
        List<@NotNull(message = "images 항목은 null일 수 없습니다.") @Valid CreatePostImageRequest> images
) {
}
