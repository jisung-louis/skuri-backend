package com.skuri.skuri_backend.domain.support.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문의 첨부 이미지 메타데이터")
public record InquiryAttachment(
        @Schema(description = "원본 이미지 URL", example = "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0.jpg")
        String url,

        @Schema(description = "썸네일 이미지 URL", example = "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0_thumb.jpg")
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
