package com.skuri.skuri_backend.infra.migration.notice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.domain.notice.entity.NoticeAttachment;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NoticeExportItem(
        String id,
        NoticeExportData data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NoticeExportData(
            String id,
            String title,
            String content,
            String link,
            JsonNode postedAt,
            String category,
            String author,
            String department,
            String source,
            String contentHash,
            String contentDetail,
            List<NoticeAttachment> contentAttachments,
            JsonNode createdAt,
            JsonNode updatedAt,
            Integer viewCount,
            Integer likeCount
    ) {
    }
}
