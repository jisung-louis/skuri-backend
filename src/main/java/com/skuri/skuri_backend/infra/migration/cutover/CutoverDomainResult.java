package com.skuri.skuri_backend.infra.migration.cutover;

import com.skuri.skuri_backend.infra.migration.MigrationReject;

import java.util.List;
import java.util.Map;

record CutoverDomainResult(
        String domain,
        Map<String, Long> counters,
        List<MigrationReject> rejects,
        Map<String, ?> artifacts
) {
}
