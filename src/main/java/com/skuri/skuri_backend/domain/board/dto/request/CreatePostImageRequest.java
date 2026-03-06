package com.skuri.skuri_backend.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "게시글 이미지 입력")
public record CreatePostImageRequest(
        @NotBlank(message = "url은 필수입니다.")
        @Size(max = 500, message = "url은 500자 이하여야 합니다.")
        @Schema(description = "원본 이미지 URL", example = "https://cdn.skuri.app/posts/post-1/image-1.jpg")
        String url,

        @Size(max = 500, message = "thumbUrl은 500자 이하여야 합니다.")
        @Schema(description = "썸네일 URL", nullable = true, example = "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg")
        String thumbUrl,

        @Schema(description = "이미지 너비", nullable = true, example = "800")
        Integer width,

        @Schema(description = "이미지 높이", nullable = true, example = "600")
        Integer height,

        @Schema(description = "이미지 크기(byte)", nullable = true, example = "245123")
        Integer size,

        @Size(max = 50, message = "mime은 50자 이하여야 합니다.")
        @Schema(description = "MIME 타입", nullable = true, example = "image/jpeg")
        String mime
) {
}
