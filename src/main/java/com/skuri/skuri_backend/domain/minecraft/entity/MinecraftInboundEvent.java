package com.skuri.skuri_backend.domain.minecraft.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "minecraft_inbound_events",
        indexes = {
                @Index(name = "idx_minecraft_inbound_events_chat_message", columnList = "chat_message_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftInboundEvent extends BaseTimeEntity {

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "chat_message_id", length = 36)
    private String chatMessageId;

    private MinecraftInboundEvent(String eventId) {
        this.eventId = eventId;
    }

    public static MinecraftInboundEvent create(String eventId) {
        return new MinecraftInboundEvent(eventId);
    }

    public void markProcessed(String chatMessageId) {
        this.chatMessageId = chatMessageId;
    }
}
