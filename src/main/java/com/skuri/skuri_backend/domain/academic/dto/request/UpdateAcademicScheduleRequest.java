package com.skuri.skuri_backend.domain.academic.dto.request;

import com.skuri.skuri_backend.domain.academic.entity.AcademicScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "학사 일정 수정 요청")
public record UpdateAcademicScheduleRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        @Schema(description = "제목", example = "중간고사")
        String title,

        @NotNull(message = "startDate는 필수입니다.")
        @Schema(description = "시작일", example = "2026-04-15")
        LocalDate startDate,

        @NotNull(message = "endDate는 필수입니다.")
        @Schema(description = "종료일", example = "2026-04-21")
        LocalDate endDate,

        @NotNull(message = "type은 필수입니다.")
        @Schema(description = "일정 타입", example = "MULTI")
        AcademicScheduleType type,

        @NotNull(message = "isPrimary는 필수입니다.")
        @Schema(description = "주요 일정 여부", example = "true")
        Boolean isPrimary,

        @Size(max = 500, message = "description은 500자 이하여야 합니다.")
        @Schema(description = "설명", nullable = true, example = "2026학년도 1학기 중간고사")
        String description
) {
}
