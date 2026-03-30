package com.skuri.skuri_backend.domain.minecraft.dto.request;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

@Schema(description = "플러그인 -> 백엔드 현재 온라인 플레이어 스냅샷 요청")
public record MinecraftOnlinePlayersUpsertRequest(
        @NotNull(message = "capturedAt은 필수입니다.")
        @Schema(description = "스냅샷 캡처 시각", example = "2026-03-30T13:20:00Z")
        Instant capturedAt,
        @Valid
        @NotNull(message = "players는 필수입니다.")
        @Schema(description = "현재 온라인 플레이어 목록")
        List<Player> players
) {

    @Schema(description = "온라인 플레이어 항목")
    public record Player(
            @NotBlank(message = "gameName은 필수입니다.")
            @Schema(description = "게임 내 닉네임", example = "skuriPlayer")
            String gameName,
            @NotNull(message = "edition은 필수입니다.")
            @Schema(description = "플레이어 에디션", example = "JAVA")
            MinecraftEdition edition,
            @Schema(
                    description = "Java는 32자리 UUID, Bedrock은 be:<storedName> 형식",
                    example = "8667ba71b85a4004af54457a9734eed7",
                    nullable = true
            )
            String minecraftUuid
    ) {
    }
}
