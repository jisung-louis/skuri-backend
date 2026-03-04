package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "파티 멤버 응답")
public record PartyMemberResponse(
        @Schema(description = "회원 ID", example = "member_uuid")
        String id,
        @Schema(description = "닉네임", example = "홍길동")
        String nickname,
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.skuri.app/profile.png", nullable = true)
        String photoUrl,
        @Schema(description = "리더 여부", example = "true")
        boolean isLeader,
        @Schema(description = "참여 시각", example = "2026-03-03T13:00:00")
        LocalDateTime joinedAt
) {
}
