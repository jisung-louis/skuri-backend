package com.skuri.skuri_backend.domain.notice.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지 동기화 실행 응답")
public record NoticeSyncResponse(
        @Schema(description = "신규 생성 수", example = "15")
        int created,
        @Schema(description = "업데이트 수", example = "3")
        int updated,
        @Schema(description = "스킵 수", example = "42")
        int skipped,
        @Schema(description = "실패 수", example = "2")
        int failed,
        @Schema(description = "동기화 완료 시각", example = "2026-02-19T12:00:00")
        LocalDateTime syncedAt
) {
}
