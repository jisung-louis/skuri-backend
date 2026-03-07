package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiMemberExamples {

    private OpenApiMemberExamples() {
    }

    public static final String SUCCESS_MEMBER_CREATE = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": {
                  "bankName": "신한은행",
                  "accountNumber": "110-123-456789",
                  "accountHolder": "홍길동",
                  "hideName": false
                },
                "joinedAt": "2026-03-02T18:37:21"
              }
            }
            """;

    public static final String SUCCESS_MEMBER_ME = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": {
                  "bankName": "신한은행",
                  "accountNumber": "110-123-456789",
                  "accountHolder": "홍길동",
                  "hideName": false
                },
                "notificationSetting": {
                  "allNotifications": true,
                  "partyNotifications": true,
                  "noticeNotifications": true,
                  "boardLikeNotifications": false,
                  "commentNotifications": true,
                  "bookmarkedPostCommentNotifications": true,
                  "systemNotifications": true,
                  "noticeNotificationsDetail": {
                    "academic": true,
                    "event": false
                  }
                },
                "joinedAt": "2026-03-02T18:37:21",
                "lastLogin": "2026-03-04T10:00:00"
              }
            }
            """;

    public static final String SUCCESS_MEMBER_ME_UPDATED = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리유저_수정",
                "studentId": "2023112233",
                "department": "소프트웨어학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-1-new.png",
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": {
                  "bankName": "신한은행",
                  "accountNumber": "110-123-456789",
                  "accountHolder": "홍길동",
                  "hideName": false
                },
                "notificationSetting": {
                  "allNotifications": true,
                  "partyNotifications": true,
                  "noticeNotifications": true,
                  "boardLikeNotifications": false,
                  "commentNotifications": true,
                  "bookmarkedPostCommentNotifications": true,
                  "systemNotifications": true,
                  "noticeNotificationsDetail": {
                    "academic": true,
                    "event": false
                  }
                },
                "joinedAt": "2026-03-02T18:37:21",
                "lastLogin": "2026-03-04T10:00:00"
              }
            }
            """;

    public static final String SUCCESS_MEMBER_ME_BANK_UPDATED = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": {
                  "bankName": "카카오뱅크",
                  "accountNumber": "3333-12-3456789",
                  "accountHolder": "홍길동",
                  "hideName": true
                },
                "notificationSetting": {
                  "allNotifications": true,
                  "partyNotifications": true,
                  "noticeNotifications": true,
                  "boardLikeNotifications": false,
                  "commentNotifications": true,
                  "bookmarkedPostCommentNotifications": true,
                  "systemNotifications": true,
                  "noticeNotificationsDetail": {
                    "academic": true,
                    "event": false
                  }
                },
                "joinedAt": "2026-03-02T18:37:21",
                "lastLogin": "2026-03-04T10:00:00"
              }
            }
            """;

    public static final String SUCCESS_MEMBER_ME_NOTIFICATION_UPDATED = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-1.png",
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": {
                  "bankName": "신한은행",
                  "accountNumber": "110-123-456789",
                  "accountHolder": "홍길동",
                  "hideName": false
                },
                "notificationSetting": {
                  "allNotifications": true,
                  "partyNotifications": true,
                  "noticeNotifications": true,
                  "boardLikeNotifications": true,
                  "commentNotifications": true,
                  "bookmarkedPostCommentNotifications": true,
                  "systemNotifications": true,
                  "noticeNotificationsDetail": {
                    "academic": true,
                    "event": true
                  }
                },
                "joinedAt": "2026-03-02T18:37:21",
                "lastLogin": "2026-03-04T10:00:00"
              }
            }
            """;

    public static final String SUCCESS_MEMBER_PUBLIC_PROFILE = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "nickname": "스쿠리 유저",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-1.png"
              }
            }
            """;

    public static final String ERROR_MEMBER_NOT_FOUND =
            "{\"success\":false,\"message\":\"회원을 찾을 수 없습니다.\",\"errorCode\":\"MEMBER_NOT_FOUND\",\"timestamp\":\"2026-03-04T12:00:00\"}";
}
