package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SungkyulNoticeDetailCrawler implements NoticeDetailCrawler {

    private static final String BASE_URL = "https://www.sungkyul.ac.kr";
    private static final int TIMEOUT_MILLIS = 10_000;

    @Override
    public NoticeCrawledDetail crawl(String noticeUrl) {
        if (!StringUtils.hasText(noticeUrl)) {
            return NoticeCrawledDetail.failed();
        }

        try {
            Document document = Jsoup.connect(noticeUrl)
                    .userAgent("Mozilla/5.0")
                    .sslSocketFactory(SungkyulNoticeTlsSupport.insecureSocketFactory())
                    .timeout(TIMEOUT_MILLIS)
                    .get();

            Element viewCon = document.selectFirst(".view-con");
            if (viewCon == null) {
                return NoticeCrawledDetail.empty();
            }

            viewCon.select("img[src]").forEach(image -> image.attr("src", toAbsoluteUrl(image.attr("src"))));
            String html = viewCon.html() == null ? "" : viewCon.html();
            String text = NoticeBodyTextExtractor.extract(html);

            List<NoticeAttachment> attachments = new ArrayList<>();
            for (Element item : document.select(".view-file li")) {
                attachments.add(parseAttachment(item));
            }
            attachments.removeIf(attachment -> !StringUtils.hasText(attachment.name())
                    && !StringUtils.hasText(attachment.downloadUrl())
                    && !StringUtils.hasText(attachment.previewUrl()));

            return NoticeCrawledDetail.of(html, text, attachments);
        } catch (IOException e) {
            log.warn("공지 상세 크롤링 실패: noticeUrl={}", noticeUrl, e);
            return NoticeCrawledDetail.failed();
        }
    }

    private NoticeAttachment parseAttachment(Element item) {
        String name = "";
        String downloadUrl = "";
        String previewUrl = "";

        for (Element link : item.select("a")) {
            String text = link.text().trim();
            if (!text.isEmpty() && name.isEmpty()) {
                name = text;
            }
            String href = link.attr("href").trim();
            if (href.isEmpty()) {
                continue;
            }
            String absoluteUrl = toAbsoluteUrl(href);
            if (href.contains("download.do")) {
                downloadUrl = absoluteUrl;
            } else if (href.contains("synapView.do")) {
                previewUrl = absoluteUrl;
            }
        }

        return new NoticeAttachment(name, downloadUrl, previewUrl);
    }

    private String toAbsoluteUrl(String href) {
        if (!StringUtils.hasText(href)) {
            return "";
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            return href;
        }
        if (href.startsWith("/")) {
            return BASE_URL + href;
        }
        return (BASE_URL + "/" + href).replaceAll("([^:]//)/+", "$1");
    }
}
