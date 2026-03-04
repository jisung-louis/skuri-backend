package com.skuri.skuri_backend.domain.taxiparty.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "party_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartyTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Column(name = "tag", nullable = false, length = 50)
    private String tag;

    private PartyTag(Party party, String tag) {
        this.party = party;
        this.tag = tag;
    }

    public static PartyTag of(Party party, String tag) {
        return new PartyTag(party, tag);
    }
}
