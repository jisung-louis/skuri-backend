package com.skuri.skuri_backend.domain.minecraft.dto.response;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 -> 마인크래프트 채팅 브리지 payload")
public record MinecraftChatBridgeMessageResponse(
        @Schema(description = "채팅 메시지 ID", example = "dfd5b4b1-54ea-4fa1-92d9-b61a931d0d56")
        String messageId,
        @Schema(description = "채팅방 ID", example = "public:game:minecraft")
        String chatRoomId,
        @Schema(description = "발신자 이름", example = "홍길동")
        String senderName,
        @Schema(description = "메시지 타입", example = "TEXT")
        ChatMessageType type,
        @Schema(description = "플러그인에 전달할 텍스트", example = "안녕하세요!")
        String text
) {
}
