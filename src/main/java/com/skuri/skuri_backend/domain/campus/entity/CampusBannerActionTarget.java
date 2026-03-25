package com.skuri.skuri_backend.domain.campus.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "캠퍼스 배너 인앱 이동 대상", example = "TAXI_MAIN")
public enum CampusBannerActionTarget {
    TAXI_MAIN,
    NOTICE_MAIN,
    TIMETABLE_DETAIL,
    CAFETERIA_DETAIL,
    ACADEMIC_CALENDAR_DETAIL
}
