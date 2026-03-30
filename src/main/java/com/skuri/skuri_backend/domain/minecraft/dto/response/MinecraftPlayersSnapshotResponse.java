package com.skuri.skuri_backend.domain.minecraft.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "플레이어 목록 SSE snapshot payload")
public record MinecraftPlayersSnapshotResponse(
        @Schema(description = "플레이어 목록")
        List<MinecraftPlayerResponse> players
) {
}
