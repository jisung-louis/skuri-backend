package com.skuri.skuri_backend.infra.migration;

import java.util.Map;

public record MigrationReject(
        String itemId,
        String reason,
        Map<String, Object> detail
) {
}
