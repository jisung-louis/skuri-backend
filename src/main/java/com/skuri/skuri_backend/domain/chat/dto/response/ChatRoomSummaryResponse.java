package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 목록 카드 응답")
public record ChatRoomSummaryResponse(
        @Schema(description = "채팅방 ID", example = "room-university")
        String id,
        @Schema(description = "채팅방 이름", example = "성결대 전체 채팅방")
        String name,
        @Schema(description = "채팅방 타입", example = "UNIVERSITY")
        ChatRoomType type,
        @Schema(description = "현재 참여 인원 수", example = "150")
        int memberCount,
        @Schema(description = "마지막 메시지 요약", nullable = true)
        ChatRoomLastMessageResponse lastMessage,
        @Schema(description = "내 미읽음 메시지 수", example = "5")
        long unreadCount,
        @Schema(description = "참여 여부", example = "true")
        boolean isJoined
) {
}
