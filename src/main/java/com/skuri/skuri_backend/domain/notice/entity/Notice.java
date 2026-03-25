package com.skuri.skuri_backend.domain.notice.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.notice.entity.converter.NoticeAttachmentListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @Column(length = 120)
    private String id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "rss_preview", columnDefinition = "TEXT")
    private String rssPreview;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false, length = 500)
    private String link;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Column(length = 50)
    private String category;

    @Column(length = 50)
    private String department;

    @Column(length = 100)
    private String author;

    @Column(length = 20)
    private String source;

    @Column(name = "rss_fingerprint", nullable = false, length = 40)
    private String rssFingerprint;

    @Column(name = "detail_hash", length = 40)
    private String detailHash;

    @Column(name = "content_hash", nullable = false, length = 40)
    private String contentHash;

    @Column(name = "detail_checked_at")
    private LocalDateTime detailCheckedAt;

    @Column(name = "body_text", columnDefinition = "LONGTEXT")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "LONGTEXT")
    private String bodyHtml;

    @Convert(converter = NoticeAttachmentListJsonConverter.class)
    @Column(name = "attachments", columnDefinition = "json")
    private List<NoticeAttachment> attachments = new ArrayList<>();

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "bookmark_count", nullable = false)
    private int bookmarkCount;

    private Notice(
            String id,
            String title,
            String rssPreview,
            String link,
            LocalDateTime postedAt,
            String category,
            String department,
            String author,
            String source,
            String rssFingerprint,
            String detailHash,
            String contentHash,
            LocalDateTime detailCheckedAt,
            String bodyText,
            String bodyHtml,
            List<NoticeAttachment> attachments
    ) {
        this.id = id;
        this.title = title;
        this.rssPreview = rssPreview;
        this.summary = null;
        this.link = link;
        this.postedAt = postedAt;
        this.category = category;
        this.department = department;
        this.author = author;
        this.source = source;
        this.rssFingerprint = rssFingerprint;
        this.detailHash = detailHash;
        this.contentHash = contentHash;
        this.detailCheckedAt = detailCheckedAt;
        this.bodyText = bodyText;
        this.bodyHtml = bodyHtml;
        this.attachments = new ArrayList<>(attachments == null ? List.of() : attachments);
        this.viewCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.bookmarkCount = 0;
    }

    public static Notice create(
            String id,
            String title,
            String rssPreview,
            String link,
            LocalDateTime postedAt,
            String category,
            String department,
            String author,
            String source,
            String rssFingerprint,
            String detailHash,
            String contentHash,
            LocalDateTime detailCheckedAt,
            String bodyText,
            String bodyHtml,
            List<NoticeAttachment> attachments
    ) {
        return new Notice(
                id,
                title,
                rssPreview,
                link,
                postedAt,
                category,
                department,
                author,
                source,
                rssFingerprint,
                detailHash,
                contentHash,
                detailCheckedAt,
                bodyText,
                bodyHtml,
                attachments
        );
    }

    public void applySync(
            String title,
            String rssPreview,
            String link,
            LocalDateTime postedAt,
            String category,
            String department,
            String author,
            String source,
            String rssFingerprint,
            String detailHash,
            String contentHash,
            LocalDateTime detailCheckedAt,
            String bodyText,
            String bodyHtml,
            List<NoticeAttachment> attachments
    ) {
        this.title = title;
        this.rssPreview = rssPreview;
        this.link = link;
        this.postedAt = postedAt;
        this.category = category;
        this.department = department;
        this.author = author;
        this.source = source;
        this.rssFingerprint = rssFingerprint;
        this.detailHash = detailHash;
        this.contentHash = contentHash;
        this.detailCheckedAt = detailCheckedAt;
        this.bodyText = bodyText;
        this.bodyHtml = bodyHtml;
        this.attachments = new ArrayList<>(attachments == null ? List.of() : attachments);
    }

    public void clearSummary() {
        this.summary = null;
    }

    public void refreshDetailCheckedAt(LocalDateTime detailCheckedAt) {
        this.detailCheckedAt = detailCheckedAt;
    }

    public void incrementViewCount() {
        this.viewCount += 1;
    }

    public void increaseLikeCount(int delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void increaseCommentCount(int delta) {
        this.commentCount = Math.max(0, this.commentCount + delta);
    }

    public void increaseBookmarkCount(int delta) {
        this.bookmarkCount = Math.max(0, this.bookmarkCount + delta);
    }
}
