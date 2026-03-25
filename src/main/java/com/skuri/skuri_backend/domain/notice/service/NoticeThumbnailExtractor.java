package com.skuri.skuri_backend.domain.notice.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

public final class NoticeThumbnailExtractor {

    private NoticeThumbnailExtractor() {
    }

    public static String extract(String html) {
        if (!StringUtils.hasText(html)) {
            return null;
        }

        Document document = Jsoup.parseBodyFragment(html);
        Element image = document.selectFirst("img[src]");
        if (image == null) {
            return null;
        }

        String src = image.attr("src");
        return StringUtils.hasText(src) ? src.trim() : null;
    }
}
