package com.skuri.skuri_backend.domain.member.dto.request;

import java.util.Map;

public record UpdateMemberNotificationSettingsRequest(
        Boolean allNotifications,
        Boolean partyNotifications,
        Boolean noticeNotifications,
        Boolean boardLikeNotifications,
        Boolean boardCommentNotifications,
        Boolean systemNotifications,
        Map<String, Boolean> noticeNotificationsDetail
) {
}
