package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiNotificationExamples {

    private OpenApiNotificationExamples() {
    }

    public static final String SUCCESS_NOTIFICATION_LIST = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "notification-uuid",
                    "type": "PARTY_JOIN_ACCEPTED",
                    "title": "동승 요청이 승인되었어요",
                    "message": "파티에 합류하세요!",
                    "data": {
                      "partyId": "party-uuid",
                      "requestId": "request-uuid"
                    },
                    "isRead": false,
                    "createdAt": "2026-03-08T09:00:00"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false,
                "unreadCount": 5
              }
            }
            """;

    public static final String SUCCESS_NOTIFICATION_UNREAD_COUNT = """
            {
              "success": true,
              "data": {
                "count": 5
              }
            }
            """;

    public static final String SUCCESS_NOTIFICATION_READ = """
            {
              "success": true,
              "data": {
                "id": "notification-uuid",
                "type": "PARTY_JOIN_ACCEPTED",
                "title": "동승 요청이 승인되었어요",
                "message": "파티에 합류하세요!",
                "data": {
                  "partyId": "party-uuid",
                  "requestId": "request-uuid"
                },
                "isRead": true,
                "createdAt": "2026-03-08T09:00:00"
              }
            }
            """;

    public static final String SUCCESS_NOTIFICATION_READ_ALL = """
            {
              "success": true,
              "data": {
                "updatedCount": 3,
                "unreadCount": 0
              }
            }
            """;

    public static final String REQUEST_REGISTER_FCM_TOKEN = """
            {
              "token": "dXZlbnQ6ZmNtLXRva2Vu",
              "platform": "ios",
              "appVersion": "1.4.2"
            }
            """;

    public static final String REQUEST_REGISTER_FCM_TOKEN_WITHOUT_APP_VERSION = """
            {
              "token": "dXZlbnQ6ZmNtLXRva2Vu",
              "platform": "ios"
            }
            """;

    public static final String ERROR_NOTIFICATION_NOT_FOUND =
            "{\"success\":false,\"message\":\"알림을 찾을 수 없습니다.\",\"errorCode\":\"NOTIFICATION_NOT_FOUND\",\"timestamp\":\"2026-03-08T09:00:00\"}";

    public static final String ERROR_NOT_NOTIFICATION_OWNER =
            "{\"success\":false,\"message\":\"본인 알림만 접근할 수 있습니다.\",\"errorCode\":\"NOT_NOTIFICATION_OWNER\",\"timestamp\":\"2026-03-08T09:00:00\"}";

    public static final String SSE_NOTIFICATIONS_STREAM_FULL = """
            event: SNAPSHOT
            data: {"unreadCount":5}

            event: NOTIFICATION
            data: {"id":"notification-uuid","type":"ACADEMIC_SCHEDULE","title":"학사 일정 리마인더","message":"수강신청 일정이 오늘 시작돼요.","data":{"academicScheduleId":"academic-schedule-uuid"},"isRead":false,"createdAt":"2026-03-08T09:00:00"}

            event: UNREAD_COUNT_CHANGED
            data: {"count":6}

            event: HEARTBEAT
            data: {"timestamp":"2026-03-08T09:00:30"}
            """;

    public static final String SSE_NOTIFICATIONS_SNAPSHOT = """
            event: SNAPSHOT
            data: {"unreadCount":5}
            """;

    public static final String SSE_NOTIFICATIONS_NOTIFICATION = """
            event: NOTIFICATION
            data: {"id":"notification-uuid","type":"ACADEMIC_SCHEDULE","title":"학사 일정 리마인더","message":"수강신청 일정이 오늘 시작돼요.","data":{"academicScheduleId":"academic-schedule-uuid"},"isRead":false,"createdAt":"2026-03-08T09:00:00"}
            """;

    public static final String SSE_NOTIFICATIONS_UNREAD_COUNT_CHANGED = """
            event: UNREAD_COUNT_CHANGED
            data: {"count":6}
            """;

    public static final String SSE_NOTIFICATIONS_HEARTBEAT = """
            event: HEARTBEAT
            data: {"timestamp":"2026-03-08T09:00:30"}
            """;
}
