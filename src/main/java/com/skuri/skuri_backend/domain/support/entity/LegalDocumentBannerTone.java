package com.skuri.skuri_backend.domain.support.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum LegalDocumentBannerTone {
    GREEN("green"),
    BLUE("blue");

    private final String value;

    LegalDocumentBannerTone(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static LegalDocumentBannerTone from(String value) {
        return Arrays.stream(values())
                .filter(tone -> tone.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 banner.tone입니다."));
    }
}
