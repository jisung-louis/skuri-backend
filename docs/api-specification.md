# Spring 백엔드 API 명세

> 최종 수정일: 2026-02-03
> 관련 문서: [도메인 분석](./domain-analysis.md) | [ERD](./erd.md)

---

## 목차

1. [공통 사항](#1-공통-사항)
2. [Member API](#2-member-api)
3. [TaxiParty API](#3-taxiparty-api)
4. [Chat API](#4-chat-api)
5. [Board API](#5-board-api)
6. [Notice API](#6-notice-api)
7. [Academic API](#7-academic-api)
8. [Support API](#8-support-api)
9. [Notification API](#9-notification-api)
10. [SSE (Server-Sent Events)](#10-sse-server-sent-events)
11. [Image API](#11-image-api)
12. [Admin API](#12-admin-api)

---

## 1. 공통 사항

### 1.1 Base URL

```
Production: (도메인 미정)
Development: http://localhost:8080/v1
```

### 1.2 인증

대부분의 API는 Firebase Authentication의 ID Token(JWT)을 사용한 Bearer 인증이 필요합니다.

```http
Authorization: Bearer <firebase_id_token>
```

**Public API (인증 불필요):**

다음 2개의 API만 인증 없이 호출 가능합니다. 나머지 모든 API는 Firebase ID Token 인증이 필수입니다.

| API | 이유 |
|-----|------|
| `GET /app-versions/{platform}` | 앱 실행 초기(로그인 전) 강제 업데이트 여부 확인 |
| `GET /app-notices` | 로그인 전 점검 공지 / 긴급 공지 표시 필요 |

### 1.3 공통 Response 형식

**성공 응답:**
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

**페이지네이션 응답:**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**에러 응답:**
```json
{
  "success": false,
  "data": null,
  "message": "에러 메시지",
  "errorCode": "ERROR_CODE",
  "timestamp": "2026-02-03T12:00:00Z"
}
```

### 1.4 공통 에러 코드

| HTTP 상태 | 에러 코드 | 설명 |
|----------|----------|------|
| 400 | `INVALID_REQUEST` | 잘못된 요청 |
| 401 | `UNAUTHORIZED` | 인증 필요 |
| 403 | `FORBIDDEN` | 권한 없음 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 충돌 (중복 등) |
| 422 | `VALIDATION_ERROR` | 유효성 검사 실패 |
| 500 | `INTERNAL_ERROR` | 서버 오류 |

### 1.5 공통 Query Parameters

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | 20 | 페이지 크기 (최대 100) |
| `sort` | string | - | 정렬 기준 (예: `createdAt,desc`) |

---

## 2. Member API

### 2.1 인증 및 로그인 플로우

본 프로젝트는 **Firebase Authentication을 인증 주체로 유지**합니다.

- 로그인/토큰 발급/토큰 갱신(Refresh Token 포함)은 **클라이언트(RN) + Firebase Auth**가 담당합니다.
- Spring 백엔드는 클라이언트가 전달한 **Firebase ID Token(JWT)** 을 검증하여 인증을 처리합니다.
- 따라서 Spring 백엔드에는 `/auth/*` 형태의 “로그인/토큰 재발급” API가 존재하지 않습니다.

> 참고: 로그아웃은 클라이언트에서 Firebase Auth 로그아웃을 수행합니다.
> 서버는 푸시 알림을 위한 FCM 토큰 등록/해제 API만 제공합니다. (2.3 참고)

---

#### 전체 로그인 플로우

클라이언트는 Firebase의 `isNewUser` 플래그를 기준으로 신규/기존 회원을 구분하여 서버 API를 분기 호출합니다.

```
클라이언트 (React Native)
    │
    ├─ 1. Google Sign-In → Firebase ID Token + isNewUser 플래그 수신
    │
    ├─ 이메일 도메인 검증 (@sungkyul.ac.kr)
    │      └─ 실패 시 → 로그인 중단, 안내 메시지 표시
    │
    ├─ isNewUser == true (신규 회원)
    │      ├─ POST /members          ← 회원 레코드 생성
    │      └─ CompleteProfile 화면   ← 학번/실명/학과 입력
    │
    └─ isNewUser == false (기존 회원)
           ├─ GET /members/me        ← last_login 갱신 + 프로필 조회
           └─ 홈 화면
```

> **주의:** Firebase의 `isNewUser`가 간헐적으로 부정확할 수 있습니다.
> 서버는 `POST /members` 중복 호출(이미 존재하는 uid)에 대해 **200 OK + 기존 프로필**을 반환하여 안전하게 처리합니다.

**이메일 도메인 제한:**
모든 인증 API에서 Firebase ID Token의 `email`이 `@sungkyul.ac.kr`로 끝나지 않으면 `403`을 반환합니다.

```json
{
  “success”: false,
  “errorCode”: “EMAIL_DOMAIN_RESTRICTED”,
  “message”: “성결대학교 이메일(@sungkyul.ac.kr)만 사용 가능합니다.”
}
```

---

### 2.2 회원 가입

#### POST /members
신규 회원 레코드 생성 (첫 로그인 시 1회 호출)

**인증:** Firebase ID Token 필수

Spring 서버 처리:
1. Firebase Admin SDK로 ID Token 검증 → `uid`, `email`, `displayName`, `photoURL` 추출
2. `members` 테이블에서 `uid` 조회
   - **없음 (신규)** → INSERT + `linked_accounts` INSERT → 201 Created
   - **있음 (중복)** → 별도 처리 없이 기존 프로필 200 OK 반환 (idempotent)

**Request:** (body 없음, ID Token만 사용)

**Response (신규 생성 — 201 Created):**
```json
{
  “success”: true,
  “data”: {
    “id”: “firebase_uid”,
    “email”: “user@sungkyul.ac.kr”,
    “displayName”: “스쿠리 유저”,
    “studentId”: null,
    “department”: null,
    “realname”: null,
    “photoUrl”: “https://...”,
    “isAdmin”: false,
    “bankAccount”: null,
    “joinedAt”: “2026-02-03T12:00:00Z”
  }
}
```

**Response (중복 호출 — 200 OK):**
```json
{
  “success”: true,
  “data”: {
    “id”: “firebase_uid”,
    “email”: “user@sungkyul.ac.kr”,
    “displayName”: “홍길동”,
    “studentId”: “20201234”,
    “department”: “컴퓨터공학과”,
    “realname”: “홍길동”,
    “photoUrl”: “https://...”,
    “isAdmin”: false,
    “bankAccount”: { ... },
    “joinedAt”: “2024-03-01T00:00:00Z”
  }
}
```

---

### 2.3 회원 정보

#### GET /members/me
내 정보 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "displayName": "홍길동",
    "studentId": "20201234",
    "department": "컴퓨터공학과",
    "photoUrl": "https://...",
    "realname": "홍길동",
    "isAdmin": false,
    "bankAccount": {
      "bankName": "카카오뱅크",
      "accountNumber": "3333-01-1234567",
      "accountHolder": "홍길동",
      "hideName": false
    },
    "notificationSetting": {
      "allNotifications": true,
      "partyNotifications": true,
      "noticeNotifications": true,
      "boardLikeNotifications": true,
      "boardCommentNotifications": true,
      "systemNotifications": true,
      "noticeNotificationsDetail": {
        "news": true,
        "academy": true,
        "scholarship": false
      }
    },
    "joinedAt": "2024-03-01T00:00:00Z"
  }
}
```

#### PATCH /members/me
내 정보 수정

**Request:**
```json
{
  "displayName": "새닉네임",
  "studentId": "20201234",
  "department": "컴퓨터공학과",
  "photoUrl": "https://..."
}
```

#### PATCH /members/me/bank-account
계좌 정보 수정

**Request:**
```json
{
  "bankName": "카카오뱅크",
  "accountNumber": "3333-01-1234567",
  "accountHolder": "홍길동",
  "hideName": false
}
```

#### PATCH /members/me/notification-settings
알림 설정 수정

**Request:**
```json
{
  "partyNotifications": true,
  "noticeNotifications": false,
  "noticeNotificationsDetail": {
    "news": true,
    "academy": false
  }
}
```

#### DELETE /members/me
회원 탈퇴

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "회원 탈퇴가 완료되었습니다."
  }
}
```

### 2.4 FCM 토큰

#### POST /members/me/fcm-tokens
FCM 토큰 등록

**Request:**
```json
{
  "token": "fcm_device_token",
  "platform": "ios"
}
```

#### DELETE /members/me/fcm-tokens/{token}
FCM 토큰 삭제

### 2.5 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `EMAIL_DOMAIN_RESTRICTED` | 403 | `@sungkyul.ac.kr` 이메일이 아닌 계정으로 로그인 시도 |
| `MEMBER_NOT_FOUND` | 404 | 존재하지 않는 회원 |
| `BANK_ACCOUNT_NOT_FOUND` | 404 | 계좌 정보 미등록 상태에서 계좌 공유 시도 |

---

## 3. TaxiParty API

### 3.1 파티 조회

#### GET /parties
파티 목록 조회

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `status` | string | 파티 상태 (OPEN, CLOSED) |
| `departureTime` | datetime | 출발 시간 이후 |
| `departureName` | string | 출발지 검색 |
| `destinationName` | string | 목적지 검색 |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "leaderId": "leader_uuid",
        "leaderName": "홍길동",
        "leaderPhotoUrl": "https://...",
        "departure": {
          "name": "성결대학교",
          "lat": 37.123456,
          "lng": 127.123456
        },
        "destination": {
          "name": "안양역",
          "lat": 37.234567,
          "lng": 127.234567
        },
        "departureTime": "2026-02-03T14:00:00Z",
        "maxMembers": 4,
        "currentMembers": 2,
        "tags": ["빠른출발", "조용한분"],
        "detail": "택시비 나눠요",
        "status": "OPEN",
        "createdAt": "2026-02-03T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "hasNext": true
  }
}
```

#### GET /parties/{partyId}
파티 상세 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "leaderId": "leader_uuid",
    "leaderName": "홍길동",
    "leaderPhotoUrl": "https://...",
    "departure": {
      "name": "성결대학교",
      "lat": 37.123456,
      "lng": 127.123456
    },
    "destination": {
      "name": "안양역",
      "lat": 37.234567,
      "lng": 127.234567
    },
    "departureTime": "2026-02-03T14:00:00Z",
    "maxMembers": 4,
    "members": [
      {
        "id": "member_uuid",
        "displayName": "홍길동",
        "photoUrl": "https://...",
        "isLeader": true,
        "joinedAt": "2026-02-03T12:00:00Z"
      },
      {
        "id": "member_uuid_2",
        "displayName": "김철수",
        "photoUrl": "https://...",
        "isLeader": false,
        "joinedAt": "2026-02-03T12:30:00Z"
      }
    ],
    "tags": ["빠른출발"],
    "detail": "택시비 나눠요",
    "status": "OPEN",
    "settlement": null,
    "createdAt": "2026-02-03T12:00:00Z"
  }
}
```

#### GET /parties/my
내가 참여중인 파티 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "ARRIVED",
    "departure": { ... },
    "destination": { ... },
    "isLeader": true,
    "settlement": {
      "status": "PENDING",
      "perPersonAmount": 3500,
      "memberSettlements": [
        {
          "memberId": "uuid",
          "memberName": "홍길동",
          "settled": true,
          "settledAt": "2026-02-03T14:30:00Z"
        },
        {
          "memberId": "uuid2",
          "memberName": "김철수",
          "settled": false,
          "settledAt": null
        }
      ]
    }
  }
}
```

### 3.2 파티 생성/관리

#### POST /parties
파티 생성

**Request:**
```json
{
  "departure": {
    "name": "성결대학교",
    "lat": 37.123456,
    "lng": 127.123456
  },
  "destination": {
    "name": "안양역",
    "lat": 37.234567,
    "lng": 127.234567
  },
  "departureTime": "2026-02-03T14:00:00Z",
  "maxMembers": 4,
  "tags": ["빠른출발"],
  "detail": "택시비 나눠요"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "created_party_uuid",
    "chatRoomId": "party:created_party_uuid"
  }
}
```

#### PATCH /parties/{partyId}
파티 정보 수정 (리더만)

**Request:**
```json
{
  "departureTime": "2026-02-03T14:30:00Z",
  "maxMembers": 3,
  "detail": "수정된 상세"
}
```

#### POST /parties/{partyId}/close
파티 모집 마감 (리더만)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "CLOSED"
  }
}
```

#### POST /parties/{partyId}/reopen
파티 모집 재개 (리더만)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "OPEN"
  }
}
```

#### POST /parties/{partyId}/arrive
도착 및 정산 시작 (리더만)

- OPEN 또는 CLOSED 상태에서만 호출 가능
- 리더를 제외한 멤버가 1명 이상 있어야 함 (정산 대상이 없으면 호출 불가)

**Request:**
```json
{
  "taxiFare": 14000
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "ARRIVED",
    "settlement": {
      "status": "PENDING",
      "perPersonAmount": 3500,
      "memberSettlements": [ ... ]
    }
  }
}
```

**에러 코드 (arrive 전용):**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `PARTY_NOT_ARRIVABLE` | 409 | OPEN/CLOSED 상태가 아닌 파티 |
| `NO_MEMBERS_TO_SETTLE` | 409 | 리더 외 멤버가 없어 정산 불가 |

#### POST /parties/{partyId}/settle
정산 완료 표시 (본인)

- 리더(정산자)는 호출 불가 (리더는 수금자이므로 정산 대상 아님)
- 모든 멤버의 정산이 완료되면 서버가 자동으로 `ENDED(ARRIVED)` 전이

**Response:**
```json
{
  "success": true,
  "data": {
    "settled": true,
    "settledAt": "2026-02-03T14:30:00Z",
    "allSettled": true
  }
}
```

> **`allSettled: true`** 시 파티 상태는 `ENDED`, `endReason: ARRIVED`로 자동 전이됩니다.
> 클라이언트는 이 필드를 감지해 종료 처리 UI를 표시합니다.

#### POST /parties/{partyId}/end
파티 강제 종료 (리더만, 정산 미완료 상태에서도 가능)

- ARRIVED 상태에서만 호출 가능
- 미정산 멤버가 남아 있더라도 리더가 강제로 파티를 종료할 수 있음
- `endReason: FORCE_ENDED`로 기록

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "ENDED",
    "endReason": "FORCE_ENDED"
  }
}
```

#### DELETE /parties/{partyId}
파티 취소 (리더만)

- OPEN 또는 CLOSED 상태에서만 가능 (ARRIVED 상태 불가)
- 파티 상태 → `ENDED`, `endReason: CANCELLED`

**에러 코드:**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `PARTY_NOT_CANCELABLE` | 409 | ARRIVED 상태에서 취소 불가 |

### 3.3 멤버 관리

#### POST /parties/{partyId}/leave
파티 나가기 (멤버)

- 리더는 나가기 불가 (취소 또는 위임 불가 정책)
- ARRIVED 상태에서는 나가기 불가 (정산 완료 후 자동 종료 대기)
- 리더가 탈퇴(회원탈퇴)하면 파티 강제 종료 (`endReason: WITHDRAWED`)

**Response:**
```json
{
  "success": true
}
```

**에러 코드:**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `LEADER_CANNOT_LEAVE` | 409 | 리더는 나가기 불가 |
| `CANNOT_LEAVE_ARRIVED_PARTY` | 409 | ARRIVED 상태에서 나가기 불가 |

#### DELETE /parties/{partyId}/members/{memberId}
멤버 강퇴 (리더만)

- OPEN 또는 CLOSED 상태에서만 가능 (ARRIVED 상태 불가)
- 리더 본인은 강퇴 불가

**Response:**
```json
{
  "success": true
}
```

**에러 코드:**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `CANNOT_KICK_IN_ARRIVED` | 409 | ARRIVED 상태에서 강퇴 불가 |
| `CANNOT_KICK_LEADER` | 400 | 리더 본인 강퇴 불가 |

### 3.4 동승 요청

#### GET /parties/{partyId}/join-requests
파티의 동승 요청 목록 (리더만)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "request_uuid",
      "requesterId": "user_uuid",
      "requesterName": "김철수",
      "requesterPhotoUrl": "https://...",
      "status": "PENDING",
      "createdAt": "2026-02-03T12:30:00Z"
    }
  ]
}
```

#### POST /parties/{partyId}/join-requests
동승 요청 생성

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "request_uuid",
    "status": "PENDING"
  }
}
```

#### GET /join-requests/my
내가 보낸 동승 요청 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `status` | string | 요청 상태 (PENDING, ACCEPTED, DECLINED, CANCELED) |

#### GET /join-requests/{requestId}
동승 요청 상세

#### POST /join-requests/{requestId}/accept
동승 요청 수락 (리더만)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "request_uuid",
    "status": "ACCEPTED",
    "partyId": "party_uuid"
  }
}
```

#### POST /join-requests/{requestId}/decline
동승 요청 거절 (리더만)

#### POST /join-requests/{requestId}/cancel
동승 요청 취소 (요청자)

### 3.5 에러 코드

| 에러 코드 | 설명 |
|----------|------|
| `PARTY_NOT_FOUND` | 파티를 찾을 수 없음 |
| `PARTY_FULL` | 파티 정원 초과 |
| `PARTY_CLOSED` | 모집 마감된 파티 |
| `PARTY_ENDED` | 이미 종료된 파티 |
| `NOT_PARTY_LEADER` | 리더 권한 필요 |
| `NOT_PARTY_MEMBER` | 파티 멤버가 아님 |
| `ALREADY_IN_PARTY` | 이미 다른 파티에 참여 중 |
| `ALREADY_REQUESTED` | 이미 동승 요청함 |
| `REQUEST_NOT_FOUND` | 동승 요청을 찾을 수 없음 |
| `REQUEST_ALREADY_PROCESSED` | 이미 처리된 요청 |
| `SETTLEMENT_NOT_COMPLETED` | 정산이 완료되지 않음 |
| `LEADER_CANNOT_SETTLE` | 리더는 정산 대상이 아님 |
| `ALREADY_SETTLED` | 이미 정산 완료 처리됨 |
| `PARTY_NOT_ARRIVABLE` | OPEN/CLOSED 상태가 아닌 파티 (arrive 호출 불가) |
| `NO_MEMBERS_TO_SETTLE` | 정산 대상 멤버 없음 (리더만 남은 파티) |
| `PARTY_NOT_CANCELABLE` | ARRIVED 상태에서 취소 불가 |
| `LEADER_CANNOT_LEAVE` | 리더는 파티 나가기 불가 |
| `CANNOT_LEAVE_ARRIVED_PARTY` | ARRIVED 상태에서 나가기 불가 |
| `CANNOT_KICK_IN_ARRIVED` | ARRIVED 상태에서 강퇴 불가 |
| `CANNOT_KICK_LEADER` | 리더 본인 강퇴 불가 |

---

## 4. Chat API

### 4.1 채팅방 조회

#### GET /chat-rooms
공개 채팅방 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `type` | string | 채팅방 타입 (UNIVERSITY, DEPARTMENT, GAME, CUSTOM) |
| `joined` | boolean | 참여 중인 채팅방만 |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "room_id",
      "name": "성결대 전체 채팅방",
      "type": "UNIVERSITY",
      "memberCount": 150,
      "lastMessage": {
        "text": "안녕하세요!",
        "senderName": "홍길동",
        "timestamp": "2026-02-03T12:00:00Z"
      },
      "unreadCount": 5,
      "isJoined": true
    }
  ]
}
```

#### GET /chat-rooms/{chatRoomId}
채팅방 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "room_id",
    "name": "성결대 전체 채팅방",
    "type": "UNIVERSITY",
    "description": "성결대학교 학생들의 소통 공간",
    "memberCount": 150,
    "isPublic": true,
    "isJoined": true,
    "isMuted": false,
    "lastReadAt": "2026-02-03T11:00:00Z"
  }
}
```

#### GET /chat-rooms/{chatRoomId}/messages
채팅 메시지 조회

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `before` | datetime | 이 시간 이전 메시지 |
| `limit` | int | 메시지 수 (기본 50) |

**Response:**
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": "message_uuid",
        "senderId": "user_uuid",
        "senderName": "홍길동",
        "text": "안녕하세요!",
        "type": "TEXT",
        "createdAt": "2026-02-03T12:00:00Z"
      },
      {
        "id": "message_uuid_2",
        "senderId": "user_uuid_2",
        "senderName": "시스템",
        "text": "홍길동님이 입장했습니다.",
        "type": "SYSTEM",
        "createdAt": "2026-02-03T11:59:00Z"
      }
    ],
    "hasMore": true,
    "oldestTimestamp": "2026-02-03T11:00:00Z"
  }
}
```

### 4.2 채팅방 참여

#### POST /chat-rooms/{chatRoomId}/join
채팅방 참여

#### POST /chat-rooms/{chatRoomId}/leave
채팅방 나가기

#### PATCH /chat-rooms/{chatRoomId}/settings
채팅방 설정 (음소거 등)

**Request:**
```json
{
  "muted": true
}
```

#### POST /chat-rooms/{chatRoomId}/read
읽음 처리

**Request:**
```json
{
  "lastReadAt": "2026-02-03T12:00:00Z"
}
```

### 4.3 메시지 조회

#### GET /chat-rooms/{chatRoomId}/messages
채팅 메시지 이력 조회 (과거 메시지 로딩)

> **메시지 전송은 WebSocket(STOMP)으로만 수행합니다. (§4.5 참고)**

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `before` | datetime | 이 시간 이전 메시지 조회 (무한 스크롤) |
| `limit` | int | 메시지 수 (기본 50) |

### 4.4 파티 채팅 (TaxiParty 도메인에서 관리)

파티 채팅 메시지 **전송**은 WebSocket(STOMP)으로만 수행합니다. (§4.5 참고)
파티 채팅 비즈니스 규칙(멤버 검증, 계좌 정보 조회 등)은 서버 내부 STOMP 핸들러에서 처리합니다.

#### GET /parties/{partyId}/chat/messages
파티 채팅 메시지 이력 조회 (과거 메시지 로딩)

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `before` | datetime | 이 시간 이전 메시지 조회 |
| `limit` | int | 메시지 수 (기본 50) |

### 4.5 WebSocket (STOMP)

모든 채팅 메시지 **전송 및 실시간 수신**은 WebSocket(STOMP)을 통해 수행합니다.

#### STOMP Endpoint
```
ws://api.skuri.app/ws
```

#### 연결 인증
연결 시 STOMP CONNECT 프레임 헤더에 Firebase ID Token을 포함합니다.

```
CONNECT
accept-version:1.2
Authorization:Bearer <firebase_id_token>

^@
```

> Spring의 `@MessageMapping` 핸들러에서 `Principal` 객체로 `uid`를 추출합니다.
> 연결 인증 성공 후에는 추가 토큰 검증 없이 세션을 유지합니다.

---

#### 일반 채팅

| 방향 | 경로 | 설명 |
|------|------|------|
| 전송 (Publish) | `/app/chat-rooms/{chatRoomId}/messages` | 메시지 전송 |
| 수신 (Subscribe) | `/topic/chat-rooms/{chatRoomId}` | 실시간 메시지 수신 |

**전송 포맷:**
```json
{ "type": "TEXT", "text": "안녕하세요!" }
{ "type": "IMAGE", "imageUrl": "https://..." }
```

---

#### 파티 채팅

| 방향 | 경로 | 설명 |
|------|------|------|
| 전송 (Publish) | `/app/parties/{partyId}/chat/messages` | 파티 채팅 메시지 전송 |
| 수신 (Subscribe) | `/topic/parties/{partyId}/chat` | 실시간 메시지 수신 |

**전송 포맷:**
```json
{ "type": "TEXT", "text": "곧 도착합니다!" }
{ "type": "ACCOUNT" }
```
> `ACCOUNT` 타입: body 없이 서버가 발신자의 등록된 계좌 정보를 조회하여 메시지에 삽입

**수신 포맷 (서버 → 클라이언트):**
```json
{
  "id": "message_uuid",
  "senderId": "user_uuid",
  "senderName": "홍길동",
  "type": "ACCOUNT",
  "accountInfo": {
    "bankName": "카카오뱅크",
    "accountNumber": "3333-01-1234567",
    "accountHolder": "홍길동"
  },
  "createdAt": "2026-02-03T12:00:00Z"
}
```

### 4.6 트랜잭션 경계

| 작업 | 트랜잭션 범위 |
|------|------------|
| 메시지 전송 (STOMP 핸들러) | 메시지 DB 저장 + ChatRoom.messageCount 증가 → 커밋 후 구독자 브로드캐스트 |
| 채팅방 참여 (`POST /chat-rooms/{chatRoomId}/join`) | ChatRoomMember 추가 + ChatRoom.memberCount 증가 |
| 채팅방 나가기 (`POST /chat-rooms/{chatRoomId}/leave`) | ChatRoomMember 삭제 + ChatRoom.memberCount 감소 |
| ACCOUNT 메시지 | 계좌 정보 DB 조회 + 메시지 DB 저장 → 커밋 후 브로드캐스트 |

> 브로드캐스트(WebSocket push)는 트랜잭션 커밋 성공 후 수행합니다. (Spring의 `@TransactionalEventListener` 활용)

### 4.7 에러 코드

| 에러 코드 | 설명 |
|----------|------|
| `CHAT_ROOM_NOT_FOUND` | 채팅방을 찾을 수 없음 |
| `NOT_CHAT_ROOM_MEMBER` | 채팅방 멤버가 아님 |
| `CHAT_ROOM_FULL` | 채팅방 정원 초과 |
| `ALREADY_CHAT_ROOM_MEMBER` | 이미 참여 중인 채팅방 |
| `STOMP_AUTH_FAILED` | WebSocket STOMP 연결 인증 실패 (토큰 검증 오류) |
| `BANK_ACCOUNT_REQUIRED` | ACCOUNT 메시지 전송 시 계좌 미등록 상태 |

---

## 5. Board API

### 5.1 게시글 조회

#### GET /posts
게시글 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `category` | string | 카테고리 (GENERAL, QUESTION, REVIEW, ANNOUNCEMENT) |
| `search` | string | 제목/내용 검색 |
| `authorId` | string | 작성자 ID |
| `sort` | string | 정렬 (latest, popular, mostCommented, mostViewed) |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "post_uuid",
        "title": "게시글 제목",
        "content": "내용 미리보기...",
        "authorId": "user_uuid",
        "authorName": "홍길동",
        "authorProfileImage": "https://...",
        "isAnonymous": false,
        "category": "GENERAL",
        "viewCount": 100,
        "likeCount": 10,
        "commentCount": 5,
        "hasImage": true,
        "isPinned": false,
        "createdAt": "2026-02-03T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100
  }
}
```

#### GET /posts/{postId}
게시글 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "post_uuid",
    "title": "게시글 제목",
    "content": "게시글 전체 내용",
    "authorId": "user_uuid",
    "authorName": "홍길동",
    "authorProfileImage": "https://...",
    "isAnonymous": false,
    "category": "GENERAL",
    "viewCount": 101,
    "likeCount": 10,
    "commentCount": 5,
    "bookmarkCount": 3,
    "images": [
      {
        "url": "https://...",
        "thumbUrl": "https://...",
        "width": 800,
        "height": 600
      }
    ],
    "isLiked": true,
    "isBookmarked": false,
    "isAuthor": true,
    "createdAt": "2026-02-03T12:00:00Z",
    "updatedAt": "2026-02-03T12:00:00Z"
  }
}
```

#### GET /posts/bookmarked
북마크한 게시글 목록

### 5.2 게시글 작성/수정

#### POST /posts
게시글 작성

**Request:**
```json
{
  "title": "게시글 제목",
  "content": "게시글 내용",
  "category": "GENERAL",
  "isAnonymous": false,
  "images": [
    {
      "url": "https://...",
      "thumbUrl": "https://...",
      "width": 800,
      "height": 600
    }
  ]
}
```

#### PATCH /posts/{postId}
게시글 수정

**Request:**
```json
{
  "title": "수정된 제목",
  "content": "수정된 내용"
}
```

#### DELETE /posts/{postId}
게시글 삭제

### 5.3 게시글 상호작용

#### POST /posts/{postId}/like
좋아요

**Response:**
```json
{
  "success": true,
  "data": {
    "isLiked": true,
    "likeCount": 11
  }
}
```

#### DELETE /posts/{postId}/like
좋아요 취소

#### POST /posts/{postId}/bookmark
북마크

#### DELETE /posts/{postId}/bookmark
북마크 취소

### 5.4 댓글

#### GET /posts/{postId}/comments
댓글 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "comment_uuid",
      "content": "댓글 내용",
      "authorId": "user_uuid",
      "authorName": "홍길동",
      "authorProfileImage": "https://...",
      "isAnonymous": false,
      "anonymousOrder": null,
      "isAuthor": false,
      "isPostAuthor": true,
      "replies": [
        {
          "id": "reply_uuid",
          "content": "대댓글 내용",
          "authorId": "user_uuid_2",
          "authorName": "익명2",
          "isAnonymous": true,
          "anonymousOrder": 2,
          "createdAt": "2026-02-03T12:30:00Z"
        }
      ],
      "createdAt": "2026-02-03T12:00:00Z"
    }
  ]
}
```

#### POST /posts/{postId}/comments
댓글 작성

**Request:**
```json
{
  "content": "댓글 내용",
  "isAnonymous": false,
  "parentId": null
}
```

**Request (대댓글):**
```json
{
  "content": "대댓글 내용",
  "isAnonymous": true,
  "parentId": "parent_comment_uuid"
}
```

#### PATCH /comments/{commentId}
댓글 수정

#### DELETE /comments/{commentId}
댓글 삭제

### 5.5 에러 코드

| 에러 코드 | 설명 |
|----------|------|
| `POST_NOT_FOUND` | 게시글을 찾을 수 없음 |
| `COMMENT_NOT_FOUND` | 댓글을 찾을 수 없음 |
| `NOT_POST_AUTHOR` | 게시글 작성자가 아님 |
| `NOT_COMMENT_AUTHOR` | 댓글 작성자가 아님 |

---

## 6. Notice API

### 6.1 학교 공지

#### GET /notices
공지사항 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `category` | string | 카테고리 (새소식, 학사, 학생 등) |
| `search` | string | 제목 검색 |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "notice_id",
        "title": "2026학년도 1학기 수강신청 안내",
        "category": "학사",
        "department": "성결대학교",
        "author": "교무처",
        "postedAt": "2026-02-01T00:00:00Z",
        "viewCount": 500,
        "commentCount": 10,
        "isRead": true
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1000
  }
}
```

#### GET /notices/{noticeId}
공지사항 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "notice_id",
    "title": "2026학년도 1학기 수강신청 안내",
    "content": "요약 내용",
    "contentDetail": "<html>...</html>",
    "link": "https://www.sungkyul.ac.kr/...",
    "category": "학사",
    "department": "성결대학교",
    "author": "교무처",
    "postedAt": "2026-02-01T00:00:00Z",
    "viewCount": 501,
    "commentCount": 10,
    "attachments": [
      {
        "name": "수강신청 안내.pdf",
        "downloadUrl": "https://...",
        "previewUrl": "https://..."
      }
    ],
    "isRead": true
  }
}
```

#### POST /notices/{noticeId}/read
읽음 표시

### 6.2 공지 댓글

#### GET /notices/{noticeId}/comments
공지 댓글 목록

#### POST /notices/{noticeId}/comments
공지 댓글 작성

**Request:**
```json
{
  "content": "댓글 내용",
  "isAnonymous": false,
  "parentId": null
}
```

#### DELETE /notice-comments/{commentId}
공지 댓글 삭제

### 6.3 앱 공지

#### GET /app-notices
앱 공지 목록 **(Public API — 인증 불필요)**

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "app_notice_uuid",
      "title": "앱 업데이트 안내",
      "content": "새로운 기능이 추가되었습니다.",
      "category": "UPDATE",
      "priority": "NORMAL",
      "imageUrls": ["https://..."],
      "actionUrl": "https://...",
      "publishedAt": "2026-02-01T00:00:00Z"
    }
  ]
}
```

#### GET /app-notices/{appNoticeId}
앱 공지 상세

### 6.4 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `NOTICE_NOT_FOUND` | 404 | 존재하지 않는 학교 공지 |
| `APP_NOTICE_NOT_FOUND` | 404 | 존재하지 않는 앱 공지 |
| `NOTICE_COMMENT_NOT_FOUND` | 404 | 존재하지 않는 공지 댓글 |
| `NOT_NOTICE_COMMENT_AUTHOR` | 403 | 댓글 작성자가 아닌데 수정/삭제 시도 |

---

## 7. Academic API

### 7.1 강의

#### GET /courses
강의 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 학기 (2026-1) |
| `department` | string | 학과 |
| `professor` | string | 교수명 |
| `search` | string | 강의명 검색 |
| `dayOfWeek` | int | 요일 (1-5) |
| `grade` | int | 학년 (1-4) |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "course_uuid",
        "code": "01255",
        "division": "001",
        "name": "민법총칙",
        "credits": 3,
        "professor": "문상혁",
        "department": "법학과",
        "grade": 2,
        "category": "전공선택",
        "schedule": [
          {
            "dayOfWeek": 1,
            "startPeriod": 3,
            "endPeriod": 4
          },
          {
            "dayOfWeek": 3,
            "startPeriod": 3,
            "endPeriod": 4
          }
        ],
        "location": "영401"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 500
  }
}
```

### 7.2 시간표

#### GET /timetables/my
내 시간표 조회

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 학기 (기본: 현재 학기) |

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "timetable_uuid",
    "semester": "2026-1",
    "courses": [
      {
        "id": "course_uuid",
        "name": "민법총칙",
        "professor": "문상혁",
        "location": "영401",
        "schedule": [
          { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 }
        ],
        "color": "#4CAF50"
      }
    ]
  }
}
```

#### POST /timetables/my/courses
시간표에 강의 추가

**Request:**
```json
{
  "courseId": "course_uuid",
  "semester": "2026-1"
}
```

#### DELETE /timetables/my/courses/{courseId}
시간표에서 강의 삭제

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 학기 |

### 7.3 학사 일정

#### GET /academic-schedules
학사 일정 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `startDate` | date | 시작일 이후 |
| `endDate` | date | 종료일 이전 |
| `primary` | boolean | 주요 일정만 |

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "schedule_uuid",
      "title": "1학기 개강",
      "startDate": "2026-03-02",
      "endDate": "2026-03-02",
      "type": "SINGLE",
      "isPrimary": true,
      "description": "2026학년도 1학기 개강일"
    },
    {
      "id": "schedule_uuid_2",
      "title": "중간고사",
      "startDate": "2026-04-15",
      "endDate": "2026-04-21",
      "type": "MULTI",
      "isPrimary": true
    }
  ]
}
```

### 7.4 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `COURSE_NOT_FOUND` | 404 | 존재하지 않는 강의 |
| `TIMETABLE_CONFLICT` | 409 | 시간표 추가 시 기존 강의와 시간 충돌 |
| `COURSE_ALREADY_IN_TIMETABLE` | 409 | 이미 시간표에 추가된 강의 중복 추가 |

---

## 8. Support API

### 8.1 문의

#### POST /inquiries
문의 등록

**Request:**
```json
{
  "type": "BUG",
  "subject": "앱 오류 문의",
  "content": "채팅 화면에서 오류가 발생합니다."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "inquiry_uuid",
    "status": "PENDING",
    "createdAt": "2026-02-03T12:00:00Z"
  }
}
```

#### GET /inquiries/my
내 문의 목록

### 8.2 신고

#### POST /reports
신고 등록

**Request:**
```json
{
  "targetType": "POST",
  "targetId": "post_uuid",
  "category": "spam",
  "reason": "광고성 게시글입니다."
}
```

### 8.3 앱 버전

#### GET /app-versions/{platform}
앱 버전 정보 (Public API)

**Response:**
```json
{
  "success": true,
  "data": {
    "platform": "ios",
    "minimumVersion": "1.5.0",
    "forceUpdate": false,
    "message": "새로운 기능이 추가되었습니다.",
    "title": "업데이트 안내",
    "showButton": true,
    "buttonText": "업데이트",
    "buttonUrl": "https://apps.apple.com/..."
  }
}
```

### 8.4 학식 메뉴

#### GET /cafeteria-menus
학식 메뉴

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `date` | date | 조회 날짜 (기본: 오늘) |

**Response:**
```json
{
  "success": true,
  "data": {
    "weekId": "2026-W06",
    "weekStart": "2026-02-03",
    "weekEnd": "2026-02-07",
    "menus": {
      "2026-02-03": {
        "rollNoodles": ["우동", "김밥"],
        "theBab": ["돈까스", "된장찌개"],
        "fryRice": ["볶음밥", "짜장면"]
      }
    }
  }
}
```

### 8.5 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `INQUIRY_NOT_FOUND` | 404 | 존재하지 않는 문의 |
| `REPORT_ALREADY_SUBMITTED` | 409 | 동일 대상에 대한 중복 신고 |
| `CANNOT_REPORT_YOURSELF` | 400 | 자기 자신을 신고 시도 |

---

## 9. Notification API

### 9.1 알림 조회

#### GET /notifications
알림 목록

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `unreadOnly` | boolean | 읽지 않은 알림만 |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "notification_uuid",
        "type": "PARTY_JOIN_ACCEPTED",
        "title": "동승 요청이 승인되었어요",
        "message": "파티에 합류하세요!",
        "data": {
          "partyId": "party_uuid"
        },
        "isRead": false,
        "createdAt": "2026-02-03T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "unreadCount": 5
  }
}
```

#### GET /notifications/unread-count
읽지 않은 알림 수

**Response:**
```json
{
  "success": true,
  "data": {
    "count": 5
  }
}
```

### 9.2 알림 관리

#### POST /notifications/{notificationId}/read
알림 읽음 처리

#### POST /notifications/read-all
모든 알림 읽음 처리

#### DELETE /notifications/{notificationId}
알림 삭제

### 9.3 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `NOTIFICATION_NOT_FOUND` | 404 | 존재하지 않는 알림 |
| `NOT_NOTIFICATION_OWNER` | 403 | 다른 사람의 알림 접근 시도 |

---

## 10. SSE (Server-Sent Events)

> `role-definition.md` §8.2 기준:
> 파티 목록/상태, 알림, 게시물 목록/조회수는 SSE를 통해 실시간 업데이트를 제공한다.

### 10.1 개요

| 항목 | 내용 |
|------|------|
| 프로토콜 | HTTP/1.1 SSE (`text/event-stream`) |
| 방향 | 단방향 (서버 → 클라이언트) |
| 인증 | 연결 시작 시 Firebase ID Token을 `Authorization` 헤더로 전달 |
| 재연결 | 클라이언트가 `retry` 필드 기준으로 자동 재연결 (기본 3,000ms) |
| 연결 유지 | 30초마다 서버에서 heartbeat 이벤트 전송 |

### 10.2 SSE 이벤트 포맷

모든 SSE 이벤트는 다음 형식을 따릅니다.

```
id: <이벤트 고유 ID>
event: <이벤트 타입>
data: <JSON 문자열>
retry: 3000
```

**예시:**
```
id: 1706922000000
event: PARTY_CREATED
data: {"id":"uuid","leaderId":"uuid","departureName":"성결대학교","destinationName":"안양역","departureTime":"2026-02-03T14:00:00Z","status":"OPEN"}
retry: 3000
```

### 10.3 파티 실시간 구독

#### GET /sse/parties
파티 목록 및 상태 변경을 실시간으로 구독합니다.

**인증:** Firebase ID Token 필수

**Headers:**
```http
Authorization: Bearer <firebase_id_token>
Accept: text/event-stream
Cache-Control: no-cache
```

**구독 이벤트 목록:**

| 이벤트 타입 | 발생 시점 | 수신 대상 |
|-----------|---------|---------|
| `PARTY_CREATED` | 새 파티 생성됨 | 전체 연결 사용자 |
| `PARTY_UPDATED` | 파티 정보 수정됨 | 전체 연결 사용자 |
| `PARTY_STATUS_CHANGED` | 파티 상태 변경됨 | 전체 연결 사용자 |
| `PARTY_DELETED` | 파티 삭제/취소됨 | 전체 연결 사용자 |
| `PARTY_MEMBER_JOINED` | 멤버 합류 | 해당 파티 멤버 |
| `PARTY_MEMBER_LEFT` | 멤버 나감/강퇴 | 해당 파티 멤버 |
| `HEARTBEAT` | 30초 주기 연결 유지 | 전체 연결 사용자 |

**이벤트 데이터 형식:**

```
// PARTY_CREATED
event: PARTY_CREATED
data: {
  "id": "party_uuid",
  "leaderId": "user_uuid",
  "leaderName": "홍길동",
  "departureName": "성결대학교",
  "destinationName": "안양역",
  "departureTime": "2026-02-03T14:00:00Z",
  "maxMembers": 4,
  "currentMembers": 1,
  "tags": ["빠른출발"],
  "status": "OPEN"
}

// PARTY_STATUS_CHANGED
event: PARTY_STATUS_CHANGED
data: {
  "id": "party_uuid",
  "status": "CLOSED",
  "currentMembers": 3
}

// PARTY_DELETED
event: PARTY_DELETED
data: {
  "id": "party_uuid"
}

// PARTY_MEMBER_JOINED
event: PARTY_MEMBER_JOINED
data: {
  "partyId": "party_uuid",
  "memberId": "user_uuid",
  "memberName": "김철수",
  "currentMembers": 2
}

// PARTY_MEMBER_LEFT
event: PARTY_MEMBER_LEFT
data: {
  "partyId": "party_uuid",
  "memberId": "user_uuid",
  "reason": "LEFT",
  "currentMembers": 1
}

// HEARTBEAT
event: HEARTBEAT
data: {"timestamp": "2026-02-03T12:00:30Z"}
```

### 10.4 알림 실시간 구독

#### GET /sse/notifications
사용자 알림을 실시간으로 구독합니다.

**인증:** Firebase ID Token 필수 (본인 알림만 수신)

**Headers:**
```http
Authorization: Bearer <firebase_id_token>
Accept: text/event-stream
Cache-Control: no-cache
```

**구독 이벤트 목록:**

| 이벤트 타입 | 발생 시점 |
|-----------|---------|
| `NOTIFICATION` | 새 알림 도착 (모든 알림 타입 공통) |
| `UNREAD_COUNT_CHANGED` | 읽지 않은 알림 수 변경 |
| `HEARTBEAT` | 30초 주기 연결 유지 |

**이벤트 데이터 형식:**

```
// NOTIFICATION
event: NOTIFICATION
data: {
  "id": "notification_uuid",
  "type": "PARTY_JOIN_ACCEPTED",
  "title": "동승 요청이 승인되었어요",
  "message": "파티에 합류하세요!",
  "data": {
    "partyId": "party_uuid"
  },
  "isRead": false,
  "createdAt": "2026-02-03T12:00:00Z"
}

// UNREAD_COUNT_CHANGED
event: UNREAD_COUNT_CHANGED
data: {
  "count": 3
}
```

**알림 타입 → 이동 경로:**

| 알림 타입 | data 필드 | 이동 경로 |
|---------|---------|---------|
| `PARTY_CREATED` | `partyId` | 파티 목록 |
| `PARTY_JOIN_REQUEST` | `partyId`, `requestId` | 동승 요청 목록 |
| `PARTY_JOIN_ACCEPTED` | `partyId` | 파티 상세 |
| `PARTY_JOIN_DECLINED` | `partyId` | 파티 목록 |
| `PARTY_CLOSED` | `partyId` | 파티 상세 |
| `PARTY_ARRIVED` | `partyId` | 파티 상세 (정산) |
| `PARTY_ENDED` | `partyId` | 파티 상세 |
| `MEMBER_KICKED` | `partyId` | 파티 목록 |
| `SETTLEMENT_COMPLETED` | `partyId` | 파티 상세 |
| `CHAT_MESSAGE` | `chatRoomId` | 채팅방 |
| `POST_LIKED` | `postId` | 게시글 상세 |
| `COMMENT_CREATED` | `postId`, `commentId` | 게시글 상세 |
| `NOTICE` | `noticeId` | 공지 상세 |
| `APP_NOTICE` | `appNoticeId` | 앱 공지 상세 |

### 10.5 게시물 실시간 구독

#### GET /sse/posts
게시물 목록 변경 및 조회수를 실시간으로 구독합니다.

**인증:** Firebase ID Token 필수

**Headers:**
```http
Authorization: Bearer <firebase_id_token>
Accept: text/event-stream
Cache-Control: no-cache
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `category` | string | 특정 카테고리만 구독 (생략 시 전체) |
| `postId` | string | 특정 게시글 조회수만 구독 |

**구독 이벤트 목록:**

| 이벤트 타입 | 발생 시점 |
|-----------|---------|
| `POST_CREATED` | 새 게시글 작성됨 |
| `POST_UPDATED` | 게시글 수정됨 |
| `POST_DELETED` | 게시글 삭제됨 |
| `POST_VIEW_COUNT_UPDATED` | 조회수 변경됨 (5 단위 배치 업데이트) |
| `HEARTBEAT` | 30초 주기 연결 유지 |

**이벤트 데이터 형식:**

```
// POST_CREATED
event: POST_CREATED
data: {
  "id": "post_uuid",
  "title": "게시글 제목",
  "authorName": "홍길동",
  "isAnonymous": false,
  "category": "GENERAL",
  "createdAt": "2026-02-03T12:00:00Z"
}

// POST_DELETED
event: POST_DELETED
data: {
  "id": "post_uuid"
}

// POST_VIEW_COUNT_UPDATED
event: POST_VIEW_COUNT_UPDATED
data: {
  "id": "post_uuid",
  "viewCount": 105
}
```

### 10.6 SSE 연결 관리

#### 연결 시작

React Native 클라이언트에서 SSE 연결은 `EventSource` 또는 `fetch` 스트리밍을 사용합니다.

```
// 연결 예시
GET /v1/sse/parties
Authorization: Bearer <firebase_id_token>
Accept: text/event-stream
```

#### 연결 끊김 / 재연결

- 서버는 `retry: 3000` 필드를 통해 클라이언트에게 재연결 간격(ms)을 지시합니다.
- 클라이언트가 재연결하면 서버는 **현재 상태 스냅샷을 `SNAPSHOT` 이벤트로 즉시 전송**합니다.
- 누락 이벤트 버퍼링은 하지 않습니다. 재연결 시점의 최신 상태가 곧 정답입니다.

**스냅샷 이벤트 형식 (재연결 직후 서버 전송):**

```
// 파티 목록 SSE (/sse/parties) 재연결 시
event: SNAPSHOT
data: {
  "parties": [ /* 현재 파티 목록 전체 */ ]
}

// 알림 SSE (/sse/notifications) 재연결 시
event: SNAPSHOT
data: {
  "unreadCount": 3
}

// 게시물 SSE (/sse/posts) 재연결 시
event: SNAPSHOT
data: {
  "posts": [ /* 현재 게시글 목록 전체 */ ]
}
```

> **Firebase onSnapshot과의 차이:**
> Firebase SDK는 연결 끊김 동안의 변경사항을 SDK 레벨에서 자동으로 동기화해줬습니다.
> Spring SSE는 SDK가 없으므로, 재연결 시 서버가 현재 상태를 다시 전송하는 방식으로 대체합니다.

```http
GET /v1/sse/parties
Authorization: Bearer <firebase_id_token>
```

#### 연결 종료 코드

| HTTP 상태 | 의미 | 클라이언트 동작 |
|----------|------|--------------|
| `200` | 연결 성공, 스트리밍 중 | 이벤트 수신 대기 |
| `401` | 인증 실패 (토큰 만료 등) | Firebase ID Token 갱신 후 재연결 |
| `403` | 권한 없음 | 재연결 금지 |

### 10.7 에러 이벤트

SSE 연결 중 서버 오류 발생 시 `ERROR` 이벤트를 전송 후 연결을 종료합니다.

```
event: ERROR
data: {
  "errorCode": "INTERNAL_ERROR",
  "message": "서버 오류가 발생했습니다."
}
```

---

## 11. Image API

이미지 업로드는 **클라이언트 → Spring 서버(multipart) → Storage** 순으로 처리됩니다.
스토리지 서비스 교체(Firebase Storage → AWS S3 등) 시 클라이언트 코드 변경 없이 **서버 구현체만 교체**하면 됩니다.

### 11.1 이미지 업로드

#### POST /images
이미지 파일을 서버를 통해 Storage에 업로드합니다.

**인증:** Firebase ID Token 필수

**Request:** `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `file` | File | O | 이미지 파일 (JPEG, PNG, WebP) |
| `context` | string | O | 업로드 맥락 (`POST_IMAGE` \| `PROFILE_IMAGE`) |

**제약 조건:**
- 최대 파일 크기: 10MB
- 허용 형식: JPEG, PNG, WebP

**썸네일(thumbUrl) 생성 규칙:**
- 서버가 업로드 시점에 원본 이미지를 리사이징하여 썸네일을 함께 생성
- 최대 너비 300px (높이는 비율 유지)
- 압축 품질: 80%
- 파일명: 원본 파일명에 `_thumb` 접미사 추가 (예: `image.jpg` → `image_thumb.jpg`)
- `StorageRepository` 구현체 내부에서 처리 (Firebase Storage / AWS S3 공통)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "url": "https://storage.googleapis.com/skuri-bucket/posts/uuid/image.jpg",
    "thumbUrl": "https://storage.googleapis.com/skuri-bucket/posts/uuid/image_thumb.jpg",
    "width": 800,
    "height": 600,
    "size": 204800
  }
}
```

**에러 코드:**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `IMAGE_TOO_LARGE` | 413 | 파일 크기 초과 (10MB 이상) |
| `IMAGE_INVALID_FORMAT` | 415 | 지원하지 않는 이미지 형식 |

---

### 11.2 이미지 업로드 플로우

#### 게시글 이미지

```
클라이언트
    │
    ├─ 1. POST /images (multipart, context=POST_IMAGE)
    │      └─ Response: { url, thumbUrl, width, height }   ← 이 값을 보관
    │         (이미지가 여러 장이면 반복 호출)
    │
    └─ 2. POST /posts
           {
             "title": "...",
             "content": "...",
             "images": [{ "url": "...", "thumbUrl": "...", "width": 800, "height": 600 }]
           }
```

#### 프로필 이미지

```
클라이언트
    │
    ├─ 1. POST /images (multipart, context=PROFILE_IMAGE)
    │      └─ Response: { url, ... }
    │
    └─ 2. PATCH /members/me
           { "photoUrl": "https://..." }
```

---

### 11.3 Storage 추상화 설계

Spring 서버는 `StorageRepository` 인터페이스로 스토리지 서비스를 추상화합니다.

```java
interface StorageRepository {
    UploadResult upload(String path, byte[] data, String contentType);
    void delete(String path);
}
```

스토리지 서비스 교체 시 구현체만 교체하면 됩니다.

| 현재 | 교체 가능 대상 |
|------|--------------|
| Firebase Storage | AWS S3, Google Cloud Storage, MinIO 등 |

---

## 12. Admin API

관리자(`isAdmin: true`)만 접근 가능한 API입니다.
모든 Admin API는 Firebase ID Token 인증 후 `isAdmin` 여부를 추가로 검증합니다.

```
인증 실패 시: 401 UNAUTHORIZED
isAdmin == false 시: 403 FORBIDDEN (ADMIN_REQUIRED)
```

> **기존 방식과의 차이:**
> 마이그레이션 전에는 `scripts/manage-app-notices.js` 등 Node.js 스크립트가
> Firebase Admin SDK를 통해 Firestore에 직접 write했습니다.
> 마이그레이션 후에는 해당 스크립트들이 이 Admin API를 호출하는 방식으로 교체됩니다.

---

### 12.1 앱 공지 관리

#### POST /admin/app-notices
앱 공지 생성

**Request:**
```json
{
  "title": "서버 점검 안내",
  "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
  "category": "MAINTENANCE",
  "priority": "HIGH",
  "imageUrls": [],
  "actionUrl": null,
  "publishedAt": "2026-02-20T00:00:00Z"
}
```

**category:** `UPDATE` | `MAINTENANCE` | `EVENT` | `GENERAL`
**priority:** `HIGH` | `NORMAL` | `LOW`

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "app_notice_uuid",
    "title": "서버 점검 안내",
    "createdAt": "2026-02-19T12:00:00Z"
  }
}
```

#### PUT /admin/app-notices/{appNoticeId}
앱 공지 수정

**Request:**
```json
{
  "title": "서버 점검 안내 (수정)",
  "content": "점검 시간이 변경되었습니다.",
  "priority": "HIGH"
}
```

#### DELETE /admin/app-notices/{appNoticeId}
앱 공지 삭제

---

### 12.2 앱 버전 관리

#### PUT /admin/app-versions/{platform}
앱 버전 정보 업데이트

**platform:** `ios` | `android`

**Request:**
```json
{
  "minimumVersion": "1.6.0",
  "forceUpdate": true,
  "title": "필수 업데이트 안내",
  "message": "안정성 개선을 위한 필수 업데이트입니다.",
  "showButton": true,
  "buttonText": "업데이트",
  "buttonUrl": "https://apps.apple.com/..."
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "platform": "ios",
    "minimumVersion": "1.6.0",
    "forceUpdate": true,
    "updatedAt": "2026-02-19T12:00:00Z"
  }
}
```

---

### 12.3 공개 채팅방 관리

공개 채팅방(UNIVERSITY, DEPARTMENT 등)은 사용자가 직접 생성할 수 없으며, 관리자만 생성/삭제합니다.
파티 채팅방은 파티 생성 시 서버 내부에서 자동으로 생성됩니다.

#### POST /admin/chat-rooms
공개 채팅방 생성

**Request:**
```json
{
  "name": "성결대 전체 채팅방",
  "type": "UNIVERSITY",
  "description": "성결대학교 학생들의 소통 공간",
  "isPublic": true
}
```

**type:** `UNIVERSITY` | `DEPARTMENT` | `GAME` | `CUSTOM`

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "room_uuid",
    "name": "성결대 전체 채팅방",
    "type": "UNIVERSITY"
  }
}
```

#### DELETE /admin/chat-rooms/{chatRoomId}
공개 채팅방 삭제

---

### 12.4 학교 공지 동기화

기존 `scripts/upload-notices.js`의 역할을 대체합니다.
학교 공지 크롤링 후 DB에 동기화합니다.

#### POST /admin/notices/sync
학교 공지 동기화 실행

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "created": 15,
    "updated": 3,
    "skipped": 42,
    "syncedAt": "2026-02-19T12:00:00Z"
  }
}
```

---

### 12.5 학식 메뉴 관리

#### POST /admin/cafeteria-menus
학식 메뉴 등록

**Request:**
```json
{
  "weekId": "2026-W08",
  "weekStart": "2026-02-16",
  "weekEnd": "2026-02-20",
  "menus": {
    "2026-02-16": {
      "rollNoodles": ["우동", "김밥"],
      "theBab": ["돈까스", "된장찌개"],
      "fryRice": ["볶음밥", "짜장면"]
    }
  }
}
```

#### PUT /admin/cafeteria-menus/{weekId}
학식 메뉴 수정

#### DELETE /admin/cafeteria-menus/{weekId}
학식 메뉴 삭제

---

### 12.6 학사 일정 관리

#### POST /admin/academic-schedules
학사 일정 추가

**Request:**
```json
{
  "title": "중간고사",
  "startDate": "2026-04-15",
  "endDate": "2026-04-21",
  "type": "MULTI",
  "isPrimary": true,
  "description": "2026학년도 1학기 중간고사"
}
```

**type:** `SINGLE` (단일 날짜) | `MULTI` (기간)

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "schedule_uuid",
    "title": "중간고사",
    "startDate": "2026-04-15",
    "endDate": "2026-04-21"
  }
}
```

#### PUT /admin/academic-schedules/{scheduleId}
학사 일정 수정

#### DELETE /admin/academic-schedules/{scheduleId}
학사 일정 삭제

---

### 12.7 강의 관리

#### POST /admin/courses/bulk
학기 강의 일괄 등록 (매 학기 강의 데이터 업로드)

**Request:**
```json
{
  "semester": "2026-1",
  "courses": [
    {
      "name": "소프트웨어공학",
      "professor": "홍길동",
      "department": "컴퓨터공학과",
      "credit": 3,
      "grade": 3,
      "schedule": [
        { "dayOfWeek": 1, "startPeriod": 1, "endPeriod": 2, "classroom": "공학관 301" },
        { "dayOfWeek": 3, "startPeriod": 1, "endPeriod": 2, "classroom": "공학관 301" }
      ]
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "semester": "2026-1",
    "created": 120,
    "updated": 5,
    "deleted": 3
  }
}
```

#### DELETE /admin/courses
학기 강의 전체 삭제

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 삭제할 학기 (예: `2026-1`) |

---

### 12.8 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `ADMIN_REQUIRED` | 403 | 관리자 권한 필요 |

---

## 참고

- [도메인 분석](./domain-analysis.md)
- [역할 정의](./role-definition.md)
- [ERD](./erd.md)
- [Firestore 데이터 구조](../firestore-data-structure.md)

---

> **문서 이력**
> - 2026-02-03: 초안 작성
> - 2026-02-03: SSE 명세 추가 (§10)
> - 2026-02-19: Image API 추가 (§11, 방식 A - 서버 경유 업로드)
> - 2026-02-19: 채팅 메시지 전송 경로 WebSocket으로 통일 (§4.3~4.5), Public API 목록 명확화 (§1.2)
> - 2026-02-19: Admin API 추가 (§12 — 앱 공지/버전 관리, 학교 공지 동기화, 학식/학사일정/강의 관리)
