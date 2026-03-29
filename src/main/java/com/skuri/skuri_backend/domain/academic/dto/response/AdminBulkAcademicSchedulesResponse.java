package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "학사 일정 bulk sync 결과")
public record AdminBulkAcademicSchedulesResponse(
        @Schema(description = "동기화 시작일", example = "2026-03-01")
        LocalDate scopeStartDate,

        @Schema(description = "동기화 종료일", example = "2027-02-28")
        LocalDate scopeEndDate,

        @Schema(description = "생성 건수", example = "12")
        int created,

        @Schema(description = "수정 건수", example = "37")
        int updated,

        @Schema(description = "삭제 건수", example = "5")
        int deleted
) {
}
