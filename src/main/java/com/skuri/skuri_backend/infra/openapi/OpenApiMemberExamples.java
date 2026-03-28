package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiMemberExamples {

    private OpenApiMemberExamples() {
    }

    public static final String SUCCESS_MEMBER_CREATE_CREATED = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "studentId": null,
                "department": null,
                "photoUrl": null,
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": null,
                "joinedAt": "2026-03-02T18:37:21"
              }
            }
            """;

    public static final String SUCCESS_MEMBER_CREATE_EXISTING = """
            {
              "success": true,
              "data": {
                "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                "email": "user@sungkyul.ac.kr",
                "nickname": "홍길동",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": null,
                "realname": "홍길동",
                "isAdmin": false,
                "bankAccount": {
                  "bankName": "신한은행",
                  "accountNumber": "110-123-456789",
                  "accountHolder": "홍길동",
                  "hideName": false
                },
                "joinedAt": "2024-03-01T00:00:00"
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
                  "academicScheduleNotifications": true,
                  "academicScheduleDayBeforeEnabled": true,
                  "academicScheduleAllEventsEnabled": false,
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
                "department": "미디어소프트웨어학과",
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
                  "academicScheduleNotifications": true,
                  "academicScheduleDayBeforeEnabled": true,
                  "academicScheduleAllEventsEnabled": false,
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
                  "academicScheduleNotifications": true,
                  "academicScheduleDayBeforeEnabled": true,
                  "academicScheduleAllEventsEnabled": false,
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
                  "academicScheduleNotifications": true,
                  "academicScheduleDayBeforeEnabled": true,
                  "academicScheduleAllEventsEnabled": false,
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

    public static final String SUCCESS_MEMBER_WITHDRAW = """
            {
              "success": true,
              "data": {
                "message": "회원 탈퇴가 완료되었습니다."
              }
            }
            """;

    public static final String SUCCESS_ADMIN_MEMBERS_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "email": "admin@sungkyul.ac.kr",
                    "nickname": "스쿠리 운영자",
                    "realname": "김관리",
                    "studentId": "20190001",
                    "department": "컴퓨터공학과",
                    "isAdmin": true,
                    "joinedAt": "2024-03-01T09:00:00",
                    "lastLogin": "2026-03-29T11:20:00",
                    "status": "ACTIVE"
                  },
                  {
                    "id": "member-2",
                    "email": "user@sungkyul.ac.kr",
                    "nickname": "스쿠리 유저",
                    "realname": "홍길동",
                    "studentId": "2023112233",
                    "department": "경영학과",
                    "isAdmin": false,
                    "joinedAt": "2025-09-01T08:30:00",
                    "lastLogin": "2026-03-28T18:00:00",
                    "status": "ACTIVE"
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 48,
                "totalPages": 3,
                "hasNext": true,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_ADMIN_MEMBER_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "member-2",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "realname": "홍길동",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-2.png",
                "isAdmin": false,
                "status": "ACTIVE",
                "joinedAt": "2025-03-01T09:00:00",
                "lastLogin": "2026-03-29T10:05:00",
                "withdrawnAt": null,
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
                  "academicScheduleNotifications": true,
                  "academicScheduleDayBeforeEnabled": true,
                  "academicScheduleAllEventsEnabled": false,
                  "noticeNotificationsDetail": {
                    "academic": true,
                    "event": false
                  }
                }
              }
            }
            """;

    public static final String SUCCESS_ADMIN_MEMBER_ADMIN_ROLE_UPDATED = """
            {
              "success": true,
              "data": {
                "id": "member-2",
                "email": "user@sungkyul.ac.kr",
                "nickname": "스쿠리 유저",
                "realname": "홍길동",
                "studentId": "2023112233",
                "department": "컴퓨터공학과",
                "photoUrl": "https://cdn.skuri.app/profiles/user-2.png",
                "isAdmin": true,
                "status": "ACTIVE",
                "joinedAt": "2025-03-01T09:00:00",
                "lastLogin": "2026-03-29T10:05:00",
                "withdrawnAt": null,
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
                  "academicScheduleNotifications": true,
                  "academicScheduleDayBeforeEnabled": true,
                  "academicScheduleAllEventsEnabled": false,
                  "noticeNotificationsDetail": {
                    "academic": true,
                    "event": false
                  }
                }
              }
            }
            """;

    public static final String ERROR_MEMBER_NOT_FOUND =
            "{\"success\":false,\"message\":\"회원을 찾을 수 없습니다.\",\"errorCode\":\"MEMBER_NOT_FOUND\",\"timestamp\":\"2026-03-04T12:00:00\"}";
    public static final String ERROR_MEMBER_WITHDRAWN =
            "{\"success\":false,\"message\":\"탈퇴한 회원은 서비스에 접근할 수 없습니다.\",\"errorCode\":\"MEMBER_WITHDRAWN\",\"timestamp\":\"2026-03-09T12:00:00\"}";
    public static final String ERROR_MEMBER_WITHDRAWAL_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"현재 상태에서는 회원 탈퇴를 진행할 수 없습니다.\",\"errorCode\":\"MEMBER_WITHDRAWAL_NOT_ALLOWED\",\"timestamp\":\"2026-03-09T12:00:00\"}";
    public static final String ERROR_WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"탈퇴한 계정은 같은 인증 계정으로 재가입할 수 없습니다.\",\"errorCode\":\"WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED\",\"timestamp\":\"2026-03-09T12:00:00\"}";
    public static final String ERROR_MEMBER_ADMIN_ROLE_CHANGE_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"탈퇴한 회원의 관리자 권한은 변경할 수 없습니다.\",\"errorCode\":\"CONFLICT\",\"timestamp\":\"2026-03-29T12:00:00\"}";
}
