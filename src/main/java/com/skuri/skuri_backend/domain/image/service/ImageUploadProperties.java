package com.skuri.skuri_backend.domain.image.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "media.image")
public class ImageUploadProperties {

    private int maxFileSizeBytes = 10 * 1024 * 1024;
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp");
    private int thumbnailWidth = 300;
    private double thumbnailJpegQuality = 0.8d;
}
