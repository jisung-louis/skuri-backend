package com.skuri.skuri_backend.infra.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@Setter
@ConfigurationProperties(prefix = "media.storage")
public class MediaStorageProperties {

    private StorageProviderType provider = StorageProviderType.LOCAL;
    private String baseDir = "var/media";
    private String publicBaseUrl;
    private String urlPrefix = "/uploads";
    private String firebaseBucket;

    public Path baseDirPath() {
        return Paths.get(baseDir).toAbsolutePath().normalize();
    }

    public String normalizedUrlPrefix() {
        String normalized = trimTrailingSlash(urlPrefix);
        if (!StringUtils.hasText(normalized)) {
            return "/uploads";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    public String normalizedPublicBaseUrl() {
        return trimTrailingSlash(publicBaseUrl);
    }

    public String normalizedFirebaseBucket() {
        String normalized = trimTrailingSlash(firebaseBucket);
        if (!StringUtils.hasText(normalized)) {
            return normalized;
        }
        return normalized.startsWith("gs://") ? normalized.substring("gs://".length()) : normalized;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
