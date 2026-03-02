package com.skuri.skuri_backend.domain.member.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 계좌 정보")
public record MemberBankAccountResponse(
        @Schema(description = "은행명", example = "신한은행", nullable = true)
        String bankName,
        @Schema(description = "계좌번호", example = "110-123-456789", nullable = true)
        String accountNumber,
        @Schema(description = "예금주", example = "홍길동", nullable = true)
        String accountHolder,
        @Schema(description = "계좌 소유자명 숨김 여부", example = "false", nullable = true)
        Boolean hideName
) {
}
