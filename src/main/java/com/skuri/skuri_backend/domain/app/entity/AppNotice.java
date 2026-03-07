package com.skuri.skuri_backend.domain.app.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.app.entity.converter.StringListJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "app_notices")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppNotice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppNoticeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppNoticePriority priority;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "image_urls", columnDefinition = "json")
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    private AppNotice(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            LocalDateTime publishedAt
    ) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.priority = priority;
        this.imageUrls = new ArrayList<>(imageUrls == null ? List.of() : imageUrls);
        this.actionUrl = actionUrl;
        this.publishedAt = publishedAt;
    }

    public static AppNotice create(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            LocalDateTime publishedAt
    ) {
        return new AppNotice(title, content, category, priority, imageUrls, actionUrl, publishedAt);
    }

    public void update(
            String title,
            String content,
            AppNoticeCategory category,
            AppNoticePriority priority,
            List<String> imageUrls,
            String actionUrl,
            LocalDateTime publishedAt
    ) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
        if (priority != null) {
            this.priority = priority;
        }
        if (imageUrls != null) {
            this.imageUrls = new ArrayList<>(imageUrls);
        }
        if (actionUrl != null) {
            this.actionUrl = actionUrl;
        }
        if (publishedAt != null) {
            this.publishedAt = publishedAt;
        }
    }
}
