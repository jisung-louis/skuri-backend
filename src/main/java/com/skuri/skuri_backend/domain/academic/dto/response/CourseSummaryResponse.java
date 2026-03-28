package com.skuri.skuri_backend.domain.academic.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "강의 검색 응답")
public record CourseSummaryResponse(
        @Schema(description = "강의 ID", example = "course_uuid")
        String id,

        @Schema(description = "학기", example = "2026-1")
        String semester,

        @Schema(description = "과목 코드", example = "01255")
        String code,

        @Schema(description = "분반", example = "001")
        String division,

        @Schema(description = "강의명", example = "민법총칙")
        String name,

        @Schema(description = "학점", example = "3")
        Integer credits,

        @Schema(description = "온라인 강의 여부", example = "false")
        boolean isOnline,

        @Schema(description = "교수명", nullable = true, example = "문상혁")
        String professor,

        @Schema(description = "학과", example = "법학과")
        String department,

        @Schema(description = "학년", example = "2")
        Integer grade,

        @Schema(description = "이수 구분", example = "전공선택")
        String category,

        @Schema(description = "강의실", nullable = true, example = "영401")
        String location,

        @Schema(description = "비고", nullable = true, example = "영어 강의")
        String note,

        @Schema(description = "강의 시간 목록")
        List<CourseScheduleResponse> schedule
) {
}
