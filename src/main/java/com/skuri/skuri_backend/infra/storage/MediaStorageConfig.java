package com.skuri.skuri_backend.infra.storage;

import com.google.cloud.storage.Bucket;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.StorageClient;
import com.skuri.skuri_backend.domain.image.service.ImageUploadProperties;
import com.skuri.skuri_backend.domain.image.storage.StorageRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

@Configuration
@EnableConfigurationProperties(ImageUploadProperties.class)
public class MediaStorageConfig {

    @Bean
    public StorageRepository storageRepository(
            MediaStorageProperties mediaStorageProperties,
            Environment environment,
            ObjectProvider<FirebaseApp> firebaseAppProvider
    ) {
        return switch (mediaStorageProperties.getProvider()) {
            case LOCAL -> createLocalStorageRepository(mediaStorageProperties, environment);
            case FIREBASE -> createFirebaseStorageRepository(mediaStorageProperties, firebaseAppProvider);
        };
    }

    private StorageRepository createLocalStorageRepository(
            MediaStorageProperties mediaStorageProperties,
            Environment environment
    ) {
        try {
            Files.createDirectories(mediaStorageProperties.baseDirPath());
        } catch (IOException e) {
            throw new UncheckedIOException("media storage base directory를 생성하지 못했습니다.", e);
        }

        return new LocalStorageRepository(mediaStorageProperties, environment);
    }

    private StorageRepository createFirebaseStorageRepository(
            MediaStorageProperties mediaStorageProperties,
            ObjectProvider<FirebaseApp> firebaseAppProvider
    ) {
        String bucketName = mediaStorageProperties.normalizedFirebaseBucket();
        if (!StringUtils.hasText(bucketName)) {
            throw new IllegalStateException("FIREBASE provider는 media.storage.firebase-bucket 설정이 필요합니다.");
        }

        FirebaseApp firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null) {
            throw new IllegalStateException("FIREBASE provider는 FirebaseApp 초기화가 필요합니다.");
        }

        Bucket bucket = StorageClient.getInstance(firebaseApp).bucket(bucketName);
        return new FirebaseStorageRepository(bucket);
    }
}
