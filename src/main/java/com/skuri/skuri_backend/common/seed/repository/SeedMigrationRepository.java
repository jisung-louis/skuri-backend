package com.skuri.skuri_backend.common.seed.repository;

import com.skuri.skuri_backend.common.seed.entity.SeedMigration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedMigrationRepository extends JpaRepository<SeedMigration, String> {
}
