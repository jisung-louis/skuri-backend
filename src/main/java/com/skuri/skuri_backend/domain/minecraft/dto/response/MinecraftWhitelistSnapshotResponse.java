package com.skuri.skuri_backend.domain.minecraft.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "플러그인 화이트리스트 snapshot payload")
public record MinecraftWhitelistSnapshotResponse(
        @Schema(description = "화이트리스트 플레이어 목록")
        List<MinecraftWhitelistEntryResponse> players
) {
}
