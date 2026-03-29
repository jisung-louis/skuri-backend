package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "학사 일정 bulk sync 항목")
public record AdminBulkAcademicScheduleItemRequest(
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 200, message = "title은 200자 이하여야 합니다.")
        @Schema(description = "제목", example = "입학식 / 개강")
        String title,

        @NotNull(message = "startDate는 필수입니다.")
        @Schema(description = "시작일", example = "2026-03-03")
        LocalDate startDate,

        @NotNull(message = "endDate는 필수입니다.")
        @Schema(description = "종료일", example = "2026-03-03")
        LocalDate endDate,

        @NotBlank(message = "type은 필수입니다.")
        @Size(max = 20, message = "type은 20자 이하여야 합니다.")
        @Schema(
                description = "일정 타입. bulk sync API는 single/multi 소문자도 허용합니다.",
                allowableValues = {"SINGLE", "MULTI", "single", "multi"},
                example = "single"
        )
        String type,

        @NotNull(message = "isPrimary는 필수입니다.")
        @Schema(description = "주요 일정 여부", example = "true")
        Boolean isPrimary,

        @Size(max = 500, message = "description은 500자 이하여야 합니다.")
        @Schema(description = "설명", nullable = true, example = "정상수업")
        String description
) {
}
