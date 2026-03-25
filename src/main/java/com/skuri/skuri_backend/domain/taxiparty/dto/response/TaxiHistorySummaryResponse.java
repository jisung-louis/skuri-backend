package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "택시 이용 내역 요약")
public record TaxiHistorySummaryResponse(
        @Schema(description = "취소 포함 전체 이용 내역 수", example = "5")
        int totalRideCount,
        @Schema(description = "완료된 이용 횟수", example = "4")
        int completedRideCount,
        @Schema(description = "절약한 택시비 합계", example = "9374")
        int savedFareAmount
) {
}
