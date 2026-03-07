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
@Table(name = "notice_likes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeLike {

    @EmbeddedId
    private NoticeLikeId id;

    @MapsId("noticeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    private NoticeLike(Notice notice, String userId) {
        this.id = NoticeLikeId.of(userId, notice.getId());
        this.notice = notice;
    }

    public static NoticeLike create(Notice notice, String userId) {
        return new NoticeLike(notice, userId);
    }
}
