package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "메시지 페이지네이션 커서")
public record ChatMessageCursorResponse(
        @Schema(description = "커서 생성 시각", example = "2026-03-05T21:10:00")
        LocalDateTime createdAt,
        @Schema(description = "커서 메시지 ID", example = "9f9efc3b-4d55-44e7-a86f-93d5101938ec")
        String id
) {
}
