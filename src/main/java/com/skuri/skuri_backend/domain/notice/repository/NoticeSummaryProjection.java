package com.skuri.skuri_backend.domain.notice.repository;

import java.time.LocalDateTime;

public record NoticeSummaryProjection(
        String id,
        String title,
        String rssPreview,
        String category,
        String department,
        String author,
        LocalDateTime postedAt,
        int viewCount,
        int likeCount,
        int commentCount,
        int bookmarkCount,
        String thumbnailUrl
) {
}
