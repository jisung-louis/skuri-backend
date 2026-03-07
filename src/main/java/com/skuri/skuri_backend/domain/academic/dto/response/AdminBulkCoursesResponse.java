package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학기 강의 일괄 등록/삭제 결과")
public record AdminBulkCoursesResponse(
        @Schema(description = "학기", example = "2026-1")
        String semester,

        @Schema(description = "생성 건수", example = "120")
        int created,

        @Schema(description = "수정 건수", example = "5")
        int updated,

        @Schema(description = "삭제 건수", example = "3")
        int deleted
) {
}
