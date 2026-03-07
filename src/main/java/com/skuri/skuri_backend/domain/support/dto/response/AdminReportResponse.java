package com.skuri.skuri_backend.domain.support.dto.response;

import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import com.skuri.skuri_backend.domain.support.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 신고 목록 항목")
public record AdminReportResponse(
        @Schema(description = "신고 ID", example = "report_uuid")
        String id,

        @Schema(description = "신고자 ID", example = "user_uuid")
        String reporterId,

        @Schema(description = "신고 대상 타입", example = "POST")
        ReportTargetType targetType,

        @Schema(description = "신고 대상 ID", example = "post_uuid")
        String targetId,

        @Schema(description = "신고 대상 작성자 ID", nullable = true, example = "target_user_uuid")
        String targetAuthorId,

        @Schema(description = "신고 카테고리", example = "SPAM")
        String category,

        @Schema(description = "신고 사유", example = "광고성 게시글입니다.")
        String reason,

        @Schema(description = "신고 상태", example = "PENDING")
        ReportStatus status,

        @Schema(description = "조치 내용", nullable = true, example = "DELETE_POST")
        String action,

        @Schema(description = "관리자 메모", nullable = true, example = "광고성 게시물 삭제 및 사용자 경고")
        String memo,

        @Schema(description = "생성 시각", example = "2026-03-05T12:10:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각", example = "2026-03-05T12:20:00")
        LocalDateTime updatedAt
) {
}
