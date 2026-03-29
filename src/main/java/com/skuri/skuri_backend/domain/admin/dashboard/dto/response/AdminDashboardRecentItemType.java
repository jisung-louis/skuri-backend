package com.skuri.skuri_backend.domain.admin.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "대시보드 최근 항목 타입", allowableValues = {"INQUIRY", "REPORT", "APP_NOTICE", "PARTY"})
public enum AdminDashboardRecentItemType {
    INQUIRY,
    REPORT,
    APP_NOTICE,
    PARTY
}
