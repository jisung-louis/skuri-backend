package com.skuri.skuri_backend.domain.member.dto.response;

public record MemberBankAccountResponse(
        String bankName,
        String accountNumber,
        String accountHolder,
        Boolean hideName
) {
}
