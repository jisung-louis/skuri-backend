package com.skuri.skuri_backend.domain.image.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 업로드 결과")
public record ImageUploadResponse(
        @Schema(description = "원본 이미지 URL", example = "https://cdn.skuri.app/uploads/posts/2026/03/10/4f3ec1a0.jpg")
        String url,

        @Schema(description = "썸네일 이미지 URL", example = "https://cdn.skuri.app/uploads/posts/2026/03/10/4f3ec1a0_thumb.jpg")
        String thumbUrl,

        @Schema(description = "원본 이미지 너비", example = "800")
        Integer width,

        @Schema(description = "원본 이미지 높이", example = "600")
        Integer height,

        @Schema(description = "원본 파일 크기(byte)", example = "245123")
        Integer size,

        @Schema(description = "원본 MIME 타입", example = "image/jpeg")
        String mime
) {
}
