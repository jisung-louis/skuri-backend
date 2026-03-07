package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "학기 강의 업서트 항목")
public record AdminBulkCourseRequest(
        @NotBlank(message = "code는 필수입니다.")
        @Size(max = 20, message = "code는 20자 이하여야 합니다.")
        @Schema(description = "과목 코드", example = "01255")
        String code,

        @NotBlank(message = "division은 필수입니다.")
        @Size(max = 10, message = "division은 10자 이하여야 합니다.")
        @Schema(description = "분반", example = "001")
        String division,

        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        @Schema(description = "강의명", example = "민법총칙")
        String name,

        @NotNull(message = "credits는 필수입니다.")
        @Schema(description = "학점", example = "3")
        Integer credits,

        @Size(max = 50, message = "professor는 50자 이하여야 합니다.")
        @Schema(description = "교수명", nullable = true, example = "문상혁")
        String professor,

        @NotBlank(message = "department는 필수입니다.")
        @Size(max = 50, message = "department는 50자 이하여야 합니다.")
        @Schema(description = "학과", example = "법학과")
        String department,

        @NotNull(message = "grade는 필수입니다.")
        @Schema(description = "학년", example = "2")
        Integer grade,

        @NotBlank(message = "category는 필수입니다.")
        @Size(max = 50, message = "category는 50자 이하여야 합니다.")
        @Schema(description = "이수 구분", example = "전공선택")
        String category,

        @Size(max = 100, message = "location은 100자 이하여야 합니다.")
        @Schema(description = "강의실", nullable = true, example = "영401")
        String location,

        @Size(max = 500, message = "note는 500자 이하여야 합니다.")
        @Schema(description = "비고", nullable = true, example = "영어 강의")
        String note,

        @Valid
        @NotEmpty(message = "schedule은 최소 1개 이상이어야 합니다.")
        @Schema(description = "강의 시간 목록")
        List<@NotNull(message = "schedule 항목은 null일 수 없습니다.") @Valid AdminBulkCourseScheduleRequest> schedule
) {
}
