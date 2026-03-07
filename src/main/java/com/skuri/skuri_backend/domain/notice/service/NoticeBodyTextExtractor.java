package com.skuri.skuri_backend.domain.notice.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class NoticeBodyTextExtractor {

    private NoticeBodyTextExtractor() {
    }

    public static String extract(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }

        Document document = Jsoup.parseBodyFragment(html);
        Element body = document.body();

        body.select("br").forEach(element -> element.after("\n"));
        body.select("p,div,section,article,li,tr,h1,h2,h3,h4,h5,h6").forEach(element -> element.appendText("\n"));
        body.select("td,th").forEach(element -> element.appendText("\t"));

        return Arrays.stream(body.wholeText().replace('\u00A0', ' ').split("\\R"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }
}
