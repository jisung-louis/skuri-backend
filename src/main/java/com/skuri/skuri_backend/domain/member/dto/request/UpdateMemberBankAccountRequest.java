package com.skuri.skuri_backend.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "내 계좌 정보 수정 요청")
public record UpdateMemberBankAccountRequest(
        @Schema(description = "은행명", example = "신한은행")
        @NotBlank(message = "bankName은 필수입니다.")
        @Size(max = 20, message = "bankName은 20자 이하여야 합니다.")
        String bankName,

        @Schema(description = "계좌번호", example = "110-123-456789")
        @NotBlank(message = "accountNumber는 필수입니다.")
        @Size(max = 30, message = "accountNumber는 30자 이하여야 합니다.")
        String accountNumber,

        @Schema(description = "예금주", example = "홍길동")
        @NotBlank(message = "accountHolder는 필수입니다.")
        @Size(max = 50, message = "accountHolder는 50자 이하여야 합니다.")
        String accountHolder,

        @Schema(description = "계좌 소유자명 숨김 여부 (null이면 false)", example = "false", nullable = true)
        Boolean hideName
) {
}
