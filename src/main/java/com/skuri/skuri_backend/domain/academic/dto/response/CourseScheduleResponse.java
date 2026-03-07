package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의 시간 슬롯")
public record CourseScheduleResponse(
        @Schema(description = "요일 (1=월, 5=금)", example = "1")
        Integer dayOfWeek,

        @Schema(description = "시작 교시", example = "3")
        Integer startPeriod,

        @Schema(description = "종료 교시", example = "4")
        Integer endPeriod
) {
}
