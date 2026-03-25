package com.skuri.skuri_backend.domain.campus.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.campus.entity.converter.CampusBannerActionParamsJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "campus_banners",
        indexes = {
                @Index(name = "idx_campus_banners_display_order", columnList = "display_order"),
                @Index(name = "idx_campus_banners_active_display", columnList = "is_active, display_order"),
                @Index(name = "idx_campus_banners_display_window", columnList = "display_start_at, display_end_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampusBanner extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "badge_label", nullable = false, length = 50)
    private String badgeLabel;

    @Column(name = "title_label", nullable = false, length = 100)
    private String titleLabel;

    @Column(name = "description_label", nullable = false, length = 200)
    private String descriptionLabel;

    @Column(name = "button_label", nullable = false, length = 50)
    private String buttonLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "palette_key", nullable = false, length = 20)
    private CampusBannerPaletteKey paletteKey;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private CampusBannerActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_target", length = 40)
    private CampusBannerActionTarget actionTarget;

    @Convert(converter = CampusBannerActionParamsJsonConverter.class)
    @Column(name = "action_params", columnDefinition = "json")
    private JsonNode actionParams;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "display_start_at")
    private LocalDateTime displayStartAt;

    @Column(name = "display_end_at")
    private LocalDateTime displayEndAt;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    private CampusBanner(
            String badgeLabel,
            String titleLabel,
            String descriptionLabel,
            String buttonLabel,
            CampusBannerPaletteKey paletteKey,
            String imageUrl,
            CampusBannerActionType actionType,
            CampusBannerActionTarget actionTarget,
            JsonNode actionParams,
            String actionUrl,
            boolean active,
            LocalDateTime displayStartAt,
            LocalDateTime displayEndAt,
            int displayOrder
    ) {
        this.badgeLabel = badgeLabel;
        this.titleLabel = titleLabel;
        this.descriptionLabel = descriptionLabel;
        this.buttonLabel = buttonLabel;
        this.paletteKey = paletteKey;
        this.imageUrl = imageUrl;
        this.actionType = actionType;
        this.actionTarget = actionTarget;
        this.actionParams = copyJson(actionParams);
        this.actionUrl = actionUrl;
        this.active = active;
        this.displayStartAt = displayStartAt;
        this.displayEndAt = displayEndAt;
        this.displayOrder = displayOrder;
    }

    public static CampusBanner create(
            String badgeLabel,
            String titleLabel,
            String descriptionLabel,
            String buttonLabel,
            CampusBannerPaletteKey paletteKey,
            String imageUrl,
            CampusBannerActionType actionType,
            CampusBannerActionTarget actionTarget,
            JsonNode actionParams,
            String actionUrl,
            boolean active,
            LocalDateTime displayStartAt,
            LocalDateTime displayEndAt,
            int displayOrder
    ) {
        return new CampusBanner(
                badgeLabel,
                titleLabel,
                descriptionLabel,
                buttonLabel,
                paletteKey,
                imageUrl,
                actionType,
                actionTarget,
                actionParams,
                actionUrl,
                active,
                displayStartAt,
                displayEndAt,
                displayOrder
        );
    }

    public void update(
            String badgeLabel,
            String titleLabel,
            String descriptionLabel,
            String buttonLabel,
            CampusBannerPaletteKey paletteKey,
            String imageUrl,
            CampusBannerActionType actionType,
            CampusBannerActionTarget actionTarget,
            JsonNode actionParams,
            String actionUrl,
            boolean active,
            LocalDateTime displayStartAt,
            LocalDateTime displayEndAt
    ) {
        this.badgeLabel = badgeLabel;
        this.titleLabel = titleLabel;
        this.descriptionLabel = descriptionLabel;
        this.buttonLabel = buttonLabel;
        this.paletteKey = paletteKey;
        this.imageUrl = imageUrl;
        this.actionType = actionType;
        this.actionTarget = actionTarget;
        this.actionParams = copyJson(actionParams);
        this.actionUrl = actionUrl;
        this.active = active;
        this.displayStartAt = displayStartAt;
        this.displayEndAt = displayEndAt;
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    private static JsonNode copyJson(JsonNode source) {
        if (source == null || source.isNull()) {
            return null;
        }
        return source.deepCopy();
    }
}
