package com.skuri.skuri_backend.domain.board.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "게시글 이미지 응답")
public record PostImageResponse(
        @Schema(description = "원본 URL", example = "https://cdn.skuri.app/posts/post-1/image-1.jpg")
        String url,
        @Schema(description = "썸네일 URL", nullable = true, example = "https://cdn.skuri.app/posts/post-1/image-1-thumb.jpg")
        String thumbUrl,
        @Schema(description = "가로", nullable = true, example = "800")
        Integer width,
        @Schema(description = "세로", nullable = true, example = "600")
        Integer height,
        @Schema(description = "크기(byte)", nullable = true, example = "245123")
        Integer size,
        @Schema(description = "MIME 타입", nullable = true, example = "image/jpeg")
        String mime
) {
}
