package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "파티 참가자 요약")
public record PartyParticipantSummaryResponse(
        @Schema(description = "참가자 ID", example = "member_uuid")
        String id,
        @Schema(description = "참가자 프로필 이미지", nullable = true, example = "https://cdn.skuri.app/uploads/profiles/member.jpg")
        String photoUrl,
        @Schema(description = "참가자 닉네임", nullable = true, example = "홍길동")
        String nickname,
        @Schema(description = "리더 여부", example = "false")
        boolean isLeader
) {
}
