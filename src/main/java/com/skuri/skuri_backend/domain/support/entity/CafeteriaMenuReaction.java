package com.skuri.skuri_backend.domain.support.entity;

import com.skuri.skuri_backend.common.entity.BaseTimeEntity;
import com.skuri.skuri_backend.domain.support.model.CafeteriaMenuReactionType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "cafeteria_menu_reactions",
        indexes = {
                @Index(name = "idx_cafeteria_menu_reactions_week_id", columnList = "week_id"),
                @Index(name = "idx_cafeteria_menu_reactions_week_menu_id", columnList = "week_id,menu_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CafeteriaMenuReaction extends BaseTimeEntity {

    @EmbeddedId
    private CafeteriaMenuReactionId id;

    @Column(name = "week_id", nullable = false, length = 20)
    private String weekId;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", nullable = false, length = 20)
    private CafeteriaMenuReactionType reaction;

    private CafeteriaMenuReaction(
            String memberId,
            String menuId,
            String weekId,
            String category,
            String title,
            CafeteriaMenuReactionType reaction
    ) {
        this.id = CafeteriaMenuReactionId.of(memberId, menuId);
        this.weekId = weekId;
        this.category = category;
        this.title = title;
        this.reaction = reaction;
    }

    public static CafeteriaMenuReaction create(
            String memberId,
            String menuId,
            String weekId,
            String category,
            String title,
            CafeteriaMenuReactionType reaction
    ) {
        return new CafeteriaMenuReaction(memberId, menuId, weekId, category, title, reaction);
    }

    public void updateReaction(CafeteriaMenuReactionType reaction) {
        this.reaction = reaction;
    }
}
