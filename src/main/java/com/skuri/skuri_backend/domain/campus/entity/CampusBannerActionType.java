package com.skuri.skuri_backend.domain.campus.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "캠퍼스 배너 액션 타입", example = "IN_APP")
public enum CampusBannerActionType {
    IN_APP,
    EXTERNAL_URL
}
