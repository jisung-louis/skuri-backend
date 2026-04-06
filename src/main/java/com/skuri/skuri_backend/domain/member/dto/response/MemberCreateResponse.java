package com.skuri.skuri_backend.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "회원 생성/멱등 생성 응답")
public record MemberCreateResponse(
        @Schema(description = "회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String id,
        @Schema(description = "성결대 이메일", example = "test@sungkyul.ac.kr")
        String email,
        @Schema(description = "앱 내 닉네임", example = "스쿠리 유저", nullable = true)
        String nickname,
        @Schema(description = "학번", example = "2023112233", nullable = true)
        String studentId,
        @Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
        String department,
        @Schema(
                description = "앱 내 프로필 이미지 URL",
                example = "https://cdn.skuri.app/uploads/profiles/dw9rPtuticbjnaYPkeiF3RGPpqk1/2026/04/06/photo.jpg",
                nullable = true
        )
        String photoUrl,
        @Schema(description = "실명(가입 시 소셜 계정 이름 기반)", example = "홍길동", nullable = true)
        String realname,
        @Schema(description = "관리자 여부", example = "false")
        boolean isAdmin,
        @Schema(description = "계좌 정보", nullable = true)
        MemberBankAccountResponse bankAccount,
        @Schema(description = "가입 시각", example = "2026-03-02T18:37:21")
        LocalDateTime joinedAt
) {
}
