package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 파티 목록 아이템")
public record MyPartyResponse(
        @Schema(description = "파티 ID", example = "party_uuid")
        String id,
        @Schema(description = "파티 상태", example = "OPEN")
        PartyStatus status,
        @Schema(description = "출발지")
        PartyLocationResponse departure,
        @Schema(description = "목적지")
        PartyLocationResponse destination,
        @Schema(description = "리더 여부", example = "false")
        boolean isLeader,
        @Schema(description = "정산 정보", nullable = true)
        SettlementSummaryResponse settlement
) {
}
