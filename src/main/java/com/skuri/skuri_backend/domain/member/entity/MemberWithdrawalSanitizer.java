package com.skuri.skuri_backend.domain.member.entity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class MemberWithdrawalSanitizer {

    public static final String WITHDRAWN_AUTHOR_ID = "withdrawn-member";
    public static final String WITHDRAWN_DISPLAY_NAME = "탈퇴한 사용자";
    private static final String WITHDRAWN_EMAIL_DOMAIN = "deleted.skuri.local";
    private static final int MAX_EMAIL_LOCAL_PART_LENGTH = 240;

    private MemberWithdrawalSanitizer() {
    }

    public static String redactEmail(String memberId) {
        String localPart = "withdrawn+" + hashMemberId(memberId);
        if (localPart.length() > MAX_EMAIL_LOCAL_PART_LENGTH) {
            localPart = localPart.substring(0, MAX_EMAIL_LOCAL_PART_LENGTH);
        }
        return localPart + "@" + WITHDRAWN_EMAIL_DOMAIN;
    }

    public static boolean isWithdrawnAuthorId(String authorId) {
        return WITHDRAWN_AUTHOR_ID.equals(authorId);
    }

    private static String hashMemberId(String memberId) {
        String source = memberId == null ? "unknown" : memberId;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 해시 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
