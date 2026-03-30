package com.skuri.skuri_backend.domain.minecraft.dto.request;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftAccountRole;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "마인크래프트 계정 등록 요청")
public record CreateMinecraftAccountRequest(
        @NotNull(message = "edition은 필수입니다.")
        @Schema(description = "마인크래프트 에디션", example = "JAVA")
        MinecraftEdition edition,
        @NotNull(message = "accountRole은 필수입니다.")
        @Schema(description = "계정 역할", example = "SELF")
        MinecraftAccountRole accountRole,
        @NotBlank(message = "gameName은 필수입니다.")
        @Size(max = 50, message = "gameName은 50자를 초과할 수 없습니다.")
        @Schema(description = "게임 내 닉네임", example = "skuriPlayer")
        String gameName
) {
}
