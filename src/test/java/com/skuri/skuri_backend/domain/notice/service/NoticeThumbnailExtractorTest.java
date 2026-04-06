package com.skuri.skuri_backend.domain.notice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoticeThumbnailExtractorTest {

    @Test
    void extract_https이미지는_그대로반환한다() {
        String html = "<p>본문</p><img src=\"https://example.com/notice-thumb.jpg\" />";

        String thumbnailUrl = NoticeThumbnailExtractor.extract(html);

        assertEquals("https://example.com/notice-thumb.jpg", thumbnailUrl);
    }

    @Test
    void extract_빈src는_null을반환한다() {
        String html = "<p>본문</p><img src=\"   \" />";

        String thumbnailUrl = NoticeThumbnailExtractor.extract(html);

        assertNull(thumbnailUrl);
    }

    @Test
    void extract_dataUrl은_null을반환한다() {
        String html = "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUA\" />";

        String thumbnailUrl = NoticeThumbnailExtractor.extract(html);

        assertNull(thumbnailUrl);
    }

    @Test
    void extract_blobUrl은_null을반환한다() {
        String html = "<img src=\"blob:https://example.com/550e8400-e29b-41d4-a716-446655440000\" />";

        String thumbnailUrl = NoticeThumbnailExtractor.extract(html);

        assertNull(thumbnailUrl);
    }

    @Test
    void extract_이미지태그가없으면_null을반환한다() {
        String html = "<p>이미지 없음</p>";

        String thumbnailUrl = NoticeThumbnailExtractor.extract(html);

        assertNull(thumbnailUrl);
    }

    @Test
    void extract_과도하게긴원격url은_null을반환한다() {
        String longUrl = "https://example.com/" + "a".repeat(70_000) + ".jpg";
        String html = "<img src=\"" + longUrl + "\" />";

        String thumbnailUrl = NoticeThumbnailExtractor.extract(html);

        assertNull(thumbnailUrl);
    }
}
