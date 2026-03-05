package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅방 상세 응답")
public record ChatRoomDetailResponse(
        @Schema(description = "채팅방 ID", example = "room-university")
        String id,
        @Schema(description = "채팅방 이름", example = "성결대 전체 채팅방")
        String name,
        @Schema(description = "채팅방 타입", example = "UNIVERSITY")
        ChatRoomType type,
        @Schema(description = "채팅방 설명", example = "성결대학교 학생들의 소통 공간", nullable = true)
        String description,
        @Schema(description = "공개 여부", example = "true")
        boolean isPublic,
        @Schema(description = "현재 참여 인원 수", example = "150")
        int memberCount,
        @Schema(description = "참여 여부", example = "true")
        boolean isJoined,
        @Schema(description = "음소거 여부", example = "false")
        boolean isMuted,
        @Schema(description = "내 lastReadAt", example = "2026-03-05T21:00:00", nullable = true)
        LocalDateTime lastReadAt,
        @Schema(description = "내 미읽음 메시지 수", example = "5")
        long unreadCount
) {
}
