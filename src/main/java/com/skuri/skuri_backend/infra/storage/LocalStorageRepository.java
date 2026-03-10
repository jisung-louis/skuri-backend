package com.skuri.skuri_backend.infra.storage;

import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class LocalStorageRepository implements StorageRepository {

    private final Path baseDirectory;
    private final MediaStorageProperties mediaStorageProperties;
    private final Environment environment;

    public LocalStorageRepository(MediaStorageProperties mediaStorageProperties, Environment environment) {
        this.baseDirectory = mediaStorageProperties.baseDirPath();
        this.mediaStorageProperties = mediaStorageProperties;
        this.environment = environment;
    }

    @Override
    public StoredObject store(String relativePath, byte[] data, String contentType) {
        try {
            Path target = resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.write(
                    target,
                    data,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return new StoredObject(relativePath, buildPublicUrl(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제에 실패했습니다.", e);
        }
    }

    private Path resolve(String relativePath) {
        Path target = baseDirectory.resolve(relativePath).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("storage path는 baseDir 밖으로 벗어날 수 없습니다.");
        }
        return target;
    }

    private String buildPublicUrl(String relativePath) {
        String baseUrl = mediaStorageProperties.normalizedPublicBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            String serverPort = environment.getProperty("server.port", "8080");
            baseUrl = "http://localhost:" + serverPort + mediaStorageProperties.normalizedUrlPrefix();
        }
        return baseUrl + "/" + relativePath.replace('\\', '/');
    }
}
