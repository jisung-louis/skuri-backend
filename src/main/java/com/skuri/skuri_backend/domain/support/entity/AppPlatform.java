package com.skuri.skuri_backend.domain.support.entity;

import java.util.Arrays;

public enum AppPlatform {
    IOS("ios"),
    ANDROID("android");

    private final String value;

    AppPlatform(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AppPlatform from(String value) {
        return Arrays.stream(values())
                .filter(platform -> platform.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 platform입니다."));
    }
}
