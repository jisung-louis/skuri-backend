package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;

import java.util.List;

public record NoticeCrawledDetail(
        String html,
        String text,
        List<NoticeAttachment> attachments,
        boolean successful
) {
    public static NoticeCrawledDetail empty() {
        return new NoticeCrawledDetail("", "", List.of(), true);
    }

    public static NoticeCrawledDetail of(String html, String text, List<NoticeAttachment> attachments) {
        return new NoticeCrawledDetail(html, text, attachments, true);
    }

    public static NoticeCrawledDetail failed() {
        return new NoticeCrawledDetail(null, null, null, false);
    }
}
