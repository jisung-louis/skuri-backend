package com.skuri.skuri_backend.domain.academic.dto.response;

import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "학사 일정 응답")
public record AcademicScheduleResponse(
        @Schema(description = "일정 ID", example = "schedule_uuid")
        String id,

        @Schema(description = "제목", example = "중간고사")
        String title,

        @Schema(description = "시작일", example = "2026-04-15")
        LocalDate startDate,

        @Schema(description = "종료일", example = "2026-04-21")
        LocalDate endDate,

        @Schema(description = "일정 타입", example = "MULTI")
        AcademicScheduleType type,

        @Schema(description = "주요 일정 여부", example = "true")
        boolean isPrimary,

        @Schema(description = "설명", nullable = true, example = "2026학년도 1학기 중간고사")
        String description
) {
}
