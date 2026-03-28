package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 시간표 학기 옵션")
public record TimetableSemesterOptionResponse(
        @Schema(description = "학기 ID", example = "2026-1")
        String id,

        @Schema(description = "학기 라벨", example = "2026-1학기")
        String label
) {
}
