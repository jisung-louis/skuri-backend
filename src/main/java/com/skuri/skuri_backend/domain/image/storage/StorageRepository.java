package com.skuri.skuri_backend.domain.image.storage;

import java.util.Optional;

public interface StorageRepository {

    StoredObject store(String relativePath, byte[] data, String contentType);

    void delete(String relativePath);

    Optional<String> resolveRelativePath(String publicUrl);

    record StoredObject(String relativePath, String publicUrl) {
    }
}
