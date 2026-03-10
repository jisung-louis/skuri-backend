package com.skuri.skuri_backend.domain.image.storage;

public interface StorageRepository {

    StoredObject store(String relativePath, byte[] data, String contentType);

    void delete(String relativePath);

    record StoredObject(String relativePath, String publicUrl) {
    }
}
