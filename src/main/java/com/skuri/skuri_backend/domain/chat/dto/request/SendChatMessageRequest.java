package com.skuri.skuri_backend.domain.chat.dto.request;

import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "채팅 메시지 전송 요청")
public record SendChatMessageRequest(
        @NotNull
        @Schema(description = "메시지 타입", example = "TEXT", allowableValues = {"TEXT", "IMAGE", "ACCOUNT", "ARRIVED", "END"})
        ChatMessageType type,
        @Schema(description = "텍스트 메시지 본문", example = "안녕하세요!", nullable = true)
        String text,
        @Schema(description = "이미지 URL(IMAGE 타입에서 사용)", example = "https://cdn.skuri.app/chat/2026/03/05/image-1.jpg", nullable = true)
        String imageUrl,
        @Schema(description = "도착 메시지 택시비(레거시 필드, 현재 서버에서는 무시됨)", example = "14000", nullable = true)
        Integer taxiFare
) {
}
