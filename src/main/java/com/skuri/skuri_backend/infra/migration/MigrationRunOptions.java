package com.skuri.skuri_backend.infra.migration;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record MigrationRunOptions(
        MigrationMode mode,
        int batchSize,
        boolean failOnReject,
        Path reportRootDirectory,
        LocalDateTime startedAt
) {
}
