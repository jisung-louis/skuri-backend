package com.skuri.skuri_backend.domain.minecraft.service;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import com.skuri.skuri_backend.domain.minecraft.config.MinecraftBridgeProperties;
import com.skuri.skuri_backend.domain.minecraft.entity.MinecraftEdition;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class MinecraftIdentityService {

    private static final Pattern JAVA_UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");
    private static final Pattern JAVA_UUID_WITH_HYPHEN_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern RTDB_FORBIDDEN_CHARS = Pattern.compile("[.#$\\[\\]]");

    private final MinecraftBridgeProperties bridgeProperties;

    public MinecraftIdentityService(MinecraftBridgeProperties bridgeProperties) {
        this.bridgeProperties = bridgeProperties;
    }

    public String normalizeGameName(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "gameName은 필수입니다.");
        }
        return value.trim();
    }

    public String normalizeAccountKey(MinecraftEdition edition, String gameName, String rawIdentity) {
        return switch (edition) {
            case JAVA -> normalizeJavaUuid(rawIdentity);
            case BEDROCK -> "be:" + toStoredName(StringUtils.hasText(rawIdentity) ? rawIdentity : gameName);
        };
    }

    public String resolveAvatarUuid(MinecraftEdition edition, String normalizedKey) {
        if (edition == MinecraftEdition.JAVA) {
            return normalizeJavaUuid(normalizedKey);
        }
        return bridgeProperties.normalizedDefaultAvatarUuid();
    }

    public String resolveStoredName(MinecraftEdition edition, String gameName, String normalizedKey) {
        if (edition == MinecraftEdition.BEDROCK) {
            String raw = normalizedKey.startsWith("be:") ? normalizedKey.substring(3) : normalizedKey;
            return toStoredName(raw);
        }
        return null;
    }

    public String toSyntheticSenderId(String normalizedKey, String senderName, MinecraftEdition edition) {
        String basis = edition.name() + ":" + normalizedKey + ":" + normalizeGameName(senderName);
        return "mc:" + sha256Hex(basis).substring(0, 32);
    }

    public String toDisplayUuid(String normalizedKey) {
        if (!StringUtils.hasText(normalizedKey)) {
            return bridgeProperties.normalizedDefaultAvatarUuid();
        }
        return normalizedKey.startsWith("be:")
                ? bridgeProperties.normalizedDefaultAvatarUuid()
                : normalizeJavaUuid(normalizedKey);
    }

    public String toHyphenatedJavaUuid(String normalizedKey) {
        String raw = normalizeJavaUuid(normalizedKey);
        return raw.substring(0, 8)
                + "-" + raw.substring(8, 12)
                + "-" + raw.substring(12, 16)
                + "-" + raw.substring(16, 20)
                + "-" + raw.substring(20);
    }

    public String normalizeJavaUuid(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "minecraftUuid는 필수입니다.");
        }
        String trimmed = value.trim();
        if (JAVA_UUID_WITH_HYPHEN_PATTERN.matcher(trimmed).matches()) {
            trimmed = trimmed.replace("-", "");
        }
        if (!JAVA_UUID_PATTERN.matcher(trimmed).matches()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효한 Java UUID 형식이 아닙니다.");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    public String toStoredName(String value) {
        String normalized = normalizeGameName(value);
        if (RTDB_FORBIDDEN_CHARS.matcher(normalized).find()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "닉네임에 다음 문자를 사용할 수 없습니다: . # $ [ ]");
        }
        String replaced = normalized.replaceAll("\\s+", "_");
        return replaced.length() > 12 ? replaced.substring(0, 12) : replaced;
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
