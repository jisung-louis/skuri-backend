package com.skuri.skuri_backend.domain.notice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.skuri.skuri_backend.common.config.ObjectMapperConfig;
import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

public final class NoticeHashUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private NoticeHashUtils() {
    }

    public static String stableId(String fullLink, int categoryId, String title) {
        String seed = (fullLink == null || fullLink.isBlank()) ? (categoryId + ":" + title) : fullLink;
        return Base64.getEncoder()
                .withoutPadding()
                .encodeToString(seed.getBytes(StandardCharsets.UTF_8))
                .substring(0, Math.min(120, Base64.getEncoder().withoutPadding().encodeToString(seed.getBytes(StandardCharsets.UTF_8)).length()));
    }

    public static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 해시 생성에 실패했습니다.", e);
        }
    }

    public static String rssFingerprint(String title, String fullLink, String rawDate) {
        return sha1(title + "|" + fullLink + "|" + rawDate);
    }

    public static String detailHash(String html, List<NoticeAttachment> attachments) {
        return sha1(normalize(html) + "|" + attachmentsJson(attachments));
    }

    public static String contentHash(
            String title,
            String rssPreview,
            String category,
            LocalDateTime postedAt,
            String author,
            String detailHash
    ) {
        return sha1(
                normalize(title) + "|"
                        + normalize(rssPreview) + "|"
                        + normalize(category) + "|"
                        + normalize(author) + "|"
                        + (postedAt == null ? "" : DATE_FORMATTER.format(postedAt)) + "|"
                        + normalize(detailHash)
        );
    }

    private static String attachmentsJson(List<NoticeAttachment> attachments) {
        try {
            return ObjectMapperConfig.SHARED_OBJECT_MAPPER.writeValueAsString(attachments == null ? List.of() : attachments);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("공지 첨부파일 해시 직렬화에 실패했습니다.", e);
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
