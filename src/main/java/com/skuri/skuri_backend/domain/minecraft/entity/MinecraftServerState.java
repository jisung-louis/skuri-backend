package com.skuri.skuri_backend.domain.minecraft.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "minecraft_server_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftServerState extends BaseTimeEntity {

    @Id
    @Column(name = "server_key", length = 50)
    private String serverKey;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "current_players")
    private Integer currentPlayers;

    @Column(name = "max_players")
    private Integer maxPlayers;

    @Column(length = 50)
    private String version;

    @Column(name = "server_address", length = 255)
    private String serverAddress;

    @Column(name = "map_url", length = 500)
    private String mapUrl;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    private MinecraftServerState(String serverKey) {
        this.serverKey = serverKey;
        this.online = false;
    }

    public static MinecraftServerState create(String serverKey) {
        return new MinecraftServerState(serverKey);
    }

    public void update(
            boolean online,
            Integer currentPlayers,
            Integer maxPlayers,
            String version,
            String serverAddress,
            String mapUrl,
            Instant lastHeartbeatAt
    ) {
        this.online = online;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.version = version;
        this.serverAddress = serverAddress;
        this.mapUrl = mapUrl;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }
}
