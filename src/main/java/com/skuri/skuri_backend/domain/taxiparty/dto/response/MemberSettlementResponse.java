package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "멤버 정산 상태 응답")
public record MemberSettlementResponse(
        @Schema(description = "회원 ID", example = "member_uuid")
        String memberId,
        @Schema(description = "회원 이름", example = "홍길동")
        String memberName,
        @Schema(description = "정산 완료 여부", example = "false")
        boolean settled,
        @Schema(description = "정산 완료 시각", example = "2026-03-03T15:00:00", nullable = true)
        LocalDateTime settledAt
) {
}
