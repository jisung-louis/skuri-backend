package com.skuri.skuri_backend.domain.member.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "linked_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_linked_account_member_provider", columnNames = {"member_id", "provider"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinkedAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkedAccountProvider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(length = 255)
    private String email;

    @Column(name = "provider_display_name", length = 50)
    private String providerDisplayName;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    private LinkedAccount(
            Member member,
            LinkedAccountProvider provider,
            String providerId,
            String email,
            String providerDisplayName,
            String photoUrl
    ) {
        this.member = member;
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.providerDisplayName = providerDisplayName;
        this.photoUrl = photoUrl;
    }

    public static LinkedAccount of(
            Member member,
            LinkedAccountProvider provider,
            String providerId,
            String email,
            String providerDisplayName,
            String photoUrl
    ) {
        return new LinkedAccount(member, provider, providerId, email, providerDisplayName, photoUrl);
    }
}
