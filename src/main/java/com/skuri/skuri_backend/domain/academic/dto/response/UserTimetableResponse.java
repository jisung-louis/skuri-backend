package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 시간표 응답")
public record UserTimetableResponse(
        @Schema(description = "시간표 ID", nullable = true, example = "timetable_uuid")
        String id,

        @Schema(description = "학기", example = "2026-1")
        String semester,

        @Schema(description = "담긴 강의 수", example = "2")
        int courseCount,

        @Schema(description = "총 학점", example = "6")
        int totalCredits,

        @Schema(description = "시간표 강의 목록")
        List<TimetableCourseResponse> courses,

        @Schema(description = "프론트 렌더링용 슬롯 목록")
        List<TimetableSlotResponse> slots
) {
}
