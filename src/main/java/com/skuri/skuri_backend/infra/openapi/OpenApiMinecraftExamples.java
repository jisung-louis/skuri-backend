package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiMinecraftExamples {

    private OpenApiMinecraftExamples() {
    }

    public static final String SUCCESS_MINECRAFT_OVERVIEW = """
            {
              "success": true,
              "data": {
                "chatRoomId": "public:game:minecraft",
                "online": true,
                "currentPlayers": 12,
                "maxPlayers": 50,
                "version": "1.21.1",
                "serverAddress": "mc.skuri.app",
                "mapUrl": "https://map.skuri.app",
                "lastHeartbeatAt": "2026-03-30T13:20:00Z"
              }
            }
            """;

    public static final String SUCCESS_MINECRAFT_PLAYERS = """
            {
              "success": true,
              "data": [
                {
                  "accountId": "account-uuid",
                  "ownerMemberId": "member-1",
                  "accountRole": "SELF",
                  "edition": "JAVA",
                  "gameName": "skuriPlayer",
                  "normalizedKey": "8667ba71b85a4004af54457a9734eed7",
                  "avatarUuid": "8667ba71b85a4004af54457a9734eed7",
                  "parentGameName": null,
                  "online": true,
                  "lastSeenAt": "2026-03-30T13:18:00Z"
                }
              ]
            }
            """;

    public static final String SUCCESS_MY_MINECRAFT_ACCOUNTS = """
            {
              "success": true,
              "data": [
                {
                  "id": "account-uuid",
                  "accountRole": "SELF",
                  "edition": "JAVA",
                  "gameName": "skuriPlayer",
                  "normalizedKey": "8667ba71b85a4004af54457a9734eed7",
                  "avatarUuid": "8667ba71b85a4004af54457a9734eed7",
                  "storedName": null,
                  "parentAccountId": null,
                  "parentGameName": null,
                  "lastSeenAt": "2026-03-30T13:18:00Z",
                  "linkedAt": "2026-03-30T13:00:00Z"
                }
              ]
            }
            """;

    public static final String REQUEST_CREATE_MINECRAFT_ACCOUNT = """
            {
              "edition": "JAVA",
              "accountRole": "SELF",
              "gameName": "skuriPlayer"
            }
            """;

    public static final String SUCCESS_CREATE_MINECRAFT_ACCOUNT = """
            {
              "success": true,
              "data": {
                "id": "account-uuid",
                "accountRole": "SELF",
                "edition": "JAVA",
                "gameName": "skuriPlayer",
                "normalizedKey": "8667ba71b85a4004af54457a9734eed7",
                "avatarUuid": "8667ba71b85a4004af54457a9734eed7",
                "storedName": null,
                "parentAccountId": null,
                "parentGameName": null,
                "lastSeenAt": null,
                "linkedAt": "2026-03-30T13:00:00Z"
              }
            }
            """;

    public static final String SUCCESS_DELETE_MINECRAFT_ACCOUNT = SUCCESS_CREATE_MINECRAFT_ACCOUNT;

    public static final String ERROR_MINECRAFT_ACCOUNT_LIMIT_EXCEEDED =
            "{\"success\":false,\"message\":\"등록 가능한 마인크래프트 계정 수를 초과했습니다.\",\"errorCode\":\"MINECRAFT_ACCOUNT_LIMIT_EXCEEDED\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS =
            "{\"success\":false,\"message\":\"본인 마인크래프트 계정은 1개만 등록할 수 있습니다.\",\"errorCode\":\"MINECRAFT_SELF_ACCOUNT_ALREADY_EXISTS\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_FRIEND_ACCOUNT_LIMIT_EXCEEDED =
            "{\"success\":false,\"message\":\"친구 마인크래프트 계정은 최대 3개까지만 등록할 수 있습니다.\",\"errorCode\":\"MINECRAFT_FRIEND_ACCOUNT_LIMIT_EXCEEDED\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_PARENT_ACCOUNT_REQUIRED =
            "{\"success\":false,\"message\":\"친구 계정을 등록하려면 본인 계정을 먼저 등록해야 합니다.\",\"errorCode\":\"MINECRAFT_PARENT_ACCOUNT_REQUIRED\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED =
            "{\"success\":false,\"message\":\"친구 계정이 연결된 본인 계정은 삭제할 수 없습니다.\",\"errorCode\":\"MINECRAFT_PARENT_ACCOUNT_DELETE_NOT_ALLOWED\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_ACCOUNT_DUPLICATED =
            "{\"success\":false,\"message\":\"이미 등록된 마인크래프트 계정입니다.\",\"errorCode\":\"MINECRAFT_ACCOUNT_DUPLICATED\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_SECRET_INVALID =
            "{\"success\":false,\"message\":\"마인크래프트 플러그인 shared secret이 올바르지 않습니다.\",\"errorCode\":\"MINECRAFT_SECRET_INVALID\",\"timestamp\":\"2026-03-30T13:20:00\"}";
    public static final String ERROR_MINECRAFT_SERVER_UNAVAILABLE =
            "{\"success\":false,\"message\":\"마인크래프트 서버 상태를 아직 확인할 수 없습니다.\",\"errorCode\":\"MINECRAFT_SERVER_UNAVAILABLE\",\"timestamp\":\"2026-03-30T13:20:00\"}";

    public static final String SSE_MINECRAFT_STREAM_FULL = """
            event: SERVER_STATE_SNAPSHOT
            data: {"chatRoomId":"public:game:minecraft","online":true,"currentPlayers":12,"maxPlayers":50,"version":"1.21.1","serverAddress":"mc.skuri.app","mapUrl":"https://map.skuri.app","lastHeartbeatAt":"2026-03-30T13:20:00Z"}

            event: PLAYERS_SNAPSHOT
            data: {"players":[{"accountId":"account-uuid","ownerMemberId":"member-1","accountRole":"SELF","edition":"JAVA","gameName":"skuriPlayer","normalizedKey":"8667ba71b85a4004af54457a9734eed7","avatarUuid":"8667ba71b85a4004af54457a9734eed7","parentGameName":null,"online":true,"lastSeenAt":"2026-03-30T13:18:00Z"}]}

            event: HEARTBEAT
            data: {"timestamp":"2026-03-30T13:20:30Z"}
            """;

    public static final String SSE_MINECRAFT_SERVER_STATE_SNAPSHOT = """
            event: SERVER_STATE_SNAPSHOT
            data: {"chatRoomId":"public:game:minecraft","online":true,"currentPlayers":12,"maxPlayers":50,"version":"1.21.1","serverAddress":"mc.skuri.app","mapUrl":"https://map.skuri.app","lastHeartbeatAt":"2026-03-30T13:20:00Z"}
            """;

    public static final String SSE_MINECRAFT_PLAYERS_SNAPSHOT = """
            event: PLAYERS_SNAPSHOT
            data: {"players":[{"accountId":"account-uuid","ownerMemberId":"member-1","accountRole":"SELF","edition":"JAVA","gameName":"skuriPlayer","normalizedKey":"8667ba71b85a4004af54457a9734eed7","avatarUuid":"8667ba71b85a4004af54457a9734eed7","parentGameName":null,"online":true,"lastSeenAt":"2026-03-30T13:18:00Z"}]}
            """;

    public static final String SSE_MINECRAFT_HEARTBEAT = """
            event: HEARTBEAT
            data: {"timestamp":"2026-03-30T13:20:30Z"}
            """;

    public static final String REQUEST_INTERNAL_CHAT_MESSAGE = """
            {
              "eventId": "9fa37c63-2c5a-4d1d-8a28-55b72750e79d",
              "eventType": "CHAT",
              "systemType": null,
              "senderName": "skuriPlayer",
              "minecraftUuid": "8667ba71b85a4004af54457a9734eed7",
              "edition": "JAVA",
              "text": "안녕하세요!",
              "occurredAt": "2026-03-30T13:20:00Z"
            }
            """;

    public static final String REQUEST_INTERNAL_SERVER_STATE = """
            {
              "online": true,
              "currentPlayers": 12,
              "maxPlayers": 50,
              "version": "1.21.1",
              "serverAddress": "mc.skuri.app",
              "mapUrl": "https://map.skuri.app",
              "heartbeatAt": "2026-03-30T13:20:00Z"
            }
            """;

    public static final String REQUEST_INTERNAL_ONLINE_PLAYERS = """
            {
              "capturedAt": "2026-03-30T13:20:00Z",
              "players": [
                {
                  "gameName": "skuriPlayer",
                  "edition": "JAVA",
                  "minecraftUuid": "8667ba71b85a4004af54457a9734eed7"
                }
              ]
            }
            """;

    public static final String SSE_INTERNAL_MINECRAFT_STREAM_FULL = """
            event: WHITELIST_SNAPSHOT
            data: {"players":[{"accountId":"account-uuid","normalizedKey":"8667ba71b85a4004af54457a9734eed7","edition":"JAVA","gameName":"skuriPlayer","avatarUuid":"8667ba71b85a4004af54457a9734eed7","storedName":null}]}

            event: CHAT_FROM_APP
            id: 8c6a60c5-cc35-4e52-9afc-1a6d1fbcdb0d
            data: {"messageId":"dfd5b4b1-54ea-4fa1-92d9-b61a931d0d56","chatRoomId":"public:game:minecraft","senderName":"홍길동","type":"IMAGE","text":"홍길동님이 사진을 보냈습니다."}

            event: HEARTBEAT
            data: {"timestamp":"2026-03-30T13:20:30Z"}
            """;

    public static final String SSE_INTERNAL_MINECRAFT_WHITELIST_SNAPSHOT = """
            event: WHITELIST_SNAPSHOT
            data: {"players":[{"accountId":"account-uuid","normalizedKey":"8667ba71b85a4004af54457a9734eed7","edition":"JAVA","gameName":"skuriPlayer","avatarUuid":"8667ba71b85a4004af54457a9734eed7","storedName":null}]}
            """;

    public static final String SSE_INTERNAL_MINECRAFT_CHAT_FROM_APP = """
            event: CHAT_FROM_APP
            id: 8c6a60c5-cc35-4e52-9afc-1a6d1fbcdb0d
            data: {"messageId":"dfd5b4b1-54ea-4fa1-92d9-b61a931d0d56","chatRoomId":"public:game:minecraft","senderName":"홍길동","type":"IMAGE","text":"홍길동님이 사진을 보냈습니다."}
            """;

    public static final String SSE_INTERNAL_MINECRAFT_HEARTBEAT = """
            event: HEARTBEAT
            data: {"timestamp":"2026-03-30T13:20:30Z"}
            """;
}
