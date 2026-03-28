package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "강의 시간 슬롯 요청")
public record AdminBulkCourseScheduleRequest(
        @NotNull(message = "dayOfWeek는 필수입니다.")
        @Schema(description = "요일 (1=월, 6=토)", example = "1")
        Integer dayOfWeek,

        @NotNull(message = "startPeriod는 필수입니다.")
        @Schema(description = "시작 교시", example = "3")
        Integer startPeriod,

        @NotNull(message = "endPeriod는 필수입니다.")
        @Schema(description = "종료 교시", example = "4")
        Integer endPeriod
) {
}
