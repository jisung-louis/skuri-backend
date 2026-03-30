package com.skuri.skuri_backend.domain.minecraft.dto.response;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "마인크래프트 화이트리스트/플레이어 응답")
public record MinecraftPlayerResponse(
        @Schema(description = "계정 ID", example = "account-uuid")
        String accountId,
        @Schema(description = "소유 회원 ID", example = "member-1")
        String ownerMemberId,
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
        @Schema(description = "친구 계정인 경우 부모 계정 닉네임", example = "skuriPlayer", nullable = true)
        String parentGameName,
        @Schema(description = "현재 온라인 여부", example = "true")
        boolean online,
        @Schema(description = "최근 접속 시각", example = "2026-03-30T13:18:00Z", nullable = true)
        Instant lastSeenAt
) {
}
