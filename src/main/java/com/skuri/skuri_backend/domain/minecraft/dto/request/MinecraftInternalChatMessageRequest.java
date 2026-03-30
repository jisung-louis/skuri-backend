package com.skuri.skuri_backend.domain.minecraft.dto.request;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftInboundEventType;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftSystemType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "플러그인 -> 백엔드 마인크래프트 채팅/시스템 메시지 전달 요청")
public record MinecraftInternalChatMessageRequest(
        @NotBlank(message = "eventId는 필수입니다.")
        @Schema(description = "플러그인 이벤트 ID", example = "9fa37c63-2c5a-4d1d-8a28-55b72750e79d")
        String eventId,
        @NotNull(message = "eventType은 필수입니다.")
        @Schema(description = "이벤트 타입", example = "CHAT")
        MinecraftInboundEventType eventType,
        @Schema(description = "시스템 메시지 타입", example = "JOIN", nullable = true)
        MinecraftSystemType systemType,
        @NotBlank(message = "senderName은 필수입니다.")
        @Schema(description = "게임 내 발신자 이름", example = "skuriPlayer")
        String senderName,
        @Schema(
                description = "Minecraft 고유 식별자. Java는 32자리 UUID, Bedrock은 be:<storedName> 형식",
                example = "8667ba71b85a4004af54457a9734eed7",
                nullable = true
        )
        String minecraftUuid,
        @NotNull(message = "edition은 필수입니다.")
        @Schema(description = "플레이어 에디션", example = "JAVA")
        MinecraftEdition edition,
        @NotBlank(message = "text는 필수입니다.")
        @Schema(description = "메시지 본문", example = "안녕하세요!")
        String text,
        @NotNull(message = "occurredAt은 필수입니다.")
        @Schema(description = "게임 서버에서 이벤트가 발생한 시각", example = "2026-03-30T13:20:00Z")
        Instant occurredAt
) {
}
