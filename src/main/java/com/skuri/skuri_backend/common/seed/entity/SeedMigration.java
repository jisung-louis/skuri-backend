package com.skuri.skuri_backend.common.seed.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "seed_migrations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeedMigration {

    @Id
    @Column(name = "migration_key", length = 120)
    private String migrationKey;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    private SeedMigration(String migrationKey, LocalDateTime appliedAt) {
        this.migrationKey = migrationKey;
        this.appliedAt = appliedAt;
    }

    public static SeedMigration apply(String migrationKey) {
        return new SeedMigration(migrationKey, LocalDateTime.now());
    }
}
