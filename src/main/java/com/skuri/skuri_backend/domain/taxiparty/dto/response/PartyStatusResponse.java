package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyEndReason;
import com.skuri.skuri_backend.domain.taxiparty.entity.PartyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 상태 변경 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PartyStatusResponse(
        @Schema(description = "파티 ID", example = "party_uuid")
        String id,
        @Schema(description = "파티 상태", example = "CLOSED")
        PartyStatus status,
        @Schema(description = "종료 사유", example = "FORCE_ENDED", nullable = true)
        PartyEndReason endReason
) {
}
