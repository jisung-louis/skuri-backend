package com.skuri.skuri_backend.domain.support.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum LegalDocumentBannerIconKey {
    DOCUMENT("document"),
    SHIELD("shield");

    private final String value;

    LegalDocumentBannerIconKey(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static LegalDocumentBannerIconKey from(String value) {
        return Arrays.stream(values())
                .filter(key -> key.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 banner.iconKey입니다."));
    }
}
