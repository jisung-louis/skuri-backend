package com.skuri.skuri_backend.domain.support.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "app_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class AppVersion {

    @Id
    @Column(length = 10)
    private String platform;

    @Column(name = "minimum_version", nullable = false, length = 20)
    private String minimumVersion;

    @Column(name = "force_update", nullable = false)
    private boolean forceUpdate;

    @Column(length = 500)
    private String message;

    @Column(length = 100)
    private String title;

    @Column(name = "show_button", nullable = false)
    private boolean showButton;

    @Column(name = "button_text", length = 100)
    private String buttonText;

    @Column(name = "button_url", length = 500)
    private String buttonUrl;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private AppVersion(
            String platform,
            String minimumVersion,
            boolean forceUpdate,
            String message,
            String title,
            boolean showButton,
            String buttonText,
            String buttonUrl
    ) {
        this.platform = platform;
        this.minimumVersion = minimumVersion;
        this.forceUpdate = forceUpdate;
        this.message = message;
        this.title = title;
        this.showButton = showButton;
        this.buttonText = buttonText;
        this.buttonUrl = buttonUrl;
    }

    public static AppVersion create(
            String platform,
            String minimumVersion,
            boolean forceUpdate,
            String message,
            String title,
            boolean showButton,
            String buttonText,
            String buttonUrl
    ) {
        return new AppVersion(platform, minimumVersion, forceUpdate, message, title, showButton, buttonText, buttonUrl);
    }

    public void update(
            String minimumVersion,
            boolean forceUpdate,
            String message,
            String title,
            boolean showButton,
            String buttonText,
            String buttonUrl
    ) {
        this.minimumVersion = minimumVersion;
        this.forceUpdate = forceUpdate;
        this.message = message;
        this.title = title;
        this.showButton = showButton;
        this.buttonText = buttonText;
        this.buttonUrl = buttonUrl;
    }

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }
}
