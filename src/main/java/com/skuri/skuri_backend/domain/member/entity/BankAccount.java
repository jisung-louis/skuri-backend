package com.skuri.skuri_backend.domain.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankAccount {

    @Column(name = "bank_name", length = 20)
    private String bankName;

    @Column(name = "account_number", length = 30)
    private String accountNumber;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    @Column(name = "hide_name")
    private Boolean hideName;

    private BankAccount(String bankName, String accountNumber, String accountHolder, Boolean hideName) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.hideName = hideName;
    }

    public static BankAccount of(String bankName, String accountNumber, String accountHolder, Boolean hideName) {
        return new BankAccount(bankName, accountNumber, accountHolder, hideName != null ? hideName : Boolean.FALSE);
    }
}
