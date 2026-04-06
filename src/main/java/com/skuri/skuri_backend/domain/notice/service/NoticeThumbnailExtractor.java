package com.skuri.skuri_backend.domain.notice.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NoticeThumbnailExtractor {

    private static final Pattern URI_SCHEME_PATTERN = Pattern.compile("^(?<scheme>[a-zA-Z][a-zA-Z0-9+.-]*):");
    private static final int MAX_STORED_URL_BYTES = 60_000;

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

        return sanitize(image.attr("src"));
    }

    private static String sanitize(String rawSrc) {
        if (!StringUtils.hasText(rawSrc)) {
            return null;
        }

        String src = rawSrc.trim();
        if (src.isEmpty()) {
            return null;
        }

        Matcher matcher = URI_SCHEME_PATTERN.matcher(src);
        if (matcher.find()) {
            String scheme = matcher.group("scheme").toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return null;
            }
        }

        // Reject malformed overlong values before hitting DB column limits.
        if (src.getBytes(StandardCharsets.UTF_8).length > MAX_STORED_URL_BYTES) {
            return null;
        }

        return src;
    }
}
