package com.skuri.skuri_backend.domain.minecraft.repository;

import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftServerState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinecraftServerStateRepository extends JpaRepository<MinecraftServerState, String> {
}
