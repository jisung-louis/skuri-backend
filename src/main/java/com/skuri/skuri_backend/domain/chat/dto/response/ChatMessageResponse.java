package com.skuri.skuri_backend.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.skuri.skuri_backend.domain.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessageResponse(
        @Schema(description = "메시지 ID", example = "9f9efc3b-4d55-44e7-a86f-93d5101938ec")
        String id,
        @Schema(description = "채팅방 ID", example = "party:party-1")
        String chatRoomId,
        @Schema(description = "발신자 ID", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String senderId,
        @Schema(description = "발신자 닉네임", example = "스쿠리 유저")
        String senderName,
        @Schema(description = "메시지 타입", example = "TEXT")
        ChatMessageType type,
        @Schema(description = "텍스트 본문(TEXT/IMAGE/END 등)", example = "안녕하세요!", nullable = true)
        String text,
        @Schema(description = "이미지 URL(IMAGE 타입)", example = "https://cdn.skuri.app/chat/2026/03/05/image-1.jpg", nullable = true)
        String imageUrl,
        @Schema(description = "계좌 정보(ACCOUNT 타입)", nullable = true)
        ChatAccountDataResponse accountData,
        @Schema(description = "도착 정보(ARRIVED 타입)", nullable = true)
        ChatArrivalDataResponse arrivalData,
        @Schema(description = "메시지 생성 시각", example = "2026-03-05T21:10:00")
        LocalDateTime createdAt
) {
}
