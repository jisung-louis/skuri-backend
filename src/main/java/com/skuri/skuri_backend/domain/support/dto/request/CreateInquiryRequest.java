package com.skuri.skuri_backend.domain.support.dto.request;

import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "문의 생성 요청")
public record CreateInquiryRequest(
        @NotNull(message = "type은 필수입니다.")
        @Schema(description = "문의 유형", example = "BUG")
        InquiryType type,

        @NotBlank(message = "subject는 필수입니다.")
        @Size(max = 200, message = "subject는 200자 이하여야 합니다.")
        @Schema(description = "문의 제목", example = "앱 오류 문의")
        String subject,

        @NotBlank(message = "content는 필수입니다.")
        @Size(max = 5000, message = "content는 5000자 이하여야 합니다.")
        @Schema(description = "문의 내용", example = "채팅 화면에서 오류가 발생합니다.")
        String content,

        @Size(max = 3, message = "attachments는 3개 이하여야 합니다.")
        @Schema(
                description = "문의 첨부 이미지 메타데이터 목록. 요청에서 생략하거나 null을 보내면 빈 배열로 처리됩니다.",
                nullable = true
        )
        List<@NotNull(message = "attachments 항목은 null일 수 없습니다.") @Valid CreateInquiryAttachmentRequest> attachments
) {
}
