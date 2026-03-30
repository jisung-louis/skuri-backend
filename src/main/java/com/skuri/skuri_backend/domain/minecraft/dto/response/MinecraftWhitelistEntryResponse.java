package com.skuri.skuri_backend.domain.minecraft.dto.response;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "플러그인 화이트리스트 동기화 항목")
public record MinecraftWhitelistEntryResponse(
        @Schema(description = "계정 ID", example = "account-uuid")
        String accountId,
        @Schema(description = "정규화된 식별 키", example = "8667ba71b85a4004af54457a9734eed7")
        String normalizedKey,
        @Schema(description = "에디션", example = "JAVA")
        MinecraftEdition edition,
        @Schema(description = "게임 내 닉네임", example = "skuriPlayer")
        String gameName,
        @Schema(description = "아바타 UUID 키", example = "8667ba71b85a4004af54457a9734eed7")
        String avatarUuid,
        @Schema(description = "Bedrock 저장용 이름", example = "Skuri_Be", nullable = true)
        String storedName
) {
}
