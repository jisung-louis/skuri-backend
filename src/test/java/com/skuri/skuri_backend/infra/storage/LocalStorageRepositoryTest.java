package com.skuri.skuri_backend.infra.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalStorageRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveRelativePath_publicBaseUrl기준으로_상대경로를복원한다() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setPublicBaseUrl("https://cdn.skuri.app/uploads");
        properties.setUrlPrefix("/uploads");

        LocalStorageRepository repository = new LocalStorageRepository(
                properties,
                new MockEnvironment().withProperty("server.port", "8080")
        );

        assertEquals(
                "profiles/2026/04/06/photo.jpg",
                repository.resolveRelativePath("https://cdn.skuri.app/uploads/profiles/2026/04/06/photo.jpg").orElseThrow()
        );
        assertTrue(repository.resolveRelativePath("https://images.example.com/photo.jpg").isEmpty());
    }

    @Test
    void resolveRelativePath_publicBaseUrl이없으면_localhostUrlPrefix기준으로_상대경로를복원한다() {
        MediaStorageProperties properties = new MediaStorageProperties();
        properties.setBaseDir(tempDir.toString());
        properties.setUrlPrefix("media-files");

        LocalStorageRepository repository = new LocalStorageRepository(
                properties,
                new MockEnvironment().withProperty("server.port", "9090")
        );

        assertEquals(
                "profiles/2026/04/06/photo.jpg",
                repository.resolveRelativePath("http://localhost:9090/media-files/profiles/2026/04/06/photo.jpg").orElseThrow()
        );
    }
}
