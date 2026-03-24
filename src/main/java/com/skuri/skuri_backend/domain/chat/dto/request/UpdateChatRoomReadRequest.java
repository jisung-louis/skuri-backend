package com.skuri.skuri_backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "채팅방 읽음 처리 요청")
public record UpdateChatRoomReadRequest(
        @NotNull
        @Schema(description = "마지막으로 읽은 시각 (ISO 8601 UTC, 단조 증가)", example = "2026-03-05T12:10:00Z")
        Instant lastReadAt
) {
}
