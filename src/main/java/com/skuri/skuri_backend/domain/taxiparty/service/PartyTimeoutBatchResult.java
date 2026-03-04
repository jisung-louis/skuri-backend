package com.skuri.skuri_backend.domain.taxiparty.service;

public record PartyTimeoutBatchResult(
        int targetCount,
        int endedCount,
        int conflictedCount,
        int skippedCount
) {
}
