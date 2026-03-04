package com.skuri.skuri_backend.domain.member.entity;

import org.springframework.util.StringUtils;

public enum LinkedAccountProvider {
    GOOGLE("google.com", true),
    PASSWORD("password", false),
    UNKNOWN("", false);

    private final String signInProvider;
    private final boolean socialProvider;

    LinkedAccountProvider(String signInProvider, boolean socialProvider) {
        this.signInProvider = signInProvider;
        this.socialProvider = socialProvider;
    }

    public boolean isSocialProvider() {
        return socialProvider;
    }

    public static LinkedAccountProvider fromSignInProvider(String signInProvider) {
        if (!StringUtils.hasText(signInProvider)) {
            return UNKNOWN;
        }
        for (LinkedAccountProvider provider : values()) {
            if (provider.signInProvider.equals(signInProvider)) {
                return provider;
            }
        }
        return UNKNOWN;
    }
}
