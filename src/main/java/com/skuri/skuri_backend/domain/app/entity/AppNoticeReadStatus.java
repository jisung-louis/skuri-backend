package com.skuri.skuri_backend.domain.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
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
@Table(
        name = "app_notice_read_status",
        indexes = {
                @Index(name = "idx_app_notice_read_app_notice", columnList = "app_notice_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNoticeReadStatus {

    @EmbeddedId
    private AppNoticeReadStatusId id;

    @MapsId("appNoticeId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_notice_id", nullable = false)
    private AppNotice appNotice;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    private AppNoticeReadStatus(AppNotice appNotice, String userId, LocalDateTime readAt) {
        this.id = AppNoticeReadStatusId.of(userId, appNotice.getId());
        this.appNotice = appNotice;
        this.readAt = readAt;
    }

    public static AppNoticeReadStatus create(AppNotice appNotice, String userId, LocalDateTime readAt) {
        return new AppNoticeReadStatus(appNotice, userId, readAt);
    }
}
