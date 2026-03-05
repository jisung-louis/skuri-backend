package com.skuri.skuri_backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(description = "채팅방 읽음 처리 요청")
public record UpdateChatRoomReadRequest(
        @NotNull
        @Schema(description = "마지막으로 읽은 시각 (단조 증가)", example = "2026-03-05T21:10:00")
        LocalDateTime lastReadAt
) {
}
