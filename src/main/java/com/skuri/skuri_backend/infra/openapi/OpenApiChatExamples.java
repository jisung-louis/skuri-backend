package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiChatExamples {

    private OpenApiChatExamples() {
    }

    public static final String SUCCESS_CHAT_ROOM_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "room-university",
                  "name": "성결대 전체 채팅방",
                  "type": "UNIVERSITY",
                  "memberCount": 150,
                  "lastMessage": {
                    "type": "TEXT",
                    "text": "안녕하세요!",
                    "senderName": "홍길동",
                    "createdAt": "2026-03-05T21:10:00"
                  },
                  "unreadCount": 5,
                  "isJoined": true
                }
              ]
            }
            """;

    public static final String SUCCESS_CHAT_ROOM_DETAIL = """
            {
              "success": true,
              "data": {
                "id": "room-university",
                "name": "성결대 전체 채팅방",
                "type": "UNIVERSITY",
                "description": "성결대학교 학생들의 소통 공간",
                "isPublic": true,
                "memberCount": 150,
                "isJoined": true,
                "isMuted": false,
                "lastReadAt": "2026-03-05T21:00:00",
                "unreadCount": 5
              }
            }
            """;

    public static final String SUCCESS_CHAT_MESSAGES_PAGE = """
            {
              "success": true,
              "data": {
                "messages": [
                  {
                    "id": "9f9efc3b-4d55-44e7-a86f-93d5101938ec",
                    "chatRoomId": "room-university",
                    "senderId": "dw9rPtuticbjnaYPkeiF3RGPpqk1",
                    "senderName": "스쿠리 유저",
                    "type": "TEXT",
                    "text": "안녕하세요!",
                    "createdAt": "2026-03-05T21:10:00"
                  }
                ],
                "hasNext": true,
                "nextCursor": {
                  "createdAt": "2026-03-05T21:10:00",
                  "id": "9f9efc3b-4d55-44e7-a86f-93d5101938ec"
                }
              }
            }
            """;

    public static final String SUCCESS_CHAT_READ_UPDATE = """
            {
              "success": true,
              "data": {
                "chatRoomId": "room-university",
                "lastReadAt": "2026-03-05T21:10:00",
                "updated": true
              }
            }
            """;

    public static final String SUCCESS_CHAT_SETTINGS_UPDATE = """
            {
              "success": true,
              "data": {
                "chatRoomId": "room-university",
                "muted": true
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_CREATE = """
            {
              "success": true,
              "data": {
                "id": "room:2e8f745a-c131-4e1d-9b8e-7e8d4bb686b3",
                "name": "성결대 전체 채팅방",
                "type": "UNIVERSITY"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_CHAT_ROOM_DELETE = """
            {
              "success": true,
              "data": null
            }
            """;

    public static final String ERROR_CHAT_ROOM_NOT_FOUND = """
            {
              "success": false,
              "errorCode": "CHAT_ROOM_NOT_FOUND",
              "message": "채팅방을 찾을 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_NOT_CHAT_ROOM_MEMBER = """
            {
              "success": false,
              "errorCode": "NOT_CHAT_ROOM_MEMBER",
              "message": "채팅방 멤버가 아닙니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_CHAT_ROOM_FULL = """
            {
              "success": false,
              "errorCode": "CHAT_ROOM_FULL",
              "message": "채팅방 정원이 가득 찼습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ALREADY_CHAT_ROOM_MEMBER = """
            {
              "success": false,
              "errorCode": "ALREADY_CHAT_ROOM_MEMBER",
              "message": "이미 채팅방에 참여 중입니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_BANK_ACCOUNT_REQUIRED = """
            {
              "success": false,
              "errorCode": "BANK_ACCOUNT_REQUIRED",
              "message": "계좌 정보 등록 후 이용 가능합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_STOMP_AUTH_FAILED = """
            {
              "success": false,
              "errorCode": "STOMP_AUTH_FAILED",
              "message": "WebSocket 인증에 실패했습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_VALIDATION_CURSOR_PAIR = """
            {
              "success": false,
              "errorCode": "VALIDATION_ERROR",
              "message": "cursorCreatedAt와 cursorId는 함께 전달해야 합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_PARTY_TYPE_NOT_ALLOWED = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "PARTY 타입 채팅방은 파티 생성 시 자동 생성됩니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_PUBLIC_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "관리자 채팅방 생성 API는 isPublic=true만 허용합니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_DELETE_PARTY_NOT_ALLOWED = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "파티 채팅방은 관리자 API로 삭제할 수 없습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;

    public static final String ERROR_ADMIN_CHAT_ROOM_DELETE_PUBLIC_ONLY = """
            {
              "success": false,
              "errorCode": "INVALID_REQUEST",
              "message": "공개 채팅방만 관리자 API로 삭제할 수 있습니다.",
              "timestamp": "2026-03-05T21:10:00"
            }
            """;
}
