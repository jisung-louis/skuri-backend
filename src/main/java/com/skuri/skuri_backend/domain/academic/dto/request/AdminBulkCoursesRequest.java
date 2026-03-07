package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "학기 강의 일괄 등록 요청")
public record AdminBulkCoursesRequest(
        @NotBlank(message = "semester는 필수입니다.")
        @Schema(description = "학기", example = "2026-1")
        String semester,

        @Valid
        @NotEmpty(message = "courses는 최소 1개 이상이어야 합니다.")
        @Schema(description = "강의 목록")
        List<@NotNull(message = "courses 항목은 null일 수 없습니다.") @Valid AdminBulkCourseRequest> courses
) {
}
