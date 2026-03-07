package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.domain.notice.entity.NoticeCategory;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SungkyulNoticeRssClient implements NoticeRssClient {

    private static final String RSS_BASE_URL = "https://www.sungkyul.ac.kr/bbs/skukr";
    private static final String BASE_URL = "https://www.sungkyul.ac.kr";
    private static final int TIMEOUT_MILLIS = 10_000;
    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    @Override
    public List<NoticeFeedItem> fetch(NoticeCategory category, int rowCount) {
        String rssUrl = RSS_BASE_URL + "/" + category.categoryId() + "/rssList.do?row=" + rowCount;

        try {
            String body = Jsoup.connect(rssUrl)
                    .userAgent("Mozilla/5.0")
                    .ignoreContentType(true)
                    .sslSocketFactory(SungkyulNoticeTlsSupport.insecureSocketFactory())
                    .timeout(TIMEOUT_MILLIS)
                    .execute()
                    .body();
            Document document = Jsoup.parse(body, "", Parser.xmlParser());
            List<NoticeFeedItem> items = new ArrayList<>();
            for (Element item : document.select("item")) {
                items.add(toFeedItem(category, item));
            }
            return items;
        } catch (IOException e) {
            log.warn("공지 RSS 조회 실패: category={}, rssUrl={}", category.label(), rssUrl, e);
            return List.of();
        }
    }

    private NoticeFeedItem toFeedItem(NoticeCategory category, Element item) {
        String link = normalizeLink(text(item, "link"));
        String title = defaultIfBlank(text(item, "title"), "제목 없음");
        String rssPreview = firstNonBlank(text(item, "description"), text(item, "content"), text(item, "contentSnippet"));
        String rawDate = firstNonBlank(text(item, "pubDate"), text(item, "isoDate"));
        LocalDateTime postedAt = parsePostedAt(rawDate);
        String author = text(item, "author");

        return new NoticeFeedItem(
                NoticeHashUtils.stableId(link, category.categoryId(), title),
                title,
                rssPreview,
                link,
                postedAt,
                rawDate,
                category.label(),
                "성결대학교",
                author,
                "RSS",
                NoticeHashUtils.rssFingerprint(title, link, rawDate)
        );
    }

    private String text(Element item, String tagName) {
        Element target = item.selectFirst(tagName);
        return target == null ? "" : target.text().trim();
    }

    private String normalizeLink(String link) {
        if (!StringUtils.hasText(link)) {
            return "";
        }
        String trimmed = link.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return BASE_URL + trimmed;
        }
        return BASE_URL + "/" + trimmed;
    }

    private LocalDateTime parsePostedAt(String rawDate) {
        if (!StringUtils.hasText(rawDate)) {
            return LocalDateTime.now();
        }

        String trimmed = rawDate.trim();
        try {
            return OffsetDateTime.parse(trimmed)
                    .withOffsetSameInstant(KST_OFFSET)
                    .toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .withOffsetSameInstant(KST_OFFSET)
                        .toLocalDateTime();
            } catch (DateTimeParseException ex) {
                try {
                    String normalized = trimmed.contains("T") ? trimmed : trimmed.replace(' ', 'T');
                    return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                } catch (DateTimeParseException secondEx) {
                    log.warn("공지 날짜 파싱 실패: rawDate={}", rawDate, secondEx);
                    return LocalDateTime.now();
                }
            }
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }
}
