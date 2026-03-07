package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "시간표 렌더링용 슬롯 응답")
public record TimetableSlotResponse(
        @Schema(description = "강의 ID", example = "course_uuid")
        String courseId,

        @Schema(description = "강의명", example = "민법총칙")
        String courseName,

        @Schema(description = "과목 코드", example = "01255")
        String code,

        @Schema(description = "요일 (1=월, 5=금)", example = "1")
        Integer dayOfWeek,

        @Schema(description = "시작 교시", example = "3")
        Integer startPeriod,

        @Schema(description = "종료 교시", example = "4")
        Integer endPeriod,

        @Schema(description = "교수명", nullable = true, example = "문상혁")
        String professor,

        @Schema(description = "강의실", nullable = true, example = "영401")
        String location
) {
}
