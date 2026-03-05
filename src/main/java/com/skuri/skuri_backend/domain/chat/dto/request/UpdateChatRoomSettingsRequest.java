package com.skuri.skuri_backend.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "채팅방 설정 수정 요청")
public record UpdateChatRoomSettingsRequest(
        @NotNull
        @Schema(description = "음소거 여부", example = "true")
        Boolean muted
) {
}
