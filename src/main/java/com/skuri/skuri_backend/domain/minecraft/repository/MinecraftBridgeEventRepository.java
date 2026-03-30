package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MinecraftBridgeEventRepository extends JpaRepository<MinecraftBridgeEvent, Long> {

    Optional<MinecraftBridgeEvent> findByEventId(String eventId);

    @Query("""
            select event
            from MinecraftBridgeEvent event
            where event.createdAt > :createdAt
               or (event.createdAt = :createdAt and event.id > :afterId)
            order by event.createdAt asc, event.id asc
            """)
    List<MinecraftBridgeEvent> findReplayEventsAfter(
            @Param("createdAt") LocalDateTime createdAt,
            @Param("afterId") Long afterId
    );

    void deleteByExpiresAtBefore(Instant cutoff);
}
