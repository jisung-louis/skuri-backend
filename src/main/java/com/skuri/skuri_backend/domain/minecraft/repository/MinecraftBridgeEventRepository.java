package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftBridgeEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MinecraftBridgeEventRepository extends JpaRepository<MinecraftBridgeEvent, String> {

    Optional<MinecraftBridgeEvent> findByEventId(String eventId);

    List<MinecraftBridgeEvent> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);

    void deleteByExpiresAtBefore(Instant cutoff);
}
