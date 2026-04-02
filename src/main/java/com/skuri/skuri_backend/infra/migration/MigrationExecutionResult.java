package com.skuri.skuri_backend.infra.migration;

import java.nio.file.Path;
import java.util.List;

public record MigrationExecutionResult(
        MigrationSummary summary,
        Path reportDirectory,
        List<MigrationReject> rejects
) {
}
