package com.skuri.skuri_backend.infra.storage;

import com.skuri.skuri_backend.domain.image.service.ImageUploadProperties;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@Configuration
@EnableConfigurationProperties({MediaStorageProperties.class, ImageUploadProperties.class})
public class MediaStorageConfig {

    @Bean
    public StorageRepository storageRepository(MediaStorageProperties mediaStorageProperties) {
        if (mediaStorageProperties.getProvider() != StorageProviderType.LOCAL) {
            throw new IllegalStateException("지원하지 않는 storage provider입니다: " + mediaStorageProperties.getProvider());
        }

        try {
            Files.createDirectories(mediaStorageProperties.baseDirPath());
        } catch (IOException e) {
            throw new UncheckedIOException("media storage base directory를 생성하지 못했습니다.", e);
        }

        return new LocalStorageRepository(mediaStorageProperties);
    }
}
