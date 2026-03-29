package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiDashboardExamples {

    private OpenApiDashboardExamples() {
    }

    public static final String SUCCESS_ADMIN_DASHBOARD_SUMMARY = """
            {
              "success": true,
              "data": {
                "newMembersToday": 12,
                "totalMembers": 4831,
                "adminCount": 4,
                "openPartyCount": 17,
                "pendingInquiryCount": 9,
                "pendingReportCount": 3,
                "generatedAt": "2026-03-29T18:00:00"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_DASHBOARD_ACTIVITY = """
            {
              "success": true,
              "data": {
                "days": 7,
                "timezone": "Asia/Seoul",
                "series": [
                  {
                    "date": "2026-03-23",
                    "newMembers": 4,
                    "inquiriesCreated": 2,
                    "reportsCreated": 1,
                    "partiesCreated": 6
                  },
                  {
                    "date": "2026-03-24",
                    "newMembers": 7,
                    "inquiriesCreated": 1,
                    "reportsCreated": 0,
                    "partiesCreated": 3
                  }
                ]
              }
            }
            """;

    public static final String SUCCESS_ADMIN_DASHBOARD_RECENT_ITEMS = """
            {
              "success": true,
              "data": [
                {
                  "type": "INQUIRY",
                  "id": "inquiry-1",
                  "title": "계정 문의",
                  "subtitle": "PENDING · member-1",
                  "status": "PENDING",
                  "createdAt": "2026-03-29T17:00:00"
                },
                {
                  "type": "REPORT",
                  "id": "report-1",
                  "title": "게시글 신고",
                  "subtitle": "PENDING · POST",
                  "status": "PENDING",
                  "createdAt": "2026-03-29T16:50:00"
                },
                {
                  "type": "APP_NOTICE",
                  "id": "notice-1",
                  "title": "긴급 점검 안내",
                  "subtitle": "HIGH",
                  "status": "PUBLISHED",
                  "createdAt": "2026-03-29T16:30:00"
                },
                {
                  "type": "PARTY",
                  "id": "party-1",
                  "title": "성결대학교 -> 안양역",
                  "subtitle": "OPEN · leader-1",
                  "status": "OPEN",
                  "createdAt": "2026-03-29T16:10:00"
                }
              ]
            }
            """;

    public static final String ERROR_ADMIN_DASHBOARD_INVALID_DAYS =
            "{\"success\":false,\"message\":\"days는 7 또는 30만 허용합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T18:00:00\"}";

    public static final String ERROR_ADMIN_DASHBOARD_INVALID_LIMIT =
            "{\"success\":false,\"message\":\"limit는 1 이상 30 이하여야 합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T18:00:00\"}";
}
