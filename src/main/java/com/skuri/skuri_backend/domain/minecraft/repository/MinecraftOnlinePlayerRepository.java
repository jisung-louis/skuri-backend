package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftOnlinePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MinecraftOnlinePlayerRepository extends JpaRepository<MinecraftOnlinePlayer, String> {

    List<MinecraftOnlinePlayer> findByServerKeyOrderByPlayerNameAsc(String serverKey);

    void deleteByServerKey(String serverKey);
}
