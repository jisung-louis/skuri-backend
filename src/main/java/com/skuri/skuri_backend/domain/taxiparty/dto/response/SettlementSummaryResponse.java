package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "파티 정산 응답")
public record SettlementSummaryResponse(
        @Schema(description = "정산 상태", example = "PENDING")
        SettlementStatus status,
        @Schema(description = "1인당 금액", example = "3500", nullable = true)
        Integer perPersonAmount,
        @Schema(description = "멤버별 정산 상태", nullable = true)
        List<MemberSettlementResponse> memberSettlements
) {
}
