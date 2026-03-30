package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MinecraftAvatarService {

    private final MinecraftBridgeProperties bridgeProperties;

    public MinecraftAvatarService(MinecraftBridgeProperties bridgeProperties) {
        this.bridgeProperties = bridgeProperties;
    }

    public String resolveAvatarUrl(String minecraftUuid) {
        String avatarKey = StringUtils.hasText(minecraftUuid) && !minecraftUuid.startsWith("be:")
                ? minecraftUuid
                : bridgeProperties.normalizedDefaultAvatarUuid();
        return "https://minotar.net/avatar/" + avatarKey + "/48";
    }
}
