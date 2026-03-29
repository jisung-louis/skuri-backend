package com.skuri.skuri_backend.domain.support.model;

import java.util.List;

public record CafeteriaMenuEntryMetadata(
        String title,
        List<CafeteriaMenuBadgeMetadata> badges,
        int likeCount,
        int dislikeCount
) {
}
