package com.skuri.skuri_backend.domain.notice.service;

import java.time.LocalDateTime;

public record NoticeFeedItem(
        String id,
        String title,
        String rssPreview,
        String link,
        LocalDateTime postedAt,
        String rawDate,
        String category,
        String department,
        String author,
        String source,
        String rssFingerprint
) {
}
