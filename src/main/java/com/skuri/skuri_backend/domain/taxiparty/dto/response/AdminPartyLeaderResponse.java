package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 파티 상세의 리더 요약 정보")
public record AdminPartyLeaderResponse(
        @Schema(description = "리더 ID", example = "leader-uid")
        String id,
        @Schema(description = "리더 닉네임", example = "스쿠리 유저", nullable = true)
        String nickname,
        @Schema(description = "리더 프로필 이미지", example = "https://cdn.skuri.app/profiles/leader.png", nullable = true)
        String photoUrl
) {
}
