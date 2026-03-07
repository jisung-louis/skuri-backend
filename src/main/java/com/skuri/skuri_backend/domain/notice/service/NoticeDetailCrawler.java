package com.skuri.skuri_backend.domain.notice.service;

public interface NoticeDetailCrawler {

    NoticeCrawledDetail crawl(String noticeUrl);
}
