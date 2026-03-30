package com.skuri.skuri_backend.domain.minecraft.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "플레이어 제거 이벤트 payload")
public record MinecraftPlayerRemoveResponse(
        @Schema(description = "정규화된 식별 키", example = "8667ba71b85a4004af54457a9734eed7")
        String normalizedKey
) {
}
