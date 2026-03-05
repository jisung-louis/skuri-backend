package com.skuri.skuri_backend.domain.chat.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 공개 채팅방 생성 응답")
public record AdminCreateChatRoomResponse(
        @Schema(description = "채팅방 ID", example = "room:2e8f745a-c131-4e1d-9b8e-7e8d4bb686b3")
        String id,
        @Schema(description = "채팅방 이름", example = "성결대 전체 채팅방")
        String name,
        @Schema(description = "채팅방 타입", example = "UNIVERSITY")
        ChatRoomType type
) {
}

