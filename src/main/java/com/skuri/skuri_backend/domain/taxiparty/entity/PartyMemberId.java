package com.skuri.skuri_backend.domain.taxiparty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMemberId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "party_id", length = 36)
    private String partyId;

    @Column(name = "member_id", length = 36)
    private String memberId;

    private PartyMemberId(String partyId, String memberId) {
        this.partyId = partyId;
        this.memberId = memberId;
    }

    public static PartyMemberId of(String partyId, String memberId) {
        return new PartyMemberId(partyId, memberId);
    }
}
