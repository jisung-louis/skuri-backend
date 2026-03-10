package com.skuri.skuri_backend.domain.image.storage;

public interface StorageRepository {

    void store(String relativePath, byte[] data, String contentType);

    void delete(String relativePath);
}
