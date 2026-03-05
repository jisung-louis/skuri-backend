package com.skuri.skuri_backend.domain.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "ACCOUNT 메시지 계좌 정보")
public record ChatAccountDataResponse(
        @Schema(description = "은행명", example = "카카오뱅크")
        String bankName,
        @Schema(description = "계좌번호", example = "3333-01-1234567")
        String accountNumber,
        @Schema(description = "예금주", example = "홍길동")
        String accountHolder
) {
}
