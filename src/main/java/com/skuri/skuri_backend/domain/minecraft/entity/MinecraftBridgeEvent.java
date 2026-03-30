package com.skuri.skuri_backend.domain.minecraft.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "minecraft_bridge_events",
        indexes = {
                @Index(name = "uk_minecraft_bridge_events_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_minecraft_bridge_events_replay", columnList = "created_at, id"),
                @Index(name = "idx_minecraft_bridge_events_expires", columnList = "expires_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftBridgeEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private MinecraftBridgeEventType eventType;

    @Lob
    @Column(nullable = false)
    private String payload;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    private MinecraftBridgeEvent(MinecraftBridgeEventType eventType, String payload, Instant expiresAt) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.payload = payload;
        this.expiresAt = expiresAt;
    }

    public static MinecraftBridgeEvent create(
            MinecraftBridgeEventType eventType,
            String payload,
            Instant expiresAt
    ) {
        return new MinecraftBridgeEvent(eventType, payload, expiresAt);
    }
}
