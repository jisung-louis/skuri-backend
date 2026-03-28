package com.skuri.skuri_backend.domain.support.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum LegalDocumentKey {
    TERMS_OF_USE("termsOfUse"),
    PRIVACY_POLICY("privacyPolicy");

    private final String value;

    LegalDocumentKey(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }

    @JsonCreator
    public static LegalDocumentKey from(String value) {
        return Arrays.stream(values())
                .filter(key -> key.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 documentKey입니다."));
    }
}
