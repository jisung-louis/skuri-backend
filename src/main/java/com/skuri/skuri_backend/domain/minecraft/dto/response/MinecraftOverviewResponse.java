package com.skuri.skuri_backend.domain.minecraft.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "마인크래프트 서버 개요 응답")
public record MinecraftOverviewResponse(
        @Schema(description = "마인크래프트 공개 채팅방 ID", example = "public:game:minecraft")
        String chatRoomId,
        @Schema(description = "서버 온라인 여부", example = "true", nullable = true)
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
        @Schema(description = "마지막 heartbeat 시각", example = "2026-03-30T13:20:00Z", nullable = true)
        Instant lastHeartbeatAt
) {
}
