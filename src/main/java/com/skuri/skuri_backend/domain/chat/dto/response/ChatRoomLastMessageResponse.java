package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅방 마지막 메시지 요약")
public record ChatRoomLastMessageResponse(
        @Schema(description = "메시지 타입", example = "TEXT")
        String type,
        @Schema(description = "메시지 본문", example = "안녕하세요!", nullable = true)
        String text,
        @Schema(description = "발신자 이름", example = "홍길동", nullable = true)
        String senderName,
        @Schema(description = "메시지 시각", example = "2026-03-05T21:10:00", nullable = true)
        LocalDateTime createdAt
) {
}
