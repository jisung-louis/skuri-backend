package com.skuri.skuri_backend.domain.member.entity;

import com.skuri.skuri_backend.domain.member.entity.converter.BooleanMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSetting {

    @Column(name = "all_notifications")
    private boolean allNotifications;

    @Column(name = "party_notifications")
    private boolean partyNotifications;

    @Column(name = "notice_notifications")
    private boolean noticeNotifications;

    @Column(name = "board_like_notifications")
    private boolean boardLikeNotifications;

    @Column(name = "board_comment_notifications")
    private boolean boardCommentNotifications;

    @Column(name = "system_notifications")
    private boolean systemNotifications;

    @Convert(converter = BooleanMapJsonConverter.class)
    @Column(name = "notice_notifications_detail", columnDefinition = "json")
    private Map<String, Boolean> noticeNotificationsDetail;

    private NotificationSetting(
            boolean allNotifications,
            boolean partyNotifications,
            boolean noticeNotifications,
            boolean boardLikeNotifications,
            boolean boardCommentNotifications,
            boolean systemNotifications,
            Map<String, Boolean> noticeNotificationsDetail
    ) {
        this.allNotifications = allNotifications;
        this.partyNotifications = partyNotifications;
        this.noticeNotifications = noticeNotifications;
        this.boardLikeNotifications = boardLikeNotifications;
        this.boardCommentNotifications = boardCommentNotifications;
        this.systemNotifications = systemNotifications;
        this.noticeNotificationsDetail = noticeNotificationsDetail != null
                ? new HashMap<>(noticeNotificationsDetail)
                : new HashMap<>();
    }

    public static NotificationSetting defaultSetting() {
        Map<String, Boolean> defaults = new HashMap<>();
        defaults.put("news", Boolean.TRUE);
        defaults.put("academy", Boolean.TRUE);
        defaults.put("scholarship", Boolean.TRUE);

        return new NotificationSetting(
                true,
                true,
                true,
                true,
                true,
                true,
                defaults
        );
    }

    public void apply(
            Boolean allNotifications,
            Boolean partyNotifications,
            Boolean noticeNotifications,
            Boolean boardLikeNotifications,
            Boolean boardCommentNotifications,
            Boolean systemNotifications,
            Map<String, Boolean> noticeNotificationsDetail
    ) {
        if (allNotifications != null) {
            this.allNotifications = allNotifications;
        }
        if (partyNotifications != null) {
            this.partyNotifications = partyNotifications;
        }
        if (noticeNotifications != null) {
            this.noticeNotifications = noticeNotifications;
        }
        if (boardLikeNotifications != null) {
            this.boardLikeNotifications = boardLikeNotifications;
        }
        if (boardCommentNotifications != null) {
            this.boardCommentNotifications = boardCommentNotifications;
        }
        if (systemNotifications != null) {
            this.systemNotifications = systemNotifications;
        }
        if (noticeNotificationsDetail != null) {
            this.noticeNotificationsDetail = new HashMap<>(noticeNotificationsDetail);
        }
        if (this.noticeNotificationsDetail == null) {
            this.noticeNotificationsDetail = new HashMap<>();
        }
    }
}
