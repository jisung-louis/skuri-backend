package com.skuri.skuri_backend.infra.migration;

import java.time.LocalDateTime;
import java.util.Map;

public record MigrationSummary(
        String plan,
        String mode,
        String inputFile,
        String inputFileSha256,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Map<String, Long> counters
) {
}
