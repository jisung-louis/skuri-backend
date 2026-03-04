package com.skuri.skuri_backend.domain.taxiparty.entity;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "member_settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSettlement {

    @EmbeddedId
    private MemberSettlementId id;

    @MapsId("partyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "settled", nullable = false)
    private boolean settled;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    private MemberSettlement(Party party, String memberId) {
        this.id = MemberSettlementId.of(null, memberId);
        this.party = party;
        this.settled = false;
    }

    public static MemberSettlement create(Party party, String memberId) {
        return new MemberSettlement(party, memberId);
    }

    public void markSettled() {
        if (this.settled) {
            throw new BusinessException(ErrorCode.ALREADY_SETTLED);
        }
        this.settled = true;
        this.settledAt = LocalDateTime.now();
    }

    public String getMemberId() {
        return id.getMemberId();
    }
}
