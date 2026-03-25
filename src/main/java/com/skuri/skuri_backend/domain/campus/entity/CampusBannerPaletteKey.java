package com.skuri.skuri_backend.domain.campus.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "캠퍼스 배너 팔레트 키", example = "GREEN")
public enum CampusBannerPaletteKey {
    GREEN,
    BLUE,
    PURPLE,
    RED,
    YELLOW
}
