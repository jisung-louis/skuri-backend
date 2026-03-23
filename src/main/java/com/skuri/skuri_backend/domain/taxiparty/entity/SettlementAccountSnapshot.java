package com.skuri.skuri_backend.domain.taxiparty.entity;

import com.skuri.skuri_backend.common.util.AccountHolderMasker;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementAccountSnapshot {

    @Column(name = "settlement_bank_name", length = 20)
    private String bankName;

    @Column(name = "settlement_account_number", length = 30)
    private String accountNumber;

    @Column(name = "settlement_account_holder", length = 50)
    private String accountHolder;

    @Column(name = "settlement_hide_name")
    private Boolean hideName;

    private SettlementAccountSnapshot(String bankName, String accountNumber, String accountHolder, Boolean hideName) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.hideName = hideName != null ? hideName : Boolean.FALSE;
    }

    public static SettlementAccountSnapshot of(String bankName, String accountNumber, String accountHolder, Boolean hideName) {
        return new SettlementAccountSnapshot(bankName, accountNumber, accountHolder, hideName);
    }

    public boolean isComplete() {
        return StringUtils.hasText(bankName)
                && StringUtils.hasText(accountNumber)
                && StringUtils.hasText(accountHolder);
    }

    public String getDisplayAccountHolder() {
        return AccountHolderMasker.mask(accountHolder, hideName);
    }
}
