package com.skuri.skuri_backend.domain.minecraft.service;

public interface MojangProfileLookupClient {

    MojangProfile lookup(String gameName);

    record MojangProfile(
            String resolvedName,
            String normalizedUuid
    ) {
    }
}
