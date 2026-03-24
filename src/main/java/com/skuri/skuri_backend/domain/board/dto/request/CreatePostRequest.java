package com.skuri.skuri_backend.domain.board.dto.request;

import com.skuri.skuri_backend.domain.board.entity.PostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 작성 요청")
public record CreatePostRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        @Schema(description = "게시글 제목", example = "이번 주 택시 합승 꿀팁 공유")
        String title,

        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        @Schema(description = "게시글 본문", example = "퇴근 시간대 택시 잡는 팁 공유합니다.")
        String content,

        @NotNull(message = "category는 필수입니다.")
        @Schema(description = "게시판 카테고리", example = "GENERAL")
        PostCategory category,

        @Schema(description = "익명 여부", example = "false")
        boolean isAnonymous,

        @Size(max = 10, message = "images는 최대 10개까지 첨부할 수 있습니다.")
        @Valid
        @Schema(description = "첨부 이미지 목록. 배열 원소는 null일 수 없습니다.", nullable = true)
        List<@NotNull(message = "images 항목은 null일 수 없습니다.") @Valid CreatePostImageRequest> images
) {
}
