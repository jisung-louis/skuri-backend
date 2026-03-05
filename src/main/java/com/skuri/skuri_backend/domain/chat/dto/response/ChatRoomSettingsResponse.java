package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 설정 수정 응답")
public record ChatRoomSettingsResponse(
        @Schema(description = "채팅방 ID", example = "room-university")
        String chatRoomId,
        @Schema(description = "음소거 여부", example = "true")
        boolean muted
) {
}
