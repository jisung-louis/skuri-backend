package com.skuri.skuri_backend.domain.support.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "앱 버전 관리자 저장 응답")
public record AppVersionAdminUpdateResponse(
        @Schema(description = "플랫폼", example = "ios")
        String platform,

        @Schema(description = "최소 허용 버전", example = "1.6.0")
        String minimumVersion,

        @Schema(description = "강제 업데이트 여부", example = "true")
        boolean forceUpdate,

        @Schema(description = "최종 수정 시각", example = "2026-02-19T12:00:00")
        LocalDateTime updatedAt
) {
}
