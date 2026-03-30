package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftInboundEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MinecraftInboundEventRepository extends JpaRepository<MinecraftInboundEvent, String> {

    @Modifying
    @Query(
            value = """
                    insert ignore into minecraft_inbound_events (event_id, created_at, updated_at)
                    values (:eventId, current_timestamp(6), current_timestamp(6))
                    """,
            nativeQuery = true
    )
    int claimEvent(@Param("eventId") String eventId);

    @Modifying
    @Query(
            value = """
                    update minecraft_inbound_events
                       set chat_message_id = :chatMessageId,
                           updated_at = current_timestamp(6)
                     where event_id = :eventId
                    """,
            nativeQuery = true
    )
    int markProcessed(@Param("eventId") String eventId, @Param("chatMessageId") String chatMessageId);
}
