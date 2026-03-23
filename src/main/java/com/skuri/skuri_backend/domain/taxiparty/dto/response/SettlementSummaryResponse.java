package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "파티 정산 응답")
public record SettlementSummaryResponse(
        @Schema(description = "정산 상태", example = "PENDING")
        SettlementStatus status,
        @Schema(description = "총 택시비", example = "14000", nullable = true)
        Integer taxiFare,
        @Schema(description = "N빵 인원 수(리더 포함)", example = "3", nullable = true)
        Integer splitMemberCount,
        @Schema(description = "1인당 금액", example = "3500", nullable = true)
        Integer perPersonAmount,
        @Schema(description = "정산 대상 non-leader 멤버 ID 목록", nullable = true)
        List<String> settlementTargetMemberIds,
        @Schema(description = "정산 계좌 snapshot", nullable = true)
        SettlementAccountResponse account,
        @Schema(description = "멤버별 정산 상태", nullable = true)
        List<MemberSettlementResponse> memberSettlements
) {
}
