package com.skuri.skuri_backend.domain.taxiparty.entity;

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
@Table(name = "party_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyMember {

    @EmbeddedId
    private PartyMemberId id;

    @MapsId("partyId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    private PartyMember(Party party, String memberId, LocalDateTime joinedAt) {
        this.id = PartyMemberId.of(null, memberId);
        this.party = party;
        this.joinedAt = joinedAt;
    }

    public static PartyMember create(Party party, String memberId, LocalDateTime joinedAt) {
        return new PartyMember(party, memberId, joinedAt);
    }

    public String getMemberId() {
        return id.getMemberId();
    }
}
