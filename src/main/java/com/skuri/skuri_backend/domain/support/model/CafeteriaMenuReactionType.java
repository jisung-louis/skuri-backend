package com.skuri.skuri_backend.domain.support.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "학식 메뉴 반응 타입", enumAsRef = true)
public enum CafeteriaMenuReactionType {
    LIKE,
    DISLIKE
}
