package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 파티 pending join request 응답")
public record AdminPartyJoinRequestResponse(
        @Schema(description = "요청 ID", example = "request-20260329-001")
        String requestId,
        @Schema(description = "요청자 회원 ID", example = "member-uid")
        String memberId,
        @Schema(description = "요청자 닉네임", example = "김철수", nullable = true)
        String nickname,
        @Schema(description = "요청자 실명", example = "김철수", nullable = true)
        String realname,
        @Schema(description = "요청자 프로필 이미지", example = "https://cdn.skuri.app/profiles/member-1.png", nullable = true)
        String photoUrl,
        @Schema(description = "요청자 학과", example = "컴퓨터공학과", nullable = true)
        String department,
        @Schema(description = "요청자 학번", example = "20201234", nullable = true)
        String studentId,
        @Schema(description = "요청 시각", example = "2026-03-29T16:10:00")
        LocalDateTime requestedAt
) {
}
