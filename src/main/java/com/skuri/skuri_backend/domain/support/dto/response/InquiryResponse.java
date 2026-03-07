package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.entity.InquiryStatus;
import com.skuri.skuri_backend.domain.support.entity.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "내 문의 응답")
public record InquiryResponse(
        @Schema(description = "문의 ID", example = "inquiry_uuid")
        String id,

        @Schema(description = "문의 유형", example = "BUG")
        InquiryType type,

        @Schema(description = "문의 제목", example = "앱 오류 문의")
        String subject,

        @Schema(description = "문의 내용", example = "채팅 화면에서 오류가 발생합니다.")
        String content,

        @Schema(description = "문의 상태", example = "PENDING")
        InquiryStatus status,

        @Schema(description = "생성 시각", example = "2026-02-03T12:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-02-03T12:30:00")
        LocalDateTime updatedAt
) {
}
