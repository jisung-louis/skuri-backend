package com.skuri.skuri_backend.domain.notice.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SungkyulNoticeRssClientTest {

    private final SungkyulNoticeRssClient client = new SungkyulNoticeRssClient();

    @Test
    void parsePostedAt_zulu시간대는_kst로변환한다() {
        LocalDateTime parsed = ReflectionTestUtils.invokeMethod(client, "parsePostedAt", "2026-03-05T01:00:00Z");

        assertEquals(LocalDateTime.of(2026, 3, 5, 10, 0), parsed);
    }

    @Test
    void parsePostedAt_offset포함ISO는_중복오프셋없이파싱한다() {
        LocalDateTime parsed = ReflectionTestUtils.invokeMethod(client, "parsePostedAt", "2026-03-05T10:00:00+09:00");

        assertEquals(LocalDateTime.of(2026, 3, 5, 10, 0), parsed);
    }

    @Test
    void parsePostedAt_offset없는로컬시간은_kst기준으로해석한다() {
        LocalDateTime parsed = ReflectionTestUtils.invokeMethod(client, "parsePostedAt", "2026-03-05 10:00:00");

        assertEquals(LocalDateTime.of(2026, 3, 5, 10, 0), parsed);
    }
}
