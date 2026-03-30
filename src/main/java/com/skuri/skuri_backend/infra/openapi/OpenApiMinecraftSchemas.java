package com.skuri.skuri_backend.infra.openapi;

import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftAccountResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftOverviewResponse;
import com.skuri.skuri_backend.domain.minecraft.dto.response.MinecraftPlayerResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public final class OpenApiMinecraftSchemas {

    private OpenApiMinecraftSchemas() {
    }

    @Schema(name = "MinecraftOverviewApiResponse", description = "공통 API 응답 포맷")
    public record MinecraftOverviewApiResponse(
            boolean success,
            MinecraftOverviewResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "MinecraftPlayerListApiResponse", description = "공통 API 응답 포맷")
    public record MinecraftPlayerListApiResponse(
            boolean success,
            List<MinecraftPlayerResponse> data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "MinecraftAccountListApiResponse", description = "공통 API 응답 포맷")
    public record MinecraftAccountListApiResponse(
            boolean success,
            List<MinecraftAccountResponse> data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }

    @Schema(name = "MinecraftAccountApiResponse", description = "공통 API 응답 포맷")
    public record MinecraftAccountApiResponse(
            boolean success,
            MinecraftAccountResponse data,
            String message,
            String errorCode,
            LocalDateTime timestamp
    ) {
    }
}
