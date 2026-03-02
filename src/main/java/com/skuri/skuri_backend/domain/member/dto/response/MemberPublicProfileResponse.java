package com.skuri.skuri_backend.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 공개 프로필")
public record MemberPublicProfileResponse(
        @Schema(description = "회원 ID(Firebase UID)", example = "dw9rPtuticbjnaYPkeiF3RGPpqk1")
        String id,
        @Schema(description = "앱 내 닉네임", example = "스쿠리 유저", nullable = true)
        String nickname,
        @Schema(description = "학과", example = "컴퓨터공학과", nullable = true)
        String department,
        @Schema(description = "앱 내 프로필 이미지 URL", example = "https://cdn.skuri.app/profiles/user-1.png", nullable = true)
        String photoUrl
) {
}
