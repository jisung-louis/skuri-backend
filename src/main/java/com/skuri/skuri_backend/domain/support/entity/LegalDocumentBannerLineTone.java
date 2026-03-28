package com.skuri.skuri_backend.domain.support.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum LegalDocumentBannerLineTone {
    PRIMARY("primary"),
    SECONDARY("secondary");

    private final String value;

    LegalDocumentBannerLineTone(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static LegalDocumentBannerLineTone from(String value) {
        return Arrays.stream(values())
                .filter(tone -> tone.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 banner.lines.tone입니다."));
    }
}
