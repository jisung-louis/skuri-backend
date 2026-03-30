package com.skuri.skuri_backend.domain.minecraft.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "minecraft_online_players",
        indexes = {
                @Index(name = "idx_minecraft_online_players_server", columnList = "server_key"),
                @Index(name = "idx_minecraft_online_players_normalized_key", columnList = "normalized_key"),
                @Index(name = "idx_minecraft_online_players_verified", columnList = "verified")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftOnlinePlayer extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "server_key", nullable = false, length = 50)
    private String serverKey;

    @Column(name = "normalized_key", nullable = false, length = 64)
    private String normalizedKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MinecraftEdition edition;

    @Column(name = "player_name", nullable = false, length = 50)
    private String playerName;

    @Column(name = "avatar_uuid", nullable = false, length = 64)
    private String avatarUuid;

    @Column(name = "minecraft_account_id", length = 36)
    private String minecraftAccountId;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "joined_at")
    private Instant joinedAt;

    private MinecraftOnlinePlayer(
            String serverKey,
            String normalizedKey,
            MinecraftEdition edition,
            String playerName,
            String avatarUuid,
            String minecraftAccountId,
            boolean verified,
            Instant joinedAt
    ) {
        this.id = UUID.randomUUID().toString();
        this.serverKey = serverKey;
        this.normalizedKey = normalizedKey;
        this.edition = edition;
        this.playerName = playerName;
        this.avatarUuid = avatarUuid;
        this.minecraftAccountId = minecraftAccountId;
        this.verified = verified;
        this.joinedAt = joinedAt;
    }

    public static MinecraftOnlinePlayer create(
            String serverKey,
            String normalizedKey,
            MinecraftEdition edition,
            String playerName,
            String avatarUuid,
            String minecraftAccountId,
            boolean verified,
            Instant joinedAt
    ) {
        return new MinecraftOnlinePlayer(
                serverKey,
                normalizedKey,
                edition,
                playerName,
                avatarUuid,
                minecraftAccountId,
                verified,
                joinedAt
        );
    }
}
