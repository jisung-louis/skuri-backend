package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.skuri.skuri_backend.domain.taxiparty.entity.JoinRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동승 요청 수락 응답")
public record JoinRequestAcceptResponse(
        @Schema(description = "요청 ID", example = "request_uuid")
        String id,
        @Schema(description = "요청 상태", example = "ACCEPTED")
        JoinRequestStatus status,
        @Schema(description = "파티 ID", example = "party_uuid")
        String partyId
) {
}
