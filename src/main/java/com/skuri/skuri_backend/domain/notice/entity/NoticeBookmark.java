package com.skuri.skuri_backend.domain.notice.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notice_bookmarks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeBookmark {

    @EmbeddedId
    private NoticeBookmarkId id;

    @MapsId("noticeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    private NoticeBookmark(Notice notice, String userId) {
        this.id = NoticeBookmarkId.of(userId, notice.getId());
        this.notice = notice;
    }

    public static NoticeBookmark create(Notice notice, String userId) {
        return new NoticeBookmark(notice, userId);
    }
}
