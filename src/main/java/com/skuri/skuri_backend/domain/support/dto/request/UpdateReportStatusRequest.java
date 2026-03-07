package com.skuri.skuri_backend.domain.support.dto.request;

import com.skuri.skuri_backend.domain.support.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "신고 상태 변경 요청")
public record UpdateReportStatusRequest(
        @NotNull(message = "status는 필수입니다.")
        @Schema(description = "변경할 신고 상태", example = "ACTIONED")
        ReportStatus status,

        @Size(max = 100, message = "action은 100자 이하여야 합니다.")
        @Schema(description = "조치 내용", nullable = true, example = "DELETE_POST")
        String action,

        @Size(max = 500, message = "memo는 500자 이하여야 합니다.")
        @Schema(description = "관리자 메모", nullable = true, example = "광고성 게시물 삭제 및 사용자 경고")
        String memo
) {
}
