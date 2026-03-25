package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "택시 이용 내역 아이템")
public record TaxiHistoryItemResponse(
        @Schema(description = "파티 ID", example = "party-20260304-001")
        String id,
        @Schema(description = "출발지 라벨", example = "성결대학교")
        String departureLabel,
        @Schema(description = "도착지 라벨", example = "안양역")
        String arrivalLabel,
        @Schema(description = "내역 기준 시각", example = "2026-03-04T21:00:00")
        LocalDateTime dateTime,
        @Schema(description = "탑승 인원 수(리더 포함)", example = "3")
        int passengerCount,
        @Schema(description = "내 결제 금액(1인당 금액 기준)", example = "5000", nullable = true)
        Integer paymentAmount,
        @Schema(description = "내 역할", example = "LEADER")
        TaxiHistoryRole role,
        @Schema(description = "이용 내역 상태", example = "COMPLETED")
        TaxiHistoryStatus status
) {
}
