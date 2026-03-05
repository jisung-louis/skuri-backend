package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅방 목록 요약 이벤트")
public record ChatRoomSummaryEventResponse(
        @Schema(description = "이벤트 타입", example = "CHAT_ROOM_UPSERT")
        String eventType,
        @Schema(description = "채팅방 ID", example = "room-university")
        String chatRoomId,
        @Schema(description = "채팅방 이름", example = "성결대 전체 채팅방")
        String name,
        @Schema(description = "멤버 수", example = "150")
        int memberCount,
        @Schema(description = "미읽음 수", example = "3")
        long unreadCount,
        @Schema(description = "마지막 메시지")
        ChatRoomLastMessageResponse lastMessage,
        @Schema(description = "업데이트 시각", example = "2026-03-05T21:10:00")
        LocalDateTime updatedAt
) {
}
