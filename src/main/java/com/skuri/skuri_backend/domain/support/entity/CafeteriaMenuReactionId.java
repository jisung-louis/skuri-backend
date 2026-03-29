package com.skuri.skuri_backend.domain.support.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CafeteriaMenuReactionId implements Serializable {

    @Column(name = "member_id", length = 36)
    private String memberId;

    @Column(name = "menu_id", length = 255)
    private String menuId;

    private CafeteriaMenuReactionId(String memberId, String menuId) {
        this.memberId = memberId;
        this.menuId = menuId;
    }

    public static CafeteriaMenuReactionId of(String memberId, String menuId) {
        return new CafeteriaMenuReactionId(memberId, menuId);
    }
}
