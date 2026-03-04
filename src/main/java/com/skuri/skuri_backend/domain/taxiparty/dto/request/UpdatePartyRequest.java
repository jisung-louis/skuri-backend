package com.skuri.skuri_backend.domain.taxiparty.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "파티 수정 요청 (departureTime, detail만 허용)")
@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdatePartyRequest(
        @Schema(description = "출발 시각", example = "2026-03-03T14:00:00", nullable = true)
        @Future(message = "departureTime은 현재 이후 시각이어야 합니다.")
        LocalDateTime departureTime,

        @Schema(description = "상세 설명", example = "10분 후 출발합니다", nullable = true)
        @Size(max = 500, message = "detail은 500자 이하여야 합니다.")
        String detail
) {

    @AssertTrue(message = "departureTime 또는 detail 중 최소 하나는 입력해야 합니다.")
    public boolean isUpdatableFieldPresent() {
        return departureTime != null || detail != null;
    }
}
