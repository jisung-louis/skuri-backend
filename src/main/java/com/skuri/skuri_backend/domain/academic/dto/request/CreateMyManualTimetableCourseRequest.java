package com.skuri.skuri_backend.domain.academic.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "내 시간표 직접 입력 강의 추가 요청")
public record CreateMyManualTimetableCourseRequest(
        @NotBlank(message = "semester는 필수입니다.")
        @Schema(description = "학기", example = "2026-1")
        String semester,

        @NotBlank(message = "name은 필수입니다.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        @Schema(description = "강의명", example = "캡스톤세미나")
        String name,

        @Size(max = 50, message = "professor는 50자 이하여야 합니다.")
        @Schema(description = "교수명", nullable = true, example = "정태현")
        String professor,

        @NotNull(message = "credits는 필수입니다.")
        @Min(value = 0, message = "credits는 0 이상이어야 합니다.")
        @Schema(description = "학점", example = "3")
        Integer credits,

        @NotNull(message = "isOnline은 필수입니다.")
        @Schema(description = "온라인 강의 여부", example = "false")
        Boolean isOnline,

        @Size(max = 100, message = "locationLabel은 100자 이하여야 합니다.")
        @Schema(description = "강의실 라벨", nullable = true, example = "공학관 502")
        String locationLabel,

        @Min(value = 1, message = "dayOfWeek는 1 이상이어야 합니다.")
        @Max(value = 6, message = "dayOfWeek는 6 이하여야 합니다.")
        @Schema(description = "요일 (1=월, 6=토)", nullable = true, example = "2")
        Integer dayOfWeek,

        @Min(value = 1, message = "startPeriod는 1 이상이어야 합니다.")
        @Max(value = 15, message = "startPeriod는 15 이하여야 합니다.")
        @Schema(description = "시작 교시", nullable = true, example = "9")
        Integer startPeriod,

        @Min(value = 1, message = "endPeriod는 1 이상이어야 합니다.")
        @Max(value = 15, message = "endPeriod는 15 이하여야 합니다.")
        @Schema(description = "종료 교시", nullable = true, example = "11")
        Integer endPeriod
) {

    @AssertTrue(message = "오프라인 강의는 locationLabel이 필수입니다.")
    @Schema(hidden = true)
    public boolean hasRequiredOfflineLocation() {
        if (isOnline == null || isOnline) {
            return true;
        }
        return hasText(locationLabel);
    }

    @AssertTrue(message = "오프라인 강의는 dayOfWeek, startPeriod, endPeriod가 모두 필요합니다.")
    @Schema(hidden = true)
    public boolean hasRequiredOfflineSchedule() {
        if (isOnline == null || isOnline) {
            return true;
        }
        return dayOfWeek != null && startPeriod != null && endPeriod != null;
    }

    @AssertTrue(message = "startPeriod는 endPeriod보다 클 수 없습니다.")
    @Schema(hidden = true)
    public boolean hasValidPeriodRange() {
        if (startPeriod == null || endPeriod == null) {
            return true;
        }
        return startPeriod <= endPeriod;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
