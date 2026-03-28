package com.skuri.skuri_backend.domain.member.dto.response;

import com.skuri.skuri_backend.domain.member.entity.MemberStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 회원 목록 응답")
public record AdminMemberSummaryResponse(
        @Schema(description = "회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String id,
        @Schema(description = "성결대 이메일", example = "user@sungkyul.ac.kr")
        String email,
        @Schema(description = "앱 내 닉네임", example = "스쿠리 유저", nullable = true)
        String nickname,
        @Schema(description = "실명", example = "홍길동", nullable = true)
        String realname,
        @Schema(description = "학번", example = "2023112233", nullable = true)
        String studentId,
        @Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
        String department,
        @Schema(description = "관리자 여부", example = "false")
        boolean isAdmin,
        @Schema(description = "가입 시각", example = "2026-03-02T18:37:21")
        LocalDateTime joinedAt,
        @Schema(description = "마지막 로그인 시각", example = "2026-03-04T10:00:00", nullable = true)
        LocalDateTime lastLogin,
        @Schema(description = "회원 상태", example = "ACTIVE")
        MemberStatus status
) {
}
