package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "멤버 정산 확인 응답")
public record SettlementConfirmResponse(
        @Schema(description = "회원 ID", example = "member_uuid")
        String memberId,
        @Schema(description = "정산 완료 여부", example = "true")
        boolean settled,
        @Schema(description = "정산 완료 시각", example = "2026-03-03T14:30:00")
        LocalDateTime settledAt,
        @Schema(description = "전체 정산 완료 여부", example = "true")
        boolean allSettled
) {
}
