package com.skuri.skuri_backend.domain.member.repository;

import com.skuri.skuri_backend.domain.member.entity.MemberStatus;

import java.time.LocalDateTime;

public record AdminMemberSummaryProjection(
        String id,
        String email,
        String nickname,
        String realname,
        String studentId,
        String department,
        boolean isAdmin,
        LocalDateTime joinedAt,
        LocalDateTime lastLogin,
        String lastLoginOs,
        MemberStatus status
) {
}
