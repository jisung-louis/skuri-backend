package com.skuri.skuri_backend.domain.minecraft.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "minecraft.bridge")
public record MinecraftBridgeProperties(
        String sharedSecret,
        String roomId,
        String serverKey,
        String defaultAvatarUuid,
        long replayTtlSeconds
) {

    public String normalizedRoomId() {
        return roomId == null || roomId.isBlank() ? "public:game:minecraft" : roomId;
    }

    public String normalizedServerKey() {
        return serverKey == null || serverKey.isBlank() ? "skuri" : serverKey;
    }

    public String normalizedDefaultAvatarUuid() {
        return defaultAvatarUuid == null || defaultAvatarUuid.isBlank()
                ? "8667ba71b85a4004af54457a9734eed7"
                : defaultAvatarUuid;
    }

    public long normalizedReplayTtlSeconds() {
        return replayTtlSeconds <= 0 ? 86_400L : replayTtlSeconds;
    }
}
