package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "읽음 처리 응답")
public record ChatReadUpdateResponse(
        @Schema(description = "채팅방 ID", example = "room-university")
        String chatRoomId,
        @Schema(description = "적용된 lastReadAt(ISO 8601 UTC, 단조 증가 보장)", example = "2026-03-05T12:10:00Z")
        Instant lastReadAt,
        @Schema(description = "요청값으로 실제 갱신되었는지 여부", example = "true")
        boolean updated
) {
}
