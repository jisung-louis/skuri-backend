package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "ARRIVED 메시지 정산 대상 상태")
public record ChatArrivalSettlementMemberResponse(
        @Schema(description = "회원 ID", example = "member_uuid")
        String memberId,
        @Schema(description = "정산 대상 표시 이름 snapshot", example = "홍길동")
        String displayName,
        @Schema(description = "정산 완료 여부", example = "false")
        boolean settled,
        @Schema(description = "정산 완료 시각", example = "2026-03-03T15:00:00", nullable = true)
        LocalDateTime settledAt,
        @Schema(description = "파티 이탈 여부", example = "true")
        boolean leftParty,
        @Schema(description = "파티 이탈 시각", example = "2026-03-03T15:10:00", nullable = true)
        LocalDateTime leftAt
) {
}
