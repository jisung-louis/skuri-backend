package com.skuri.skuri_backend.domain.notice.entity;

import jakarta.persistence.Column;
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

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notice_read_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeReadStatus {

    @EmbeddedId
    private NoticeReadStatusId id;

    @MapsId("noticeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    private NoticeReadStatus(Notice notice, String userId, LocalDateTime readAt) {
        this.id = NoticeReadStatusId.of(userId, notice.getId());
        this.notice = notice;
        this.read = true;
        this.readAt = readAt;
    }

    public static NoticeReadStatus create(Notice notice, String userId, LocalDateTime readAt) {
        return new NoticeReadStatus(notice, userId, readAt);
    }

    public void markRead(LocalDateTime readAt) {
        this.read = true;
        this.readAt = readAt;
    }
}
