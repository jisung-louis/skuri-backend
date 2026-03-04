package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 생성 응답")
public record PartyCreateResponse(
        @Schema(description = "파티 ID", example = "party_uuid")
        String id,
        @Schema(description = "파티 채팅방 ID", example = "party:party_uuid")
        String chatRoomId
) {
}
