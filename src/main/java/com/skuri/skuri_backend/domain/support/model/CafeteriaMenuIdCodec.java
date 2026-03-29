package com.skuri.skuri_backend.domain.support.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CafeteriaMenuIdCodec {

    private CafeteriaMenuIdCodec() {
    }

    public static String encode(String weekId, String category, String title) {
        return weekId + "." + category + "." + shortHash(title);
    }

    public static CafeteriaMenuIdParts parse(String menuId) {
        String[] parts = menuId == null ? new String[0] : menuId.split("\\.", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException("menuId 형식이 올바르지 않습니다.");
        }
        return new CafeteriaMenuIdParts(parts[0], parts[1], parts[2]);
    }

    public static boolean matches(String menuId, String weekId, String category, String title) {
        return encode(weekId, category, title).equals(menuId);
    }

    private static String shortHash(String title) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(title.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }
}
