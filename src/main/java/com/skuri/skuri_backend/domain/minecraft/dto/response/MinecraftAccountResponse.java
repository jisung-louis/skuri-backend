package com.skuri.skuri_backend.domain.minecraft.dto.response;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "내 마인크래프트 계정 응답")
public record MinecraftAccountResponse(
        @Schema(description = "계정 ID", example = "account-uuid")
        String id,
        @Schema(description = "계정 역할", example = "SELF")
        MinecraftAccountRole accountRole,
        @Schema(description = "에디션", example = "JAVA")
        MinecraftEdition edition,
        @Schema(description = "게임 내 닉네임", example = "skuriPlayer")
        String gameName,
        @Schema(description = "정규화된 식별 키", example = "8667ba71b85a4004af54457a9734eed7")
        String normalizedKey,
        @Schema(description = "아바타 UUID 키", example = "8667ba71b85a4004af54457a9734eed7")
        String avatarUuid,
        @Schema(description = "Bedrock 저장용 이름", example = "Skuri_Be", nullable = true)
        String storedName,
        @Schema(description = "부모 계정 ID", example = "parent-account-uuid", nullable = true)
        String parentAccountId,
        @Schema(description = "친구 계정인 경우 부모 계정 닉네임", example = "skuriPlayer", nullable = true)
        String parentGameName,
        @Schema(description = "최근 접속 시각", example = "2026-03-30T13:18:00Z", nullable = true)
        Instant lastSeenAt,
        @Schema(description = "등록 시각", example = "2026-03-30T13:00:00Z", nullable = true)
        Instant linkedAt
) {
}
