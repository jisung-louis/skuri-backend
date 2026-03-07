package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "내 시간표 강의 추가 요청")
public record AddMyTimetableCourseRequest(
        @NotBlank(message = "courseId는 필수입니다.")
        @Schema(description = "강의 ID", example = "course_uuid")
        String courseId,

        @NotBlank(message = "semester는 필수입니다.")
        @Schema(description = "학기", example = "2026-1")
        String semester
) {
}
