package com.skuri.skuri_backend.infra.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MediaStorageProperties.class)
public class MediaStorageWebMvcConfig implements WebMvcConfigurer {

    private final MediaStorageProperties mediaStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(mediaStorageProperties.normalizedUrlPrefix() + "/**")
                .addResourceLocations(mediaStorageProperties.baseDirPath().toUri().toString());
    }
}
