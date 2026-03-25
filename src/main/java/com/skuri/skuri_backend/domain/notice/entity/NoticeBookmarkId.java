package com.skuri.skuri_backend.domain.notice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeBookmarkId implements Serializable {

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "notice_id", length = 120)
    private String noticeId;

    private NoticeBookmarkId(String userId, String noticeId) {
        this.userId = userId;
        this.noticeId = noticeId;
    }

    public static NoticeBookmarkId of(String userId, String noticeId) {
        return new NoticeBookmarkId(userId, noticeId);
    }
}
