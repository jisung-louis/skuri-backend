package com.skuri.skuri_backend.domain.member.dto.response;

import java.util.Map;

public record MemberNotificationSettingResponse(
        boolean allNotifications,
        boolean partyNotifications,
        boolean noticeNotifications,
        boolean boardLikeNotifications,
        boolean boardCommentNotifications,
        boolean systemNotifications,
        Map<String, Boolean> noticeNotificationsDetail
) {
}
