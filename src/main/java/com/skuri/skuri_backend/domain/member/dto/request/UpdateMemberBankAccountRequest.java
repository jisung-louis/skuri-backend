package com.skuri.skuri_backend.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberBankAccountRequest(
        @NotBlank(message = "bankName은 필수입니다.")
        @Size(max = 20, message = "bankName은 20자 이하여야 합니다.")
        String bankName,

        @NotBlank(message = "accountNumber는 필수입니다.")
        @Size(max = 30, message = "accountNumber는 30자 이하여야 합니다.")
        String accountNumber,

        @NotBlank(message = "accountHolder는 필수입니다.")
        @Size(max = 50, message = "accountHolder는 50자 이하여야 합니다.")
        String accountHolder,

        Boolean hideName
) {
}
