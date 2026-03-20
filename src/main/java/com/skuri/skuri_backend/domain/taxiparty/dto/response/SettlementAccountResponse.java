package com.skuri.skuri_backend.domain.taxiparty.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "정산 계좌 snapshot 응답")
public record SettlementAccountResponse(
        @Schema(description = "은행명", example = "카카오뱅크")
        String bankName,
        @Schema(description = "계좌번호", example = "3333-01-1234567")
        String accountNumber,
        @Schema(description = "예금주 표시값(hideName=true면 마스킹됨)", example = "홍*동")
        String accountHolder,
        @Schema(description = "이름 일부 숨김 여부", example = "true", nullable = true)
        Boolean hideName
) {
}
