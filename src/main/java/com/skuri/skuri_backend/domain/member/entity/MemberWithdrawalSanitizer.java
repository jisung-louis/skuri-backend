package com.skuri.skuri_backend.domain.member.entity;

import java.util.Locale;

public final class MemberWithdrawalSanitizer {

    public static final String WITHDRAWN_AUTHOR_ID = "withdrawn-member";
    public static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";
    private static final String WITHDRAWN_EMAIL_DOMAIN = "deleted.skuri.local";
    private static final int MAX_EMAIL_LOCAL_PART_LENGTH = 240;

    private MemberWithdrawalSanitizer() {
    }

    public static String redactEmail(String memberId) {
        String normalizedId = memberId == null ? "unknown" : memberId.trim().toLowerCase(Locale.ROOT);
        String localPart = "withdrawn+" + normalizedId.replaceAll("[^a-z0-9]", "");
        if (localPart.length() > MAX_EMAIL_LOCAL_PART_LENGTH) {
            localPart = localPart.substring(0, MAX_EMAIL_LOCAL_PART_LENGTH);
        }
        return localPart + "@" + WITHDRAWN_EMAIL_DOMAIN;
    }

    public static boolean isWithdrawnAuthorId(String authorId) {
        return WITHDRAWN_AUTHOR_ID.equals(authorId);
    }
}
