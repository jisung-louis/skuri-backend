package com.skuri.skuri_backend.domain.support.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(description = "문의 첨부 이미지 메타데이터")
public record CreateInquiryAttachmentRequest(
        @NotBlank(message = "attachments.url은 필수입니다.")
        @Schema(description = "원본 이미지 URL", example = "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0.jpg")
        String url,

        @NotBlank(message = "attachments.thumbUrl은 필수입니다.")
        @Schema(description = "썸네일 이미지 URL", example = "https://cdn.skuri.app/uploads/inquiries/2026/03/28/4f3ec1a0_thumb.jpg")
        String thumbUrl,

        @NotNull(message = "attachments.width는 필수입니다.")
        @Positive(message = "attachments.width는 1 이상이어야 합니다.")
        @Schema(description = "원본 이미지 너비", example = "800")
        Integer width,

        @NotNull(message = "attachments.height는 필수입니다.")
        @Positive(message = "attachments.height는 1 이상이어야 합니다.")
        @Schema(description = "원본 이미지 높이", example = "600")
        Integer height,

        @NotNull(message = "attachments.size는 필수입니다.")
        @Positive(message = "attachments.size는 1 이상이어야 합니다.")
        @Schema(description = "원본 파일 크기(byte)", example = "245123")
        Integer size,

        @NotBlank(message = "attachments.mime은 필수입니다.")
        @Pattern(
                regexp = "image/(jpeg|png|webp)",
                message = "attachments.mime은 image/jpeg, image/png, image/webp 중 하나여야 합니다."
        )
        @Schema(
                description = "원본 MIME 타입",
                example = "image/jpeg",
                allowableValues = {"image/jpeg", "image/png", "image/webp"}
        )
        String mime
) {
}
