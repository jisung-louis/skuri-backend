package com.skuri.skuri_backend.domain.app.entity;

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
public class AppNoticeReadStatusId implements Serializable {

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "app_notice_id", length = 36)
    private String appNoticeId;

    private AppNoticeReadStatusId(String userId, String appNoticeId) {
        this.userId = userId;
        this.appNoticeId = appNoticeId;
    }

    public static AppNoticeReadStatusId of(String userId, String appNoticeId) {
        return new AppNoticeReadStatusId(userId, appNoticeId);
    }
}
