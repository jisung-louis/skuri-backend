package com.skuri.skuri_backend.domain.member.dto.response;

import java.time.LocalDateTime;

public record MemberMeResponse(
        String id,
        String email,
        String nickname,
        String studentId,
        String department,
        String photoUrl,
        String realname,
        boolean isAdmin,
        MemberBankAccountResponse bankAccount,
        MemberNotificationSettingResponse notificationSetting,
        LocalDateTime joinedAt,
        LocalDateTime lastLogin
) {
}
