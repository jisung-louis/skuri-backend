package com.skuri.skuri_backend.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "fcm_tokens",
        indexes = {
                @Index(name = "idx_fcm_tokens_user", columnList = "user_id"),
                @Index(name = "idx_fcm_tokens_token", columnList = "token", unique = true)
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 500, unique = true)
    private String token;

    @Column(nullable = false, length = 10)
    private String platform;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    private FcmToken(String userId, String token, String platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.lastUsedAt = LocalDateTime.now();
    }

    public static FcmToken create(String userId, String token, String platform) {
        return new FcmToken(userId, token, platform);
    }

    public void registerTo(String userId, String platform) {
        this.userId = userId;
        this.platform = platform;
        touch();
    }

    public void touch() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public boolean belongsTo(String memberId) {
        return userId.equals(memberId);
    }
}
