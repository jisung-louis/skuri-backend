package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채팅 메시지 페이지 응답")
public record ChatMessagePageResponse(
        @Schema(description = "메시지 목록")
        List<ChatMessageResponse> messages,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,
        @Schema(description = "다음 요청 커서 (정렬: createdAt DESC, id DESC)", nullable = true)
        ChatMessageCursorResponse nextCursor
) {
}
