package com.skuri.skuri_backend.domain.minecraft.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(description = "플러그인 -> 백엔드 서버 상태 upsert 요청")
public record MinecraftServerStateUpsertRequest(
        @NotNull(message = "online은 필수입니다.")
        @Schema(description = "서버 온라인 여부", example = "true")
        Boolean online,
        @Schema(description = "현재 접속 인원", example = "12", nullable = true)
        Integer currentPlayers,
        @Schema(description = "최대 접속 인원", example = "50", nullable = true)
        Integer maxPlayers,
        @Schema(description = "서버 버전", example = "1.21.1", nullable = true)
        String version,
        @Schema(description = "접속 주소", example = "mc.skuri.app", nullable = true)
        String serverAddress,
        @Schema(description = "지도 URL", example = "https://map.skuri.app", nullable = true)
        String mapUrl,
        @NotNull(message = "heartbeatAt은 필수입니다.")
        @Schema(description = "플러그인 heartbeat 시각", example = "2026-03-30T13:20:00Z")
        Instant heartbeatAt
) {
}
