package com.skuri.skuri_backend.domain.notice.service;

import com.skuri.skuri_backend.domain.notice.entity.NoticeCategory;

import java.util.List;

public interface NoticeRssClient {

    List<NoticeFeedItem> fetch(NoticeCategory category, int rowCount);
}
