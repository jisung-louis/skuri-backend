package com.skuri.skuri_backend.common.util;

import org.springframework.util.StringUtils;

public final class AccountHolderMasker {

    private AccountHolderMasker() {
    }

    public static String mask(String accountHolder, Boolean hideName) {
        if (!Boolean.TRUE.equals(hideName) || !StringUtils.hasText(accountHolder)) {
            return accountHolder;
        }

        String trimmed = accountHolder.trim();
        int length = trimmed.length();
        if (length == 1) {
            return "*";
        }
        if (length == 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "*".repeat(length - 2) + trimmed.charAt(length - 1);
    }
}
