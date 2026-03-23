package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ARRIVED 메시지 정산 정보")
public record ChatArrivalDataResponse(
        @Schema(description = "택시비", example = "14000", nullable = true)
        Integer taxiFare,
        @Schema(description = "1/N 정산 인원 수(leader 포함)", example = "4", nullable = true)
        Integer splitMemberCount,
        @Schema(description = "1인당 금액", example = "3500", nullable = true)
        Integer perPersonAmount,
        @Schema(description = "정산 대상 멤버 ID 목록(leader 제외)", nullable = true)
        java.util.List<String> settlementTargetMemberIds,
        @Schema(description = "송금 계좌 정보", nullable = true)
        ChatAccountDataResponse accountData
) {
}
