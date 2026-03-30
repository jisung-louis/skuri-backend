package com.skuri.skuri_backend.domain.minecraft.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "minecraft_accounts",
        indexes = {
                @Index(name = "idx_minecraft_accounts_owner", columnList = "owner_member_id"),
                @Index(name = "idx_minecraft_accounts_parent", columnList = "parent_account_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MinecraftAccount extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "owner_member_id", nullable = false, length = 36)
    private String ownerMemberId;

    @Column(name = "parent_account_id", length = 36)
    private String parentAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", nullable = false, length = 20)
    private MinecraftAccountRole accountRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MinecraftEdition edition;

    @Column(name = "game_name", nullable = false, length = 50)
    private String gameName;

    @Column(name = "stored_name", length = 50)
    private String storedName;

    @Column(name = "normalized_key", nullable = false, unique = true, length = 64)
    private String normalizedKey;

    @Column(name = "avatar_uuid", nullable = false, length = 64)
    private String avatarUuid;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    private MinecraftAccount(
            String ownerMemberId,
            String parentAccountId,
            MinecraftAccountRole accountRole,
            MinecraftEdition edition,
            String gameName,
            String storedName,
            String normalizedKey,
            String avatarUuid
    ) {
        this.id = UUID.randomUUID().toString();
        this.ownerMemberId = ownerMemberId;
        this.parentAccountId = parentAccountId;
        this.accountRole = accountRole;
        this.edition = edition;
        this.gameName = gameName;
        this.storedName = storedName;
        this.normalizedKey = normalizedKey;
        this.avatarUuid = avatarUuid;
    }

    public static MinecraftAccount create(
            String ownerMemberId,
            String parentAccountId,
            MinecraftAccountRole accountRole,
            MinecraftEdition edition,
            String gameName,
            String storedName,
            String normalizedKey,
            String avatarUuid
    ) {
        return new MinecraftAccount(
                ownerMemberId,
                parentAccountId,
                accountRole,
                edition,
                gameName,
                storedName,
                normalizedKey,
                avatarUuid
        );
    }

    public void updateLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
