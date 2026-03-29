package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "학사 일정 bulk sync 요청")
public record AdminBulkAcademicSchedulesRequest(
        @NotNull(message = "scopeStartDate는 필수입니다.")
        @Schema(description = "동기화 시작일", example = "2026-03-01")
        LocalDate scopeStartDate,

        @NotNull(message = "scopeEndDate는 필수입니다.")
        @Schema(description = "동기화 종료일", example = "2027-02-28")
        LocalDate scopeEndDate,

        @NotEmpty(message = "schedules는 최소 1개 이상이어야 합니다.")
        @Schema(description = "scope 범위 안에서 동기화할 일정 목록")
        List<@NotNull(message = "schedules 항목은 null일 수 없습니다.") @Valid AdminBulkAcademicScheduleItemRequest> schedules
) {
}
