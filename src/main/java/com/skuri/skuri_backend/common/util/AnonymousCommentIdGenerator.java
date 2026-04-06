package com.skuri.skuri_backend.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class AnonymousCommentIdGenerator {

    private static final String PREFIX = "ac:";
    private static final int HASH_LENGTH = 32;

    private AnonymousCommentIdGenerator() {
    }

    public static String generate(String scopeId, String memberId) {
        return PREFIX + sha256Hex(scopeId + ":" + memberId).substring(0, HASH_LENGTH);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 익명 식별자 생성에 실패했습니다.", e);
        }
    }
}
