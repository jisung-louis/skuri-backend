package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiAppExamples {

    private OpenApiAppExamples() {
    }

    public static final String SUCCESS_APP_VERSION = """
            {
              "success": true,
              "data": {
                "platform": "ios",
                "minVersion": "1.4.0",
                "latestVersion": "1.6.1",
                "forceUpdate": false
              }
            }
            """;

    public static final String SUCCESS_APP_NOTICES = """
            {
              "success": true,
              "data": [
                {
                  "id": "notice-2026-spring-01",
                  "title": "개강 주간 택시 이벤트",
                  "content": "첫 주 택시파티 이용 시 포인트 2배 적립",
                  "category": "event",
                  "publishedAt": "2026-03-02T09:00:00"
                }
              ]
            }
            """;
}
