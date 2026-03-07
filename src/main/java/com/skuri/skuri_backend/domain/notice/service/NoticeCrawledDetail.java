package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;

import java.util.List;

public record NoticeCrawledDetail(
        String html,
        String text,
        List<NoticeAttachment> attachments
) {
    public static NoticeCrawledDetail empty() {
        return new NoticeCrawledDetail("", "", List.of());
    }
}
