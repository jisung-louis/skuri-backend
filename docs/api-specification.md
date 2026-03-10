# Spring 백엔드 API 명세

> 최종 수정일: 2026-03-09
> 관련 문서: [도메인 분석](./domain-analysis.md) | [ERD](./erd.md) | [Member 탈퇴 정책](./member-withdrawal-policy.md)

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

### 1.1-1 계약 소스 정책

- 런타임 API 계약의 최종 기준은 `/v3/api-docs`입니다.
- 본 문서(`docs/api-specification.md`)는 사람이 읽는 설명 문서이며, 코드 변경 PR에서 `/v3/api-docs`와 함께 동기화합니다.

### 1.2 인증

대부분의 API는 Firebase Authentication의 ID Token(JWT)을 사용한 Bearer 인증이 필요합니다.

```http
Authorization: Bearer <firebase_id_token>
```

**Public API (인증 불필요):**

비즈니스 API 기준으로는 아래 3개 API만 인증 없이 호출 가능합니다.  
추가로 API 문서 UI/스펙 조회 엔드포인트도 인증 없이 접근 가능합니다.

| API | 이유 |
|-----|------|
| `GET /v1/app-versions/{platform}` | 앱 실행 초기(로그인 전) 강제 업데이트 여부 확인 |
| `GET /v1/app-notices` | 로그인 전 점검 공지 / 긴급 공지 표시 필요 |
| `GET /v1/app-notices/{appNoticeId}` | 로그인 전 개별 점검/업데이트 공지 상세 표시 필요 |
| `GET /v3/api-docs/**` | OpenAPI 스펙(JSON) 조회 |
| `GET /swagger-ui/**`, `GET /swagger-ui.html` | Swagger UI 조회 |
| `GET /scalar/**` | Scalar UI 조회 |

### 1.3 공통 Response 형식

> 공통 응답은 `ApiResponse`의 `@JsonInclude(Include.NON_NULL)` 정책을 사용합니다.  
> 즉, `null` 값 필드는 직렬화 시 생략될 수 있습니다.

**성공 응답:**
```json
{
  "success": true,
  "data": { ... }
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
| 403 | `ADMIN_REQUIRED` | 관리자 권한 필요 |
| 403 | `MEMBER_WITHDRAWN` | 탈퇴한 회원 접근 차단 |
| 404 | `NOT_FOUND` | 리소스 없음 |
| 409 | `CONFLICT` | 충돌 (중복 등) |
| 409 | `WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED` | 탈퇴한 동일 UID 재가입 불가 |
| 409 | `MEMBER_WITHDRAWAL_NOT_ALLOWED` | 현재 상태상 회원 탈퇴 불가 |
| 409 | `RESOURCE_CONCURRENT_MODIFICATION` | 낙관적 락 기반 동시 수정 충돌 |
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

> 구현 상태 (2026-03-09):
> - 구현 완료: `POST /v1/members`, `GET /v1/members/me`, `PATCH /v1/members/me`, `PUT /v1/members/me/bank-account`, `PATCH /v1/members/me/notification-settings`, `DELETE /v1/members/me`, `GET /v1/members/{id}`, `POST/DELETE /v1/members/me/fcm-tokens`
> - 계정 라이프사이클 정책: [Member 탈퇴 정책](./member-withdrawal-policy.md)

### 2.1 인증 및 로그인 플로우

본 프로젝트는 **Firebase Authentication을 인증 주체로 유지**합니다.

- 로그인/토큰 발급/토큰 갱신(Refresh Token 포함)은 **클라이언트(RN) + Firebase Auth**가 담당합니다.
- Spring 백엔드는 클라이언트가 전달한 **Firebase ID Token(JWT)** 을 검증하여 인증을 처리합니다.
- 따라서 Spring 백엔드에는 `/auth/*` 형태의 “로그인/토큰 재발급” API가 존재하지 않습니다.

> 참고: 로그아웃은 클라이언트에서 Firebase Auth 로그아웃을 수행합니다.
> 서버는 푸시 알림을 위한 FCM 토큰 등록/해제 API만 제공합니다. (2.4 참고)

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
    │      ├─ POST /v1/members          ← 회원 레코드 생성
    │      └─ CompleteProfile 화면   ← 닉네임/학번/학과 입력
    │
    └─ isNewUser == false (기존 회원)
           ├─ GET /v1/members/me        ← last_login 갱신 + 프로필 조회
           └─ 홈 화면
```

> **주의:** Firebase의 `isNewUser`가 간헐적으로 부정확할 수 있습니다.
> 서버는 `POST /v1/members` 중복 호출(이미 존재하는 활성 uid)에 대해 **200 OK + 기존 프로필**을 반환하여 안전하게 처리합니다.
> 단, 이미 탈퇴 처리된 동일 uid는 `409 WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 반환하며 재활성화하지 않습니다.

**이메일 도메인 제한:**
모든 인증 API에서 Firebase ID Token의 `email`이 `@sungkyul.ac.kr`로 끝나지 않으면 `403`을 반환합니다.

```json
{
  "success": false,
  "errorCode": "EMAIL_DOMAIN_RESTRICTED",
  "message": "성결대학교 이메일(@sungkyul.ac.kr)만 사용 가능합니다."
}
```

**탈퇴 회원 접근 제한:**
- 보호 API에 탈퇴 회원 토큰이 들어오면 `403 MEMBER_WITHDRAWN`을 반환합니다.
- 예외적으로 `POST /v1/members`는 탈퇴한 동일 UID가 명시적인 `409 WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 받을 수 있도록 허용됩니다.

**로컬 Auth Emulator 정책:**
- `local-emulator` 프로필에서만 Auth Emulator 사용을 허용합니다.
- 필수 환경변수: `FIREBASE_AUTH_EMULATOR_HOST`, `FIREBASE_PROJECT_ID`
- `local-emulator`에서는 `FIREBASE_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`를 비워 두는 것을 기본값으로 봅니다. 실제 서비스 계정 파일은 필요하지 않습니다.
- `firebase.auth.use-emulator=false` 상태에서 emulator host가 설정되면 서버는 기동 실패합니다. (운영 오염 방지)
- IntelliJ `실행 전` 작업은 `/Users/jisung/skuri-backend/bin/start-firebase-auth-emulator.sh` 사용을 권장합니다.
  - 이 스크립트는 PID를 `/tmp/firebase-auth-emulator.pid`에 기록하고,
    서버 종료 시(`local-emulator` 프로필) 해당 PID의 Emulator를 자동 종료합니다.

```bash
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 \
FIREBASE_PROJECT_ID=sktaxi-acb4c \
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```

- 위 예시는 호스트에서 서버를 직접 실행할 때 기준이다.
- 기본 DB는 `localhost:3306`을 사용하며, 필요하면 `DB_URL` 환경변수로 Docker MySQL(`localhost:3307`) 같은 다른 포트를 덮어쓸 수 있다.
- Docker 컨테이너에서 emulator를 사용할 경우 `127.0.0.1` 대신 `host.docker.internal:9099` 같은 호스트 접근 주소를 사용해야 한다.

---

### 2.2 회원 가입

#### POST /v1/members
신규 회원 레코드 생성 (첫 로그인 시 1회 호출)

**인증:** Firebase ID Token 필수

Spring 서버 처리:
1. Firebase Admin SDK로 ID Token 검증 → `uid`, `email`, `sign_in_provider`, provider 계정 정보(`name`, `picture`, provider id) 추출
   - `google.com` provider를 강제하지 않는다.
   - `linked_accounts.provider`는 `GOOGLE`, `PASSWORD`, `UNKNOWN` 중 하나로 저장한다.
   - 소셜 로그인(`GOOGLE`)이 아닌 경우 `linked_accounts`는 `provider`를 제외한 provider 부가 컬럼(`provider_id`, `email`, `provider_display_name`, `photo_url`)을 `null`로 저장한다.
2. `members` 테이블에서 `uid` 조회
   - **없음 (신규)** → INSERT + `linked_accounts` INSERT → 201 Created
   - **있음 + ACTIVE (중복)** → 별도 처리 없이 기존 프로필 200 OK 반환 (idempotent)
   - **있음 + WITHDRAWN** → 409 Conflict (`WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`)

> 정책: 회원 생성 시 `members.photoUrl`은 `null`로 저장한다.  
> 소셜 계정 프로필 이미지(`picture`)는 `linked_accounts.photo_url`에만 저장한다.
> 회원 생성 시 `members.realname`은 provider 프로필 이름(`linked_accounts.provider_display_name`)으로 초기화한다.
>
> 용어 구분:
> - API 요청/응답의 `nickname` = `members.nickname` (앱 내 닉네임)
> - 소셜 계정 이름 = `linked_accounts.provider_display_name`

**Request:** (body 없음, ID Token만 사용)

**Response (신규 생성 — 201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "firebase_uid",
    "email": "user@sungkyul.ac.kr",
    "nickname": "스쿠리 유저",
    "studentId": null,
    "department": null,
    "realname": "홍길동",
    "photoUrl": null,
    "isAdmin": false,
    "bankAccount": null,
    "joinedAt": "2026-02-03T12:00:00Z"
  }
}
```

**Response (중복 호출 — 200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "firebase_uid",
    "email": "user@sungkyul.ac.kr",
    "nickname": "홍길동",
    "studentId": "20201234",
    "department": "컴퓨터공학과",
    "realname": "홍길동",
    "photoUrl": null,
    "isAdmin": false,
    "bankAccount": { ... },
    "joinedAt": "2024-03-01T00:00:00Z"
  }
}
```

**Response (탈퇴한 동일 UID 재가입 시도 — 409 Conflict):**
```json
{
  "success": false,
  "message": "탈퇴한 계정은 같은 인증 계정으로 재가입할 수 없습니다.",
  "errorCode": "WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED",
  "timestamp": "2026-03-09T12:00:00"
}
```

---

### 2.3 회원 정보

#### GET /v1/members/me
내 정보 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "user@example.com",
    "nickname": "홍길동",
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
      "commentNotifications": true,
      "bookmarkedPostCommentNotifications": true,
      "systemNotifications": true,
      "academicScheduleNotifications": true,
      "academicScheduleDayBeforeEnabled": true,
      "academicScheduleAllEventsEnabled": false,
      "noticeNotificationsDetail": {
        "news": true,
        "academy": true,
        "scholarship": false
      }
    },
    "joinedAt": "2024-03-01T00:00:00Z",
    "lastLogin": "2026-03-02T09:10:11Z"
  }
}
```

#### PATCH /v1/members/me
내 정보 수정

- 부분 업데이트 API입니다.
- 요청 본문에 포함되지 않은 필드는 기존 값을 유지합니다.
- `nickname`은 `members.nickname`을 수정합니다.
- `realname`은 회원 생성 시 provider 이름으로 초기화되며, 이 API로 수정할 수 없습니다.
- `photoUrl`은 현재 앱에서 미사용이며, 추후 프로필 이미지 기능에서 사용 예정입니다.

**Request:**
```json
{
  "nickname": "새닉네임",
  "studentId": "20201234",
  "department": "컴퓨터공학과",
  "photoUrl": "https://..."
}
```

#### PUT /v1/members/me/bank-account
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

#### PATCH /v1/members/me/notification-settings
알림 설정 수정

- 부분 업데이트 API입니다.
- 요청 본문에 포함되지 않은 알림 필드는 기존 값을 유지합니다.
- 런타임 기준으로 현재 지원되는 필드는 `allNotifications`, `partyNotifications`, `noticeNotifications`, `boardLikeNotifications`, `commentNotifications`, `bookmarkedPostCommentNotifications`, `systemNotifications`, `academicScheduleNotifications`, `academicScheduleDayBeforeEnabled`, `academicScheduleAllEventsEnabled`, `noticeNotificationsDetail`입니다.
- 학사 일정 알림 기본값은 `academicScheduleNotifications=true`, `academicScheduleDayBeforeEnabled=true`, `academicScheduleAllEventsEnabled=false`입니다.

**Request:**
```json
{
  "partyNotifications": true,
  "noticeNotifications": false,
  "commentNotifications": true,
  "bookmarkedPostCommentNotifications": true,
  "academicScheduleNotifications": true,
  "academicScheduleDayBeforeEnabled": true,
  "academicScheduleAllEventsEnabled": false,
  "noticeNotificationsDetail": {
    "news": true,
    "academy": false
  }
}
```

#### GET /v1/members/{id}
특정 회원 공개 프로필 조회

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "firebase_uid",
    "nickname": "홍길동",
    "department": "컴퓨터공학과",
    "photoUrl": "https://..."
  }
}
```

#### DELETE /v1/members/me
회원 탈퇴

- 즉시 탈퇴 정책을 사용합니다.
- `members` row는 soft delete tombstone으로 유지하고 `status=WITHDRAWN`, `withdrawnAt`를 기록합니다.
- 개인정보는 스크럽합니다.
  - `email`은 unique 충돌 방지용 placeholder로 치환
  - `studentId`, `department`, `photoUrl`, `realname`, 계좌 정보는 제거
  - `linked_accounts`, `user_notifications`, `fcm_tokens`, `user_timetables`는 삭제
- Board/Notice 콘텐츠는 보존하고 작성자 표시만 `탈퇴한 사용자`로 익명화합니다.
- TaxiParty는 리더 탈퇴 시 `WITHDRAWED`로 종료되며, 일반 멤버는 `ARRIVED` 파티 참여 중이면 탈퇴할 수 없습니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "회원 탈퇴가 완료되었습니다."
  }
}
```

**Response (탈퇴 불가 상태 — 409 Conflict):**
```json
{
  "success": false,
  "message": "현재 상태에서는 회원 탈퇴를 진행할 수 없습니다.",
  "errorCode": "MEMBER_WITHDRAWAL_NOT_ALLOWED",
  "timestamp": "2026-03-09T12:00:00"
}
```

### 2.4 FCM 토큰

> 구현 완료: Phase 8 (Notification 인프라)

#### POST /v1/members/me/fcm-tokens
FCM 토큰 등록

- 같은 토큰이 이미 존재하면 소유자/플랫폼을 현재 요청 기준으로 갱신합니다.
- 멀티 디바이스를 지원하며, 토큰별 unique 제약을 사용합니다.

**Request:**
```json
{
  "token": "fcm_device_token",
  "platform": "ios"
}
```

**Response:**
```json
{
  "success": true,
  "data": null
}
```

#### DELETE /v1/members/me/fcm-tokens
FCM 토큰 삭제

- 존재하지 않는 토큰이거나 본인 소유가 아니어도 성공 응답을 반환합니다.

**Request:**
```json
{
  "token": "fcm_device_token"
}
```

**Response:**
```json
{
  "success": true,
  "data": null
}
```

#### FCM Raw Push Payload Contract

> Spring 서버는 특정 RN legacy payload에 맞추지 않습니다. 푸시 payload의 `data.type`은 서버 canonical enum을 그대로 사용하고,
> 클라이언트는 명세 기준으로 `type + data`를 해석해 화면 이동을 결정해야 합니다.

- 공통 원칙:
  - `notification.title` / `notification.body`: OS 알림 UI에 표시할 텍스트
  - `data.contractVersion`: 현재 `"1"`
  - `data.type`: canonical `NotificationType` enum (`PARTY_*`, `CHAT_MESSAGE`, `NOTICE`, `APP_NOTICE`, `ACADEMIC_SCHEDULE` 등)
  - `data.*Id`: 클라이언트가 상세 화면 이동 대상을 식별하기 위한 리소스 ID
  - 서버는 RN route/screen 이름을 payload에 넣지 않습니다.

**공통 형태:**
```json
{
  "notification": {
    "title": "새 성결대 학사 공지",
    "body": "2026학년도 1학기 수강신청 정정 안내"
  },
  "data": {
    "contractVersion": "1",
    "type": "NOTICE",
    "noticeId": "notice-uuid"
  },
  "android": {
    "priority": "high",
    "notification": {
      "channelId": "notice_channel",
      "sound": "new_notice"
    }
  },
  "apns": {
    "payload": {
      "aps": {
        "sound": "new_notice.wav"
      }
    }
  }
}
```

**`data` 필드 키:**

| 키 | 설명 | nullable |
|---|---|---|
| `contractVersion` | push payload 계약 버전. 현재 `"1"` | X |
| `type` | canonical 알림 타입 enum | X |
| `partyId` | 파티 상세/파티 채팅 이동 식별자 | O |
| `requestId` | 동승 요청 식별자 | O |
| `chatRoomId` | 채팅방 식별자 | O |
| `postId` | 게시글 식별자 | O |
| `commentId` | 댓글 식별자 | O |
| `noticeId` | 학교 공지 식별자 | O |
| `appNoticeId` | 앱 공지 식별자 | O |
| `academicScheduleId` | 학사 일정 식별자 | O |

**프레젠테이션 프로필 (sound/channel):**

| 알림 그룹 | canonical type | Android | iOS |
|---|---|---|---|
| Party | `PARTY_CREATED`, `PARTY_JOIN_REQUEST`, `PARTY_JOIN_ACCEPTED`, `PARTY_JOIN_DECLINED`, `PARTY_CLOSED`, `PARTY_ARRIVED`, `PARTY_ENDED`, `MEMBER_KICKED`, `SETTLEMENT_COMPLETED` | `channelId=party_channel`, `sound=new_taxi_party` | `aps.sound=new_taxi_party.wav` |
| Chat | `CHAT_MESSAGE` | `channelId=chat_channel`, `sound=new_chat_notification` | `aps.sound=new_chat_notification.wav` |
| Notice | `NOTICE`, `APP_NOTICE`, `ACADEMIC_SCHEDULE` | `channelId=notice_channel`, `sound=new_notice` | `aps.sound=new_notice.wav` |
| Default | `POST_LIKED`, `COMMENT_CREATED` | 별도 channel/sound override 없음 (`priority=high`만 지정) | `aps.sound=default` |

- Android `party_channel`, `chat_channel`, `notice_channel`은 클라이언트가 미리 생성해둔 채널과 이름을 맞춰야 합니다.
- `Default` 그룹은 현재 서버가 별도 `channelId`를 강제하지 않습니다. 전용 기본 채널을 도입하려면 클라이언트와 채널 ID 계약을 함께 추가해야 합니다.

**논리적 이동 대상:**

| `type` | 필수 식별자 | 클라이언트가 여는 논리적 화면 |
|---|---|---|
| `PARTY_CREATED` | `partyId` | 파티 상세 또는 택시 탭 루트 |
| `PARTY_JOIN_REQUEST` | `partyId`, `requestId` | 동승 요청 확인 화면 |
| `PARTY_JOIN_ACCEPTED` / `PARTY_JOIN_DECLINED` | `partyId`, `requestId` | 파티 상세/요청 결과 화면 |
| `PARTY_CLOSED` / `PARTY_ARRIVED` / `PARTY_ENDED` / `MEMBER_KICKED` / `SETTLEMENT_COMPLETED` | `partyId` | 파티 상세 또는 파티 채팅 |
| `CHAT_MESSAGE` | `chatRoomId` | 채팅방 상세 |
| `POST_LIKED` | `postId` | 게시글 상세 |
| `COMMENT_CREATED` (게시글) | `postId`, `commentId` | 게시글 상세 + 댓글 포커스 |
| `COMMENT_CREATED` (공지) | `noticeId`, `commentId` | 공지 상세 + 댓글 포커스 |
| `NOTICE` | `noticeId` | 학교 공지 상세 |
| `APP_NOTICE` | `appNoticeId` | 앱 공지 상세 |
| `ACADEMIC_SCHEDULE` | `academicScheduleId` | 학사 일정 상세 |

**예시: `PARTY_JOIN_ACCEPTED`**
```json
{
  "notification": {
    "title": "동승 요청이 승인되었어요",
    "body": "파티에 합류하세요!"
  },
  "data": {
    "contractVersion": "1",
    "type": "PARTY_JOIN_ACCEPTED",
    "partyId": "party-uuid",
    "requestId": "request-uuid"
  },
  "android": {
    "priority": "high",
    "notification": {
      "channelId": "party_channel",
      "sound": "new_taxi_party"
    }
  },
  "apns": {
    "payload": {
      "aps": {
        "sound": "new_taxi_party.wav"
      }
    }
  }
}
```

**예시: `COMMENT_CREATED`**
```json
{
  "notification": {
    "title": "내 게시글에 댓글이 달렸어요",
    "body": "새 댓글"
  },
  "data": {
    "contractVersion": "1",
    "type": "COMMENT_CREATED",
    "postId": "post-uuid",
    "commentId": "comment-uuid"
  },
  "android": {
    "priority": "high"
  },
  "apns": {
    "payload": {
      "aps": {
        "sound": "default"
      }
    }
  }
}
```

### 2.5 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `EMAIL_DOMAIN_RESTRICTED` | 403 | `@sungkyul.ac.kr` 이메일이 아닌 계정으로 로그인 시도 |
| `MEMBER_NOT_FOUND` | 404 | 존재하지 않는 회원 |
| `BANK_ACCOUNT_NOT_FOUND` | 404 | 계좌 정보 미등록 상태에서 계좌 공유 시도 |

---

## 3. TaxiParty API

### 3.1 파티 조회

#### GET /v1/parties
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

#### GET /v1/parties/{partyId}
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
        "nickname": "홍길동",
        "photoUrl": "https://...",
        "isLeader": true,
        "joinedAt": "2026-02-03T12:00:00Z"
      },
      {
        "id": "member_uuid_2",
        "nickname": "김철수",
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

#### GET /v1/members/me/parties
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

#### POST /v1/parties
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

#### PATCH /v1/parties/{partyId}
파티 수정 (리더만)

- 수정 가능 상태: `OPEN`, `CLOSED`
- 수정 가능 필드(화이트리스트): `departureTime`, `detail`
- `maxMembers` 포함 또는 허용되지 않은 필드 포함 시 `422 VALIDATION_ERROR`
- `departureTime`, `detail` 모두 누락 시 `422 VALIDATION_ERROR`
- `departureTime`은 현재 이후 시각이어야 함 (`@Future`)
- `CLOSED` 상태에서 시간/상세 수정 시에도 상태는 `CLOSED` 유지 (`reopen`으로만 모집 재개)
- 동승 요청(`PENDING`)과 기존 참여 멤버는 자동 취소/재동의 처리 없이 유지
- 응답은 최신 파티 상세(`PartyDetailResponse`) 반환

**Request:**
```json
{
  "departureTime": "2026-02-03T14:30:00",
  "detail": "10분 후 출발합니다."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "party_uuid",
    "status": "OPEN",
    "departureTime": "2026-02-03T14:30:00",
    "detail": "10분 후 출발합니다."
  }
}
```

**에러 코드 (update 전용):**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `NOT_PARTY_LEADER` | 403 | 리더가 아닌 사용자가 수정 시도 |
| `INVALID_PARTY_STATE_TRANSITION` | 409 | OPEN/CLOSED 상태가 아닌 파티 수정 시도 |
| `VALIDATION_ERROR` | 422 | 허용되지 않은 필드 포함 또는 수정 필드 누락/형식 오류 |

#### PATCH /v1/parties/{partyId}/close
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

#### PATCH /v1/parties/{partyId}/reopen
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

#### PATCH /v1/parties/{partyId}/arrive
도착 및 정산 시작 (리더만)

- OPEN 또는 CLOSED 상태에서만 호출 가능
- 리더를 제외한 멤버가 1명 이상 있어야 함 (정산 대상이 없으면 호출 불가)
- `perPersonAmount = taxiFare / 정산대상인원` 정수 나눗셈(버림)으로 계산
- 정수 나눗셈으로 생기는 잔여 1원 단위 금액은 서버에서 자동 분배하지 않음

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

#### PATCH /v1/parties/{partyId}/settlement/members/{memberId}/confirm
멤버 정산 완료 표시 (리더만)

- 파티 리더가 특정 멤버의 정산 상태를 `settled=true`로 확정합니다.
- 앱 내 결제/송금 기능은 제공하지 않으며, 향후에도 제공하지 않습니다.
- 일반 멤버는 자신의 정산 상태를 직접 변경할 수 없고, 리더가 실제 입금 확인 후 호출합니다.
- 모든 멤버의 정산이 완료되면 `allSettled=true`를 반환하고 정산 상태만 `COMPLETED`로 변경됩니다.
- 파티 상태를 `ENDED`로 바꾸려면 리더가 별도로 `PATCH /v1/parties/{partyId}/end`를 호출해야 합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "memberId": "member_uuid",
    "settled": true,
    "settledAt": "2026-02-03T14:30:00Z",
    "allSettled": true
  }
}
```

> **`allSettled: true`** 는 "모든 정산 확인 완료"를 의미합니다.
> 이 시점에도 파티 상태는 `ARRIVED`를 유지하며, 종료는 리더의 `/end` 호출에서만 발생합니다.

#### PATCH /v1/parties/{partyId}/end
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

#### POST /v1/parties/{partyId}/cancel
파티 취소 (리더만)

- OPEN 또는 CLOSED 상태에서만 가능 (ARRIVED 상태 불가)
- 파티 상태 → `ENDED`, `endReason: CANCELLED`

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "status": "ENDED",
    "endReason": "CANCELLED"
  }
}
```

**에러 코드:**

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `PARTY_NOT_CANCELABLE` | 409 | ARRIVED 상태에서 취소 불가 |

### 3.3 멤버 관리

#### DELETE /v1/parties/{partyId}/members/me
파티 나가기 (멤버)

- 리더는 나가기 불가 (취소 또는 위임 불가 정책)
- ARRIVED 상태에서는 나가기 불가 (정산 진행/완료 여부와 무관)
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

#### DELETE /v1/parties/{partyId}/members/{memberId}
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

#### GET /v1/parties/{partyId}/join-requests
파티의 동승 요청 목록 (리더만)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "request_uuid",
      "partyId": "party_uuid",
      "requesterId": "user_uuid",
      "requesterName": "김철수",
      "requesterPhotoUrl": "https://...",
      "status": "PENDING",
      "createdAt": "2026-02-03T12:30:00Z"
    }
  ]
}
```

#### POST /v1/parties/{partyId}/join-requests
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

#### GET /v1/members/me/join-requests
내가 보낸 동승 요청 목록

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `status` | string | 요청 상태 (PENDING, ACCEPTED, DECLINED, CANCELED) |

#### PATCH /v1/join-requests/{requestId}/accept
동승 요청 수락 (리더만)

- 요청 수락으로 파티 인원이 정원(`maxMembers`)에 도달하면 파티 상태는 자동으로 `CLOSED` 전이됩니다.

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

#### PATCH /v1/join-requests/{requestId}/decline
동승 요청 거절 (리더만)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "request_uuid",
    "status": "DECLINED"
  }
}
```

#### PATCH /v1/join-requests/{requestId}/cancel
동승 요청 취소 (요청자)

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "request_uuid",
    "status": "CANCELED"
  }
}
```

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
| `ALREADY_SETTLED` | 이미 정산 완료 처리됨 |
| `PARTY_NOT_ARRIVABLE` | OPEN/CLOSED 상태가 아닌 파티 (arrive 호출 불가) |
| `PARTY_NOT_CANCELABLE` | ARRIVED 상태에서 취소 불가 |
| `NO_MEMBERS_TO_SETTLE` | 정산 대상 멤버 없음 (리더만 남은 파티) |
| `LEADER_CANNOT_LEAVE` | 리더는 파티 나가기 불가 |
| `CANNOT_LEAVE_ARRIVED_PARTY` | ARRIVED 상태에서 나가기 불가 |
| `CANNOT_KICK_IN_ARRIVED` | ARRIVED 상태에서 강퇴 불가 |
| `CANNOT_KICK_LEADER` | 리더 본인 강퇴 불가 |
| `INVALID_PARTY_STATE_TRANSITION` | 허용되지 않는 파티 상태 전이 |
| `PARTY_CONCURRENT_MODIFICATION` | 동시 요청 충돌 발생 |

---

## 4. Chat API

### 4.1 채팅방 조회

#### GET /v1/chat-rooms
접근 가능한 채팅방 목록

- 기본 정책: `공개 채팅방 + 내가 참여 중인 비공개 채팅방(PARTY 포함)`만 반환합니다.

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `type` | string | 채팅방 타입 (UNIVERSITY, DEPARTMENT, GAME, CUSTOM, PARTY) |
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
        "type": "TEXT",
        "text": "안녕하세요!",
        "senderName": "홍길동",
        "createdAt": "2026-02-03T12:00:00Z"
      },
      "unreadCount": 5,
      "isJoined": true
    }
  ]
}
```

#### GET /v1/chat-rooms/{chatRoomId}
채팅방 상세

- 비공개 채팅방은 멤버만 조회 가능합니다.

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
    "lastReadAt": "2026-02-03T11:00:00Z",
    "unreadCount": 5
  }
}
```

#### GET /v1/chat-rooms/{chatRoomId}/messages
채팅 메시지 조회

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `cursorCreatedAt` | datetime | 다음 페이지 시작 기준 createdAt (nullable, `cursorId`와 쌍) |
| `cursorId` | string | 다음 페이지 시작 기준 messageId (nullable, `cursorCreatedAt`와 쌍) |
| `size` | int | 페이지 크기 (기본 50, 최대 100) |

**정렬/커서 규칙:**
- 정렬은 `createdAt DESC, id DESC` 고정입니다.
- 다음 페이지 조회 조건은 아래와 같습니다.
  - `createdAt < cursorCreatedAt`
  - 또는 `createdAt == cursorCreatedAt AND id < cursorId`
- `nextCursor`는 현재 페이지의 마지막 메시지 `(createdAt, id)`로 생성됩니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": "message_uuid",
        "chatRoomId": "room_id",
        "senderId": "user_uuid",
        "senderName": "홍길동",
        "text": "안녕하세요!",
        "type": "TEXT",
        "createdAt": "2026-02-03T12:00:00Z"
      },
      {
        "id": "message_uuid_2",
        "chatRoomId": "room_id",
        "senderId": "user_uuid_2",
        "senderName": "시스템",
        "text": "홍길동님이 입장했습니다.",
        "type": "SYSTEM",
        "createdAt": "2026-02-03T11:59:00Z"
      }
    ],
    "hasNext": true,
    "nextCursor": {
      "createdAt": "2026-02-03T11:59:00Z",
      "id": "message_uuid_2"
    }
  }
}
```

### 4.2 채팅방 사용자 상태

#### PATCH /v1/chat-rooms/{chatRoomId}/settings
채팅방 설정 (음소거 등)

**Request:**
```json
{
  "muted": true
}
```

#### PATCH /v1/chat-rooms/{chatRoomId}/read
읽음 처리

- 클라이언트는 채팅방 포커스 획득/이탈, 앱 백그라운드 전환 시점마다 `lastReadAt`을 갱신합니다.
- 서버는 저장된 `lastReadAt`보다 과거 시각 요청을 무시해 단조 증가를 보장하고, 미래 시각 요청은 서버 현재 시각과 마지막 메시지 시각을 상한으로 clamp합니다.
- 미읽음 계산 기준은 `message.createdAt > lastReadAt` 입니다. (`==` 는 읽음으로 간주)

**Request:**
```json
{
  "lastReadAt": "2026-02-03T12:00:00Z"
}
```

### 4.3 메시지 전송 정책

> 채팅 메시지 **전송**은 WebSocket(STOMP)으로만 수행합니다. (§4.5 참고)

### 4.4 파티 채팅 (TaxiParty 도메인에서 관리)

파티 채팅 메시지 **전송**은 WebSocket(STOMP)으로만 수행합니다. (§4.5 참고)
파티 채팅 비즈니스 규칙(멤버 검증, 계좌 정보 조회 등)은 서버 내부 STOMP 핸들러에서 처리합니다.
- 파티 채팅 이력 조회는 `GET /v1/chat-rooms/{chatRoomId}/messages`를 사용합니다.
  - 예: `chatRoomId = party:{partyId}`

### 4.5 WebSocket (STOMP)

모든 채팅 메시지 **전송 및 실시간 수신**은 WebSocket(STOMP)을 통해 수행합니다.
채팅방 목록 화면은 방별 다중 구독이 아닌 **사용자 전용 요약 채널 1개**를 구독합니다.

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

#### 연결 후 인가(Authorization)

- `SEND /app/chat/{chatRoomId}`: 해당 채팅방 멤버만 전송 가능
- `SUBSCRIBE /topic/chat/{chatRoomId}`: 해당 채팅방 멤버만 구독 가능
- 비멤버 요청은 `NOT_CHAT_ROOM_MEMBER` 에러로 거부됩니다.

#### 구독 토폴로지 (중요)

- 채팅방 목록 화면: `SUBSCRIBE /user/queue/chat-rooms` 1개만 구독
- 채팅방 상세 화면: 진입한 방에 한해 `SUBSCRIBE /topic/chat/{chatRoomId}` 구독
- STOMP 에러 수신: `SUBSCRIBE /user/queue/errors`
- 상세 화면 이탈 시 해당 room topic 구독을 즉시 해제
- 모든 채팅방 topic을 동시에 구독하는 방식은 사용하지 않음

---

#### 일반 채팅

| 방향 | 경로 | 설명 |
|------|------|------|
| 수신 (Subscribe) | `/user/queue/chat-rooms` | 내 채팅방 목록 카드 요약(이름/인원/마지막 메시지/미읽음) 수신 |
| 전송 (Publish) | `/app/chat/{chatRoomId}` | 메시지 전송 |
| 수신 (Subscribe) | `/topic/chat/{chatRoomId}` | 실시간 메시지 수신 |

**전송 포맷:**
```json
{ "type": "TEXT", "text": "안녕하세요!" }
{ "type": "IMAGE", "imageUrl": "https://..." }
```

**채팅방 목록 요약 이벤트 포맷 (서버 → 클라이언트):**
```json
{
  "eventType": "CHAT_ROOM_UPSERT",
  "chatRoomId": "room_uuid",
  "name": "컴공 24학번",
  "memberCount": 42,
  "unreadCount": 3,
  "lastMessage": {
    "type": "TEXT",
    "text": "오늘 과제 어디까지 했어?",
    "senderName": "홍길동",
    "createdAt": "2026-02-03T12:00:00Z"
  },
  "updatedAt": "2026-02-03T12:00:00Z"
}
```

> `eventType`은 `CHAT_ROOM_SNAPSHOT`, `CHAT_ROOM_UPSERT`, `CHAT_ROOM_REMOVED`를 사용합니다.

---

#### 파티 채팅

- 파티 채팅도 동일 경로를 사용합니다.
  - 전송: `/app/chat/party:{partyId}`
  - 수신: `/topic/chat/party:{partyId}`
- 특수 메시지 타입: `ACCOUNT`, `ARRIVED`, `END`

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
  "chatRoomId": "party:party_uuid",
  "senderId": "user_uuid",
  "senderName": "홍길동",
  "type": "ACCOUNT",
  "accountData": {
    "bankName": "카카오뱅크",
    "accountNumber": "3333-01-1234567",
    "accountHolder": "홍길동"
  },
  "createdAt": "2026-02-03T12:00:00Z"
}
```

> STOMP 메시지 응답은 `ChatMessageResponse`의 `@JsonInclude(NON_NULL)` 정책을 따르므로, 타입별로 사용하지 않는 필드는 생략될 수 있습니다.

#### STOMP 에러 포맷 (서버 → 클라이언트)

```json
{
  "success": false,
  "errorCode": "NOT_CHAT_ROOM_MEMBER",
  "message": "채팅방 멤버가 아닙니다.",
  "timestamp": "2026-03-05T21:10:00"
}
```

### 4.6 트랜잭션 경계

| 작업 | 트랜잭션 범위 |
|------|------------|
| 메시지 전송 (STOMP 핸들러) | 메시지 DB 저장 + ChatRoom.messageCount 증가 → 커밋 후 구독자 브로드캐스트 |
| 채팅방 목록 요약 이벤트 | 메시지 저장/멤버수 변경 커밋 후 `/user/queue/chat-rooms`로 요약 이벤트 전송 |
| 읽음 처리 (`PATCH /v1/chat-rooms/{chatRoomId}/read`) | `lastReadAt` 단조 증가 갱신 + 미래 시각 clamp |
| 설정 수정 (`PATCH /v1/chat-rooms/{chatRoomId}/settings`) | ChatRoomMember.muted 갱신 |
| ACCOUNT 메시지 | 계좌 정보 DB 조회 + 메시지 DB 저장 → 커밋 후 브로드캐스트 |

> 브로드캐스트(WebSocket push)는 트랜잭션 커밋 성공 후 수행합니다. (트랜잭션 커밋 후 콜백)

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

### 5.1 구현 범위

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/v1/posts` | 게시글 작성 |
| `GET` | `/v1/posts` | 게시글 목록 조회 |
| `GET` | `/v1/posts/{postId}` | 게시글 상세 조회 (조회수 증가) |
| `PATCH` | `/v1/posts/{postId}` | 게시글 수정 (작성자) |
| `DELETE` | `/v1/posts/{postId}` | 게시글 삭제 (작성자, soft delete) |
| `POST` | `/v1/posts/{postId}/like` | 좋아요 등록 |
| `DELETE` | `/v1/posts/{postId}/like` | 좋아요 취소 |
| `POST` | `/v1/posts/{postId}/bookmark` | 북마크 등록 |
| `DELETE` | `/v1/posts/{postId}/bookmark` | 북마크 취소 |
| `GET` | `/v1/posts/bookmarked` | 내 북마크 게시글 목록 |
| `GET` | `/v1/posts/{postId}/comments` | 댓글 목록 조회 (flat list, 무제한 depth) |
| `POST` | `/v1/posts/{postId}/comments` | 댓글/대댓글 작성 |
| `PATCH` | `/v1/comments/{commentId}` | 댓글 수정 (작성자) |
| `DELETE` | `/v1/comments/{commentId}` | 댓글 삭제 (작성자, placeholder soft delete) |
| `GET` | `/v1/members/me/posts` | 내가 작성한 게시글 목록 |
| `GET` | `/v1/members/me/bookmarks` | 내가 북마크한 게시글 목록 |

### 5.2 게시글 목록/상세

#### GET /v1/posts

**Query Parameters**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `category` | string | `GENERAL`, `QUESTION`, `REVIEW`, `ANNOUNCEMENT` |
| `search` | string | 제목/본문 검색 |
| `authorId` | string | 특정 작성자 필터 |
| `sort` | string | `latest`, `popular`, `mostCommented`, `mostViewed` |
| `page` | number | 기본 0 |
| `size` | number | 기본 20 (1~100) |

**Response**

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
    "totalElements": 100,
    "totalPages": 5,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### GET /v1/posts/{postId}

- 조회 시 `viewCount`를 서버에서 +1 동기화한다.

### 5.3 게시글 작성/수정/삭제

#### POST /v1/posts

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
      "height": 600,
      "size": 245123,
      "mime": "image/jpeg"
    }
  ]
}
```

#### PATCH /v1/posts/{postId}

```json
{
  "title": "수정된 제목",
  "content": "수정된 내용",
  "category": "QUESTION"
}
```

#### DELETE /v1/posts/{postId}

- `posts.is_deleted=true`로 soft delete 처리한다.
- 삭제된 게시글은 목록/상세/북마크 조회에서 제외한다.

### 5.4 상호작용(좋아요/북마크)

- 단일 테이블 `post_interactions`(`user_id`,`post_id`)에서 `isLiked`, `isBookmarked`를 함께 관리한다.
- 카운트(`likeCount`, `bookmarkCount`)는 같은 트랜잭션에서 동기화한다.

#### POST /v1/posts/{postId}/like
#### DELETE /v1/posts/{postId}/like
#### POST /v1/posts/{postId}/bookmark
#### DELETE /v1/posts/{postId}/bookmark

### 5.5 댓글 정책

- 댓글은 `parentId` self-reference 기반으로 무제한 대댓글을 허용한다.
- 익명 규칙:
  - `anonId = "{postId}:{userId}"`
  - 게시글 단위로 기존 `anonId`가 있으면 기존 `anonymousOrder` 재사용
  - 없으면 `max(anonymousOrder)+1` 부여
  - 삭제 후에도 순번 재계산 없음

#### GET /v1/posts/{postId}/comments

**부모 삭제 정책(B)**

- 부모 댓글 삭제 시 하드 삭제하지 않고 `isDeleted=true`, `content="삭제된 댓글입니다"`로 placeholder 처리
- 자식 댓글은 유지한다.
- 조회 응답은 flat list를 반환한다.
- 각 댓글은 최소 `id`, `parentId`, `depth`, `createdAt`, `updatedAt`, `isDeleted`를 포함한다.
- 서버는 thread 순서를 보장한 flat list를 반환하고, 클라이언트가 트리 UI를 조립한다.

#### POST /v1/posts/{postId}/comments

```json
{
  "content": "댓글 내용",
  "isAnonymous": false,
  "parentId": null
}
```

```json
{
  "content": "대댓글 내용",
  "isAnonymous": true,
  "parentId": "parent_comment_uuid"
}
```

#### PATCH /v1/comments/{commentId}
#### DELETE /v1/comments/{commentId}

### 5.6 내 게시글/북마크

#### GET /v1/members/me/posts

- 내 작성글 페이징 조회.

#### GET /v1/members/me/bookmarks

- 내 북마크 페이징 조회.

### 5.7 에러 코드

| 에러 코드 | HTTP | 설명 |
|---|---|---|
| `POST_NOT_FOUND` | 404 | 게시글 없음 |
| `COMMENT_NOT_FOUND` | 404 | 댓글 없음 |
| `NOT_POST_AUTHOR` | 403 | 게시글 작성자만 수정/삭제 가능 |
| `NOT_COMMENT_AUTHOR` | 403 | 댓글 작성자만 수정/삭제 가능 |
| `COMMENT_ALREADY_DELETED` | 409 | 이미 삭제된 댓글 수정/삭제 시도 |

---

## 6. Notice API

### 6.1 학교 공지

#### GET /v1/notices
공지사항 목록

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `category` | string | 카테고리 (새소식, 학사, 학생 등) |
| `search` | string | 제목/요약 검색 |
| `page` | number | 페이지 번호 (기본 0) |
| `size` | number | 페이지 크기 (기본 20, 최대 100) |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "notice_id",
        "title": "2026학년도 1학기 수강신청 안내",
        "rssPreview": "수강신청 일정, 대상 학년, 유의사항을 안내합니다.",
        "category": "학사",
        "department": "성결대학교",
        "author": "교무처",
        "postedAt": "2026-02-01T00:00:00",
        "viewCount": 500,
        "likeCount": 10,
        "commentCount": 10,
        "isRead": true,
        "isLiked": false
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1000,
    "totalPages": 50,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

**검증 규칙:**
- `category`는 14개 공지 카테고리만 허용한다.
- `page < 0` 또는 `size < 1 || size > 100`이면 `422 VALIDATION_ERROR`
- `rssPreview`는 RSS의 `description/content/contentSnippet` fallback으로 수집한 미리보기 텍스트다.
- `rssPreview`는 RSS 길이 제한 때문에 잘린 텍스트일 수 있으며, AI 요약이 아니다.

#### GET /v1/notices/{noticeId}
공지사항 상세

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "notice_id",
    "title": "2026학년도 1학기 수강신청 안내",
    "rssPreview": "RSS 미리보기",
    "bodyHtml": "<html>...</html>",
    "link": "https://www.sungkyul.ac.kr/...",
    "category": "학사",
    "department": "성결대학교",
    "author": "교무처",
    "source": "RSS",
    "postedAt": "2026-02-01T00:00:00",
    "viewCount": 501,
    "likeCount": 11,
    "commentCount": 10,
    "attachments": [
      {
        "name": "수강신청 안내.pdf",
        "downloadUrl": "https://...",
        "previewUrl": "https://..."
      }
    ],
    "isRead": true,
    "isLiked": true
  }
}
```

**정책:**
- `rssPreview`는 RSS에서 받은 미리보기 텍스트이며, 일부 공지는 원문 2~3줄 수준에서 잘려 들어올 수 있다.
- `bodyHtml`은 상세 페이지에서 크롤링한 HTML 원문이며, 클라이언트가 공지 웹 구조(`h*`, `table`, `br` 등)를 최대한 유지해서 렌더링할 수 있도록 그대로 저장한다.
- `bodyText`는 `bodyHtml`에서 태그를 제거해 정규화한 내부 저장용 텍스트이며, 검색/AI 요약/RAG 용도로 사용한다. 현재 공개 API에는 노출하지 않는다.
- `summary` 컬럼은 추후 AI 생성 공지 요약을 저장하기 위한 예약 필드이며, 현재 공개 API에는 노출하지 않는다.
- 상세 조회는 조회수만 증가시키며, 읽음 상태를 저장하지 않는다.
- 읽음 상태 저장은 `POST /v1/notices/{noticeId}/read`로만 처리한다.

#### POST /v1/notices/{noticeId}/read
읽음 표시

**Response:**
```json
{
  "success": true,
  "data": {
    "noticeId": "notice_id",
    "isRead": true,
    "readAt": "2026-02-01T12:34:56"
  }
}
```

#### POST /v1/notices/{noticeId}/like
공지 좋아요 등록

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

#### DELETE /v1/notices/{noticeId}/like
공지 좋아요 취소

**Response:**
```json
{
  "success": true,
  "data": {
    "isLiked": false,
    "likeCount": 10
  }
}
```

### 6.2 공지 댓글

#### GET /v1/notices/{noticeId}/comments
공지 댓글 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "notice_comment_uuid",
      "parentId": null,
      "depth": 0,
      "content": "댓글 내용",
      "authorId": "user_uuid",
      "authorName": "홍길동",
      "isAnonymous": false,
      "anonymousOrder": null,
      "isAuthor": true,
      "isDeleted": false,
      "createdAt": "2026-02-03T12:00:00",
      "updatedAt": "2026-02-03T12:00:00"
    },
    {
      "id": "notice_reply_uuid",
      "parentId": "notice_comment_uuid",
      "depth": 1,
      "content": "대댓글 내용",
      "authorId": null,
      "authorName": "익명2",
      "isAnonymous": true,
      "anonymousOrder": 2,
      "isAuthor": false,
      "isDeleted": false,
      "createdAt": "2026-02-03T12:10:00",
      "updatedAt": "2026-02-03T12:10:00"
    }
  ]
}
```

#### POST /v1/notices/{noticeId}/comments
공지 댓글 작성

**Request:**
```json
{
  "content": "댓글 내용",
  "isAnonymous": false,
  "parentId": null
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": "notice_comment_uuid",
    "parentId": null,
    "depth": 0,
    "content": "댓글 내용",
    "authorId": "user_uuid",
    "authorName": "홍길동",
    "isAnonymous": false,
    "anonymousOrder": null,
    "isAuthor": true,
    "isDeleted": false,
    "createdAt": "2026-02-03T12:00:00",
    "updatedAt": "2026-02-03T12:00:00"
  }
}
```

**댓글 정책:**
- 댓글은 `parentId` self-reference 기반으로 무제한 대댓글을 허용한다.
- 익명 규칙:
  - `anonId = "{noticeId}:{userId}"`
  - 공지 단위로 기존 `anonId`가 있으면 기존 `anonymousOrder` 재사용
  - 없으면 `max(anonymousOrder)+1` 부여
  - 삭제 후에도 순번 재계산 없음
- 부모 댓글 삭제 시 하드 삭제하지 않고 `isDeleted=true`, `content="삭제된 댓글입니다"` placeholder 처리하며 자식은 유지한다.
- 조회 응답은 flat list를 반환한다.
- 각 댓글은 최소 `id`, `parentId`, `depth`, `createdAt`, `updatedAt`, `isDeleted`를 포함한다.
- 서버는 thread 순서를 보장한 flat list를 반환하고, 클라이언트가 트리 UI를 조립한다.

#### DELETE /v1/notice-comments/{commentId}
공지 댓글 삭제

### 6.3 앱 공지

#### GET /v1/app-notices
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
      "publishedAt": "2026-02-01T00:00:00Z",
      "createdAt": "2026-01-31T18:00:00Z",
      "updatedAt": "2026-01-31T18:00:00Z"
    }
  ]
}
```

#### GET /v1/app-notices/{appNoticeId}
앱 공지 상세

**정책:**
- `GET /v1/app-notices`
- `GET /v1/app-notices/{appNoticeId}`
위 두 API는 모두 Public API이며 인증이 필요 없다.
- Public 조회는 `publishedAt <= now()`인 앱 공지만 노출한다.

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "app_notice_uuid",
    "title": "서버 점검 안내",
    "content": "2월 20일 새벽 2시~4시 서버 점검이 있습니다.",
    "category": "MAINTENANCE",
    "priority": "HIGH",
    "imageUrls": [],
    "actionUrl": null,
    "publishedAt": "2026-02-20T00:00:00Z",
    "createdAt": "2026-02-19T12:00:00Z",
    "updatedAt": "2026-02-19T12:00:00Z"
  }
}
```

### 6.4 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `NOTICE_NOT_FOUND` | 404 | 존재하지 않는 학교 공지 |
| `APP_NOTICE_NOT_FOUND` | 404 | 존재하지 않는 앱 공지 |
| `NOTICE_COMMENT_NOT_FOUND` | 404 | 존재하지 않는 공지 댓글 |
| `NOT_NOTICE_COMMENT_AUTHOR` | 403 | 댓글 작성자가 아닌데 삭제 시도 |
| `COMMENT_ALREADY_DELETED` | 409 | 이미 삭제된 댓글 재삭제 시도 |
| `RESOURCE_CONCURRENT_MODIFICATION` | 409 | 공지 동기화가 이미 진행 중임 |

---

## 7. Academic API

### 7.1 강의

#### GET /v1/courses
강의 목록

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 학기 (예: `2026-1`, 미지정 시 전체 학기 검색) |
| `department` | string | 학과 |
| `professor` | string | 교수명 |
| `search` | string | 강의명/과목코드/카테고리/교수/강의실/비고 키워드 검색 |
| `dayOfWeek` | int | 요일 (1-5) |
| `grade` | int | 학년 |
| `page` | int | 페이지 번호 (기본: 0) |
| `size` | int | 페이지 크기 (기본: 20, 최대: 100) |

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "course_uuid",
        "semester": "2026-1",
        "code": "01255",
        "division": "001",
        "name": "민법총칙",
        "credits": 3,
        "professor": "문상혁",
        "department": "법학과",
        "grade": 2,
        "category": "전공선택",
        "location": "영401",
        "note": null,
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
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 500,
    "totalPages": 25,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

### 7.2 시간표

#### GET /v1/timetables/my
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
    "courseCount": 1,
    "totalCredits": 3,
    "courses": [
      {
        "id": "course_uuid",
        "code": "01255",
        "division": "001",
        "name": "민법총칙",
        "professor": "문상혁",
        "location": "영401",
        "category": "전공선택",
        "credits": 3,
        "schedule": [
          { "dayOfWeek": 1, "startPeriod": 3, "endPeriod": 4 }
        ]
      }
    ],
    "slots": [
      {
        "courseId": "course_uuid",
        "courseName": "민법총칙",
        "code": "01255",
        "dayOfWeek": 1,
        "startPeriod": 3,
        "endPeriod": 4,
        "professor": "문상혁",
        "location": "영401"
      }
    ]
  }
}
```

시간표가 아직 생성되지 않은 경우에도 `200 OK`를 반환하며, `id`는 `null`, `courses`/`slots`는 빈 배열로 내려간다.
`semester`를 생략하면 서버는 현재 날짜 기준 `2~7월 -> yyyy-1`, `8~12월 -> yyyy-2`, `1월 -> 전년도 yyyy-2` 규칙으로 학기를 계산한다.
성결대학교 실제 학기 시작은 3월/9월이지만, 스쿠리는 수강신청과 시간표 준비 수요를 반영해 한 달 앞선 2월/8월부터 새 학기를 사용한다.

#### POST /v1/timetables/my/courses
시간표에 강의 추가

**Request:**
```json
{
  "courseId": "course_uuid",
  "semester": "2026-1"
}
```

**Response (200 OK):**

`GET /v1/timetables/my`와 동일한 형태의 최신 시간표를 반환한다.

#### DELETE /v1/timetables/my/courses/{courseId}
시간표에서 강의 삭제

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 학기 (필수) |

**Response (200 OK):**

`GET /v1/timetables/my`와 동일한 형태의 최신 시간표를 반환한다.

### 7.3 학사 일정

#### GET /v1/academic-schedules
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

**구현 예정 알림 정책 (Phase 8):**
- 기본 리마인더 기준일은 `startDate`
- 기본 발송 시각은 당일 오전 `09:00`
- 기본 대상은 중요 일정(`isPrimary = true`)
- 사용자 옵션으로 전날 오전 `09:00` 추가와 모든 일정 대상 확장을 허용

### 7.4 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `COURSE_NOT_FOUND` | 404 | 존재하지 않는 강의 |
| `ACADEMIC_SCHEDULE_NOT_FOUND` | 404 | 존재하지 않는 학사 일정 |
| `TIMETABLE_CONFLICT` | 409 | 시간표 추가 시 기존 강의와 시간 충돌 |
| `COURSE_ALREADY_IN_TIMETABLE` | 409 | 이미 시간표에 추가된 강의 중복 추가 |
| `CONFLICT` | 409 | 관리자 강의 bulk 처리 중 동시 저장 충돌 |

---

## 8. Support API

### 8.1 문의

#### POST /v1/inquiries
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

#### GET /v1/inquiries/my
내 문의 목록

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": "inquiry_uuid",
      "type": "BUG",
      "subject": "앱 오류 문의",
      "content": "채팅 화면에서 오류가 발생합니다.",
      "status": "PENDING",
      "createdAt": "2026-02-03T12:00:00Z",
      "updatedAt": "2026-02-03T12:00:00Z"
    }
  ]
}
```

### 8.2 신고

#### POST /v1/reports
신고 등록

**targetType:** `POST` | `COMMENT` | `MEMBER`

**Request:**
```json
{
  "targetType": "POST",
  "targetId": "post_uuid",
  "category": "SPAM",
  "reason": "광고성 게시글입니다."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "report_uuid",
    "status": "PENDING",
    "createdAt": "2026-03-05T12:10:00Z"
  }
}
```

### 8.3 앱 버전

#### GET /v1/app-versions/{platform}
앱 버전 정보 (Public API)

- 저장된 버전 정보가 없으면 기본 응답으로 `minimumVersion=1.0.0`, `forceUpdate=false`, `showButton=false`를 반환합니다.

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

#### GET /v1/cafeteria-menus
학식 메뉴

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `date` | date | 조회 날짜 (기본: 오늘, Asia/Seoul 기준) |

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

#### GET /v1/cafeteria-menus/{weekId}
특정 주차 학식 메뉴

**Path Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `weekId` | string | 조회할 주차 ID (예: `2026-W06`) |

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
| `CAFETERIA_MENU_NOT_FOUND` | 404 | 존재하지 않는 학식 메뉴 |
| `CAFETERIA_MENU_ALREADY_EXISTS` | 409 | 이미 등록된 주차의 학식 메뉴 |
| `REPORT_NOT_FOUND` | 404 | 존재하지 않는 신고 |
| `REPORT_ALREADY_SUBMITTED` | 409 | 동일 대상에 대한 중복 신고 |
| `INVALID_INQUIRY_STATUS_TRANSITION` | 409 | 허용되지 않는 문의 상태 전이 |
| `INVALID_REPORT_STATUS_TRANSITION` | 409 | 허용되지 않는 신고 상태 전이 |
| `CANNOT_REPORT_YOURSELF` | 400 | 자기 자신을 신고 시도 |

---

## 9. Notification API

### 9.1 알림 조회

#### GET /v1/notifications
알림 목록

**Query Parameters:**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 0부터 시작하는 페이지 번호 |
| `size` | int | 20 | 페이지 크기 (`1~100`, 100 초과 시 100으로 clamp) |
| `unreadOnly` | boolean | false | `true`면 읽지 않은 알림만 조회 |

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
          "partyId": "party_uuid",
          "requestId": "join_request_uuid"
        },
        "isRead": false,
        "createdAt": "2026-02-03T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false,
    "unreadCount": 5
  }
}
```

#### GET /v1/notifications/unread-count
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

#### POST /v1/notifications/{notificationId}/read
알림 읽음 처리

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "notification_uuid",
    "type": "PARTY_JOIN_ACCEPTED",
    "title": "동승 요청이 승인되었어요",
    "message": "파티에 합류하세요!",
    "data": {
      "partyId": "party_uuid",
      "requestId": "join_request_uuid"
    },
    "isRead": true,
    "createdAt": "2026-02-03T12:00:00Z"
  }
}
```

#### POST /v1/notifications/read-all
모든 알림 읽음 처리

**Response:**
```json
{
  "success": true,
  "data": {
    "updatedCount": 3,
    "unreadCount": 0
  }
}
```

#### DELETE /v1/notifications/{notificationId}
알림 삭제

**Response:**
```json
{
  "success": true,
  "data": null
}
```

### 9.3 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `NOTIFICATION_NOT_FOUND` | 404 | 존재하지 않는 알림 |
| `NOT_NOTIFICATION_OWNER` | 403 | 다른 사람의 알림 접근 시도 |

### 9.4 알림 정책 및 저장 모델

> Phase 8 Spring Notification 인프라는 `references/index.ts`의 Cloud Functions 운영 정책을 기준으로 parity를 맞추되,
> 런타임 계약의 canonical enum/DTO는 Spring API 기준으로 정렬합니다.

- 저장 모델:
  - 인앱 인박스: `user_notifications`
  - FCM 토큰: `fcm_tokens`
  - 레퍼런스의 Firestore(`users/{uid}.fcmTokens[]`, `userNotifications/...`)는 운영 정책 비교용 참고 자료입니다.
- 이벤트 발행 시점:
  - 상태 변경 성공 후 `after-commit`으로 `ApplicationEventPublisher`에 전달합니다.
  - 알림 저장/푸시 실패는 핵심 트랜잭션을 롤백시키지 않습니다.

| 알림 타입 | 기준 트리거 | 수신 대상 | 설정 반영 | 인앱 인박스 |
|----------|-------------|-----------|-----------|-------------|
| `PARTY_CREATED` | 새 파티 생성 | 생성자 제외 전체 사용자 | `allNotifications` + `partyNotifications` | X |
| `PARTY_JOIN_REQUEST` | 동승 요청 생성 | 파티 리더 | `allNotifications` + `partyNotifications` | O |
| `PARTY_JOIN_ACCEPTED` / `PARTY_JOIN_DECLINED` | 요청 상태 변경 | 요청자 | `allNotifications` + `partyNotifications` | O |
| `PARTY_CLOSED` / `PARTY_ARRIVED` | 파티 상태 변경 | 리더 제외 파티 멤버 | `allNotifications` + `partyNotifications` | `PARTY_CLOSED`: X / `PARTY_ARRIVED`: O |
| `SETTLEMENT_COMPLETED` | 마지막 정산 완료 | 파티 전체 멤버 | `allNotifications` + `partyNotifications` | O |
| `MEMBER_KICKED` | 강퇴 감지 | 강퇴된 멤버 | `allNotifications` + `partyNotifications` | O |
| `PARTY_ENDED` | 파티 해체 | 리더 제외 파티 멤버 | `allNotifications` + `partyNotifications` | O |
| `CHAT_MESSAGE` (공개 채팅) | 공개 채팅방 메시지 | 채팅방 멤버(송신자 제외) | `allNotifications` + 채팅방 mute | X |
| `CHAT_MESSAGE` (파티 채팅) | 파티 채팅 메시지 | 파티 멤버(송신자 제외) | 채팅 mute 중심 parity 우선, 전역 토글은 현재 미반영 | X |
| `POST_LIKED` | 게시글 좋아요 | 게시글 작성자 | `allNotifications` + `boardLikeNotifications` | O |
| `COMMENT_CREATED` (게시글) | 댓글/답글 생성 | 게시글 작성자, 부모 댓글 작성자, 게시글 북마크 사용자 | `allNotifications` + `commentNotifications` + `bookmarkedPostCommentNotifications` (중복 수신자는 1회 dedupe) | O |
| `COMMENT_CREATED` (공지) | 공지 댓글 답글 생성 | 부모 댓글 작성자 | `allNotifications` + `commentNotifications` | O |
| `NOTICE` | 새 학교 공지 | 공지 허용 사용자 | `allNotifications` + `noticeNotifications` + `noticeNotificationsDetail` | O |
| `APP_NOTICE` | 앱 공지 생성 | 일반: 시스템 알림 허용 사용자 / `HIGH`: 전체 사용자 | 일반: `allNotifications` + `systemNotifications`, `HIGH`는 설정 무시 | O |
| `ACADEMIC_SCHEDULE` | 학사 일정 리마인더 | 학사 일정 알림 허용 사용자 | `allNotifications` + `academicScheduleNotifications` | O |

- 공지 댓글은 현재 `Notice.author`가 회원 식별자가 아닌 문자열이므로, 루트 댓글 작성자(공지 작성자) 알림은 런타임에서 보장하지 않습니다. 현재 구현은 부모 댓글 작성자 대상 답글 알림만 발송합니다.
- 학사 일정 리마인더는 `AcademicScheduleReminder` 도메인 이벤트 기반으로 처리합니다.
- 기본 정책:
  - 기준일: `AcademicSchedule.startDate`
  - 시각: 오전 `09:00 Asia/Seoul`
  - 대상: 중요 일정(`isPrimary = true`)
  - 멀티데이 일정은 `startDate`를 기준으로 계산
- 사용자 옵션:
  - 일정 전날 오전 `09:00` 추가
  - 중요 일정만이 아니라 모든 일정 대상으로 확장
- 사용자 설정 필드:
  - `academicScheduleNotifications`
  - `academicScheduleDayBeforeEnabled`
  - `academicScheduleAllEventsEnabled`

---

## 10. SSE (Server-Sent Events)

> `role-definition.md` §8.2 기준:
> 파티 목록/상태, 알림, 게시물 목록/조회수는 SSE를 통해 실시간 업데이트를 제공한다.
> 채팅 실시간 통신은 SSE 대상이 아니며, §4.5 WebSocket(STOMP) 경로를 사용한다.

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
data: {"id":"uuid","leaderId":"uuid","leaderName":"홍길동","leaderPhotoUrl":null,"departure":{"name":"성결대학교","lat":37.382742,"lng":126.928031},"destination":{"name":"안양역","lat":37.401234,"lng":126.922345},"departureTime":"2026-02-03T14:00:00","maxMembers":4,"currentMembers":1,"tags":["빠른출발"],"detail":"정문 앞에서 출발","status":"OPEN","createdAt":"2026-02-03T12:00:00"}
retry: 3000
```

**OpenAPI/Scalar 예시 표기 전략**

- SSE `200` 응답은 `stream_full`(연속 이벤트 흐름) 1개와 이벤트별 단건 예시를 함께 제공한다.
- 이벤트별 단건 예시는 `event` 이름 단위로 분리한다. (`SNAPSHOT`, `HEARTBEAT`, 도메인 이벤트)
- 각 예시의 `data` 구조는 런타임 발행 payload와 동일해야 한다.

### 10.3 파티 실시간 구독

#### GET /v1/sse/parties
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
| `SNAPSHOT` | 연결 직후/재연결 직후 현재 상태 스냅샷 전송 | 연결 사용자 |
| `PARTY_CREATED` | 새 파티 생성됨 | 전체 연결 사용자 |
| `PARTY_UPDATED` | 파티 정보 수정됨 | 전체 연결 사용자 |
| `PARTY_STATUS_CHANGED` | 파티 상태 변경됨 | 전체 연결 사용자 |
| `PARTY_DELETED` | 파티 삭제/취소됨 | 전체 연결 사용자 |
| `PARTY_MEMBER_JOINED` | 멤버 합류 | 해당 파티 멤버 |
| `PARTY_MEMBER_LEFT` | 멤버 나감/강퇴 | 해당 파티 멤버 |
| `HEARTBEAT` | 30초 주기 연결 유지 | 전체 연결 사용자 |

**RN 파싱 기준 DTO (strict)**

```ts
type PartyLocationPayload = {
  name: string;
  lat: number;
  lng: number;
};

type PartySummaryPayload = {
  id: string;
  leaderId: string;
  leaderName: string | null;
  leaderPhotoUrl: string | null;
  departure: PartyLocationPayload;
  destination: PartyLocationPayload;
  departureTime: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss" (KST 기준)
  maxMembers: number;
  currentMembers: number;
  tags: string[] | null;
  detail: string | null;
  status: "OPEN" | "CLOSED" | "ARRIVED" | "ENDED";
  createdAt: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};

type PartyStatusChangedPayload = {
  id: string;
  status: "OPEN" | "CLOSED" | "ARRIVED" | "ENDED";
  currentMembers: number;
  endReason?: "ARRIVED" | "FORCE_ENDED" | "CANCELLED" | "TIMEOUT" | "WITHDRAWED";
};

type PartyMemberJoinedPayload = {
  partyId: string;
  memberId: string;
  memberName: string | null;
  currentMembers: number;
};

type PartyMemberLeftPayload = {
  partyId: string;
  memberId: string;
  reason: "LEFT" | "KICKED";
  currentMembers: number;
};

type SnapshotPayload = {
  parties: PartySummaryPayload[];
};

type HeartbeatPayload = {
  timestamp: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};
```

> `PARTY_CREATED`, `PARTY_UPDATED`, `SNAPSHOT.parties[*]`는 동일한 `PartySummaryPayload` 스키마를 사용합니다.

**이벤트 데이터 형식:**

```
// SNAPSHOT
event: SNAPSHOT
data: {
  "parties": [
    {
      "id": "party_uuid",
      "leaderId": "user_uuid",
      "leaderName": "홍길동",
      "leaderPhotoUrl": null,
      "departure": {
        "name": "성결대학교",
        "lat": 37.382742,
        "lng": 126.928031
      },
      "destination": {
        "name": "안양역",
        "lat": 37.401234,
        "lng": 126.922345
      },
      "departureTime": "2026-02-03T14:00:00",
      "maxMembers": 4,
      "currentMembers": 1,
      "tags": ["빠른출발"],
      "detail": null,
      "status": "OPEN",
      "createdAt": "2026-02-03T12:00:00"
    }
  ]
}

// PARTY_CREATED
event: PARTY_CREATED
data: {
  "id": "party_uuid",
  "leaderId": "user_uuid",
  "leaderName": "홍길동",
  "leaderPhotoUrl": "https://example.com/profile.jpg",
  "departure": {
    "name": "성결대학교",
    "lat": 37.382742,
    "lng": 126.928031
  },
  "destination": {
    "name": "안양역",
    "lat": 37.401234,
    "lng": 126.922345
  },
  "departureTime": "2026-02-03T14:00:00",
  "maxMembers": 4,
  "currentMembers": 1,
  "tags": ["빠른출발"],
  "detail": "정문 앞에서 출발",
  "status": "OPEN",
  "createdAt": "2026-02-03T12:00:00"
}

// PARTY_UPDATED
event: PARTY_UPDATED
data: {
  "id": "party_uuid",
  "leaderId": "user_uuid",
  "leaderName": "홍길동",
  "leaderPhotoUrl": "https://example.com/profile.jpg",
  "departure": {
    "name": "성결대학교",
    "lat": 37.382742,
    "lng": 126.928031
  },
  "destination": {
    "name": "안양역",
    "lat": 37.401234,
    "lng": 126.922345
  },
  "departureTime": "2026-02-03T14:30:00",
  "maxMembers": 4,
  "currentMembers": 2,
  "tags": ["빠른출발"],
  "detail": "10분 후 출발",
  "status": "OPEN",
  "createdAt": "2026-02-03T12:00:00"
}

// PARTY_STATUS_CHANGED
event: PARTY_STATUS_CHANGED
data: {
  "id": "party_uuid",
  "status": "CLOSED",
  "currentMembers": 3
}

// PARTY_STATUS_CHANGED (종료 시)
event: PARTY_STATUS_CHANGED
data: {
  "id": "party_uuid",
  "status": "ENDED",
  "currentMembers": 3,
  "endReason": "TIMEOUT"
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
  "reason": "KICKED",
  "currentMembers": 1
}

// HEARTBEAT
event: HEARTBEAT
data: {"timestamp": "2026-02-03T12:00:30"}
```

#### GET /v1/sse/parties/{partyId}/join-requests
특정 파티의 동승 요청 목록/상태를 실시간으로 구독합니다.

> 도입 목적: 파티 리더 화면에서 동승 요청 목록 UI를 자동 갱신

**인증:** Firebase ID Token 필수 (파티 리더만 구독 가능)

**Headers:**
```http
Authorization: Bearer <firebase_id_token>
Accept: text/event-stream
Cache-Control: no-cache
```

**구독 이벤트 목록:**

| 이벤트 타입 | 발생 시점 | 수신 대상 |
|-----------|---------|---------|
| `SNAPSHOT` | 연결 직후/재연결 직후 현재 요청 목록 스냅샷 전송 | 연결 사용자(리더) |
| `JOIN_REQUEST_CREATED` | 해당 파티에 신규 동승 요청 생성 | 해당 파티 리더 |
| `JOIN_REQUEST_UPDATED` | 동승 요청 상태 변경 (ACCEPTED/DECLINED/CANCELED) | 해당 파티 리더 |
| `HEARTBEAT` | 30초 주기 연결 유지 | 연결 사용자(리더) |

**권한/예외:**

| errorCode | HTTP | 설명 |
|----------|------|------|
| `NOT_PARTY_LEADER` | 403 | 파티 리더가 아닌 사용자의 구독 요청 |
| `PARTY_NOT_FOUND` | 404 | 존재하지 않는 파티 |

**RN 파싱 기준 DTO (strict)**

```ts
type JoinRequestSsePayload = {
  id: string;
  partyId: string;
  requesterId: string;
  requesterName: string | null;
  requesterPhotoUrl: string | null;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELED";
  createdAt: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};

type PartyJoinRequestSnapshotPayload = {
  partyId: string;
  requests: JoinRequestSsePayload[];
};
```

**이벤트 데이터 형식:**

```
// SNAPSHOT
event: SNAPSHOT
data: {
  "partyId": "party_uuid",
  "requests": [
    {
      "id": "request_uuid",
      "partyId": "party_uuid",
      "requesterId": "user_uuid",
      "requesterName": "김철수",
      "requesterPhotoUrl": null,
      "status": "PENDING",
      "createdAt": "2026-02-03T12:00:00"
    }
  ]
}

// JOIN_REQUEST_CREATED
event: JOIN_REQUEST_CREATED
data: {
  "id": "request_uuid",
  "partyId": "party_uuid",
  "requesterId": "user_uuid",
  "requesterName": "김철수",
  "requesterPhotoUrl": null,
  "status": "PENDING",
  "createdAt": "2026-02-03T12:00:00"
}

// JOIN_REQUEST_UPDATED
event: JOIN_REQUEST_UPDATED
data: {
  "id": "request_uuid",
  "partyId": "party_uuid",
  "requesterId": "user_uuid",
  "requesterName": "김철수",
  "requesterPhotoUrl": null,
  "status": "ACCEPTED",
  "createdAt": "2026-02-03T12:00:00"
}
```

#### GET /v1/sse/members/me/join-requests
내 동승 요청 목록/상태를 실시간으로 구독합니다.

> 도입 목적: 요청자 화면에서 수락/거절/취소 상태를 자동 갱신

**인증:** Firebase ID Token 필수 (본인 요청만 수신)

**Headers:**
```http
Authorization: Bearer <firebase_id_token>
Accept: text/event-stream
Cache-Control: no-cache
```

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `status` | string | 필터 (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELED`) |

> `status` 필터는 `SNAPSHOT`에 즉시 적용됩니다.  
> `MY_JOIN_REQUEST_UPDATED`는 상태 전이 전/후 중 하나라도 필터와 일치하면 전송됩니다(목록 제거/추가 동기화 목적).

**구독 이벤트 목록:**

| 이벤트 타입 | 발생 시점 |
|-----------|---------|
| `SNAPSHOT` | 연결 직후/재연결 직후 현재 요청 목록 스냅샷 전송 |
| `MY_JOIN_REQUEST_CREATED` | 내 신규 동승 요청 생성 |
| `MY_JOIN_REQUEST_UPDATED` | 내 동승 요청 상태 변경 |
| `HEARTBEAT` | 30초 주기 연결 유지 |

**RN 파싱 기준 DTO (strict)**

```ts
type MyJoinRequestSsePayload = {
  id: string;
  partyId: string;
  requesterId: string;
  requesterName: string | null;
  requesterPhotoUrl: string | null;
  status: "PENDING" | "ACCEPTED" | "DECLINED" | "CANCELED";
  createdAt: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};

type MyJoinRequestSnapshotPayload = {
  requests: MyJoinRequestSsePayload[];
};
```

**이벤트 데이터 형식:**

```
// SNAPSHOT
event: SNAPSHOT
data: {
  "requests": [
    {
      "id": "request_uuid",
      "partyId": "party_uuid",
      "requesterId": "user_uuid",
      "requesterName": "김철수",
      "requesterPhotoUrl": null,
      "status": "PENDING",
      "createdAt": "2026-02-03T12:00:00"
    }
  ]
}

// MY_JOIN_REQUEST_UPDATED
event: MY_JOIN_REQUEST_UPDATED
data: {
  "id": "request_uuid",
  "partyId": "party_uuid",
  "requesterId": "user_uuid",
  "requesterName": "김철수",
  "requesterPhotoUrl": null,
  "status": "DECLINED",
  "createdAt": "2026-02-03T12:00:00"
}
```

### 10.4 알림 실시간 구독

#### GET /v1/sse/notifications
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
| `SNAPSHOT` | 연결 직후/재연결 직후 현재 상태 스냅샷 전송 |
| `NOTIFICATION` | 새 알림 도착 (모든 알림 타입 공통) |
| `UNREAD_COUNT_CHANGED` | 읽지 않은 알림 수 변경 |
| `HEARTBEAT` | 30초 주기 연결 유지 |

**RN 파싱 기준 DTO (strict)**

```ts
type NotificationDataPayload = {
  partyId?: string;
  requestId?: string;
  chatRoomId?: string;
  postId?: string;
  commentId?: string;
  noticeId?: string;
  appNoticeId?: string;
  academicScheduleId?: string;
};

type NotificationPayload = {
  id: string;
  type:
    | "PARTY_CREATED"
    | "PARTY_JOIN_REQUEST"
    | "PARTY_JOIN_ACCEPTED"
    | "PARTY_JOIN_DECLINED"
    | "PARTY_CLOSED"
    | "PARTY_ARRIVED"
    | "PARTY_ENDED"
    | "MEMBER_KICKED"
    | "SETTLEMENT_COMPLETED"
    | "CHAT_MESSAGE"
    | "POST_LIKED"
    | "COMMENT_CREATED"
    | "NOTICE"
    | "APP_NOTICE"
    | "ACADEMIC_SCHEDULE";
  title: string;
  message: string;
  data: NotificationDataPayload;
  isRead: boolean;
  createdAt: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};

type UnreadCountChangedPayload = {
  count: number;
};

type NotificationSnapshotPayload = {
  unreadCount: number;
};

type NotificationHeartbeatPayload = {
  timestamp: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};
```

**이벤트 데이터 형식:**

```
// SNAPSHOT
event: SNAPSHOT
data: {
  "unreadCount": 3
}

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
  "createdAt": "2026-02-03T12:00:00"
}

// NOTIFICATION (학사 일정)
event: NOTIFICATION
data: {
  "id": "notification_uuid",
  "type": "ACADEMIC_SCHEDULE",
  "title": "학사 일정 리마인더",
  "message": "수강신청 일정이 오늘 시작돼요.",
  "data": {
    "academicScheduleId": "academic_schedule_uuid"
  },
  "isRead": false,
  "createdAt": "2026-03-08T09:00:00"
}

// UNREAD_COUNT_CHANGED
event: UNREAD_COUNT_CHANGED
data: {
  "count": 3
}

// HEARTBEAT
event: HEARTBEAT
data: {
  "timestamp": "2026-02-03T12:00:30"
}
```

**알림 타입 → 이동 경로:**

| 알림 타입 | data 필드 | 이동 경로 |
|---------|---------|---------|
| `PARTY_CREATED` | `partyId` | 파티 목록 |
| `PARTY_JOIN_REQUEST` | `partyId`, `requestId` | 동승 요청 목록 |
| `PARTY_JOIN_ACCEPTED` | `partyId`, `requestId` | 파티 상세 |
| `PARTY_JOIN_DECLINED` | `partyId`, `requestId` | 파티 목록 |
| `PARTY_CLOSED` | `partyId` | 파티 상세 |
| `PARTY_ARRIVED` | `partyId` | 파티 상세 (정산) |
| `PARTY_ENDED` | `partyId` | 파티 상세 |
| `MEMBER_KICKED` | `partyId` | 파티 목록 |
| `SETTLEMENT_COMPLETED` | `partyId` | 파티 상세 |
| `CHAT_MESSAGE` | `chatRoomId` | 채팅방 |
| `POST_LIKED` | `postId` | 게시글 상세 |
| `COMMENT_CREATED` (게시글) | `postId`, `commentId` | 게시글 상세 |
| `COMMENT_CREATED` (공지) | `noticeId`, `commentId` | 공지 상세 |
| `NOTICE` | `noticeId` | 공지 상세 |
| `APP_NOTICE` | `appNoticeId` | 앱 공지 상세 |
| `ACADEMIC_SCHEDULE` | `academicScheduleId` | 학사 일정 상세 |

### 10.5 게시물 실시간 구독

#### GET /v1/sse/posts
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
| `SNAPSHOT` | 연결 직후/재연결 직후 현재 상태 스냅샷 전송 |
| `POST_CREATED` | 새 게시글 작성됨 |
| `POST_UPDATED` | 게시글 수정됨 |
| `POST_DELETED` | 게시글 삭제됨 |
| `POST_VIEW_COUNT_UPDATED` | 조회수 변경됨 (5 단위 배치 업데이트) |
| `HEARTBEAT` | 30초 주기 연결 유지 |

**RN 파싱 기준 DTO (strict)**

```ts
type PostSummaryPayload = {
  id: string;
  title: string;
  authorName: string | null;
  isAnonymous: boolean;
  category: "GENERAL" | "QUESTION" | "REVIEW" | "ANNOUNCEMENT";
  createdAt: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};

type PostDeletedPayload = {
  id: string;
};

type PostViewCountUpdatedPayload = {
  id: string;
  viewCount: number;
};

type PostSnapshotPayload = {
  posts: PostSummaryPayload[];
};

type PostHeartbeatPayload = {
  timestamp: string; // LocalDateTime, "yyyy-MM-dd'T'HH:mm:ss"
};
```

**이벤트 데이터 형식:**

```
// SNAPSHOT
event: SNAPSHOT
data: {
  "posts": [
    {
      "id": "post_uuid",
      "title": "게시글 제목",
      "authorName": "홍길동",
      "isAnonymous": false,
      "category": "GENERAL",
      "createdAt": "2026-02-03T12:00:00"
    }
  ]
}

// POST_CREATED
event: POST_CREATED
data: {
  "id": "post_uuid",
  "title": "게시글 제목",
  "authorName": "홍길동",
  "isAnonymous": false,
  "category": "GENERAL",
  "createdAt": "2026-02-03T12:00:00"
}

// POST_UPDATED
event: POST_UPDATED
data: {
  "id": "post_uuid",
  "title": "수정된 게시글 제목",
  "authorName": "홍길동",
  "isAnonymous": false,
  "category": "GENERAL",
  "createdAt": "2026-02-03T12:00:00"
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

// HEARTBEAT
event: HEARTBEAT
data: {
  "timestamp": "2026-02-03T12:00:30"
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
// 파티 목록 SSE (/v1/sse/parties) 재연결 시
event: SNAPSHOT
data: {
  "parties": [ /* 현재 파티 목록 전체 */ ]
}

// 알림 SSE (/v1/sse/notifications) 재연결 시
event: SNAPSHOT
data: {
  "unreadCount": 3
}

// 게시물 SSE (/v1/sse/posts) 재연결 시
event: SNAPSHOT
data: {
  "posts": [
    {
      "id": "post_uuid",
      "title": "게시글 제목",
      "authorName": "홍길동",
      "isAnonymous": false,
      "category": "GENERAL",
      "createdAt": "2026-02-03T12:00:00"
    }
  ]
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

#### POST /v1/images
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
    ├─ 1. POST /v1/images (multipart, context=POST_IMAGE)
    │      └─ Response: { url, thumbUrl, width, height }   ← 이 값을 보관
    │         (이미지가 여러 장이면 반복 호출)
    │
    └─ 2. POST /v1/posts
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
    ├─ 1. POST /v1/images (multipart, context=PROFILE_IMAGE)
    │      └─ Response: { url, ... }
    │
    └─ 2. PATCH /v1/members/me
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

#### POST /v1/admin/app-notices
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

#### PATCH /v1/admin/app-notices/{appNoticeId}
앱 공지 부분 수정

- 전달한 필드만 반영한다.
- 누락된 필드와 `null` 필드는 변경하지 않는다.
- 모든 수정 가능 필드를 보내면 전체 수정처럼 사용할 수 있다.

**Request:**
```json
{
  "title": "서버 점검 안내 (수정)",
  "content": "점검 시간이 변경되었습니다.",
  "priority": "HIGH"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "app_notice_uuid",
    "title": "서버 점검 안내 (수정)",
    "content": "점검 시간이 변경되었습니다.",
    "category": "MAINTENANCE",
    "priority": "HIGH",
    "imageUrls": [],
    "actionUrl": null,
    "publishedAt": "2026-02-20T00:00:00Z",
    "createdAt": "2026-02-19T12:00:00Z",
    "updatedAt": "2026-02-19T12:30:00Z"
  }
}
```

#### DELETE /v1/admin/app-notices/{appNoticeId}
앱 공지 삭제

**Response (200 OK):**
```json
{
  "success": true,
  "data": null
}
```

---

### 12.2 앱 버전 관리

#### PUT /v1/admin/app-versions/{platform}
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

#### POST /v1/admin/chat-rooms
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

**type:** `UNIVERSITY` | `DEPARTMENT` | `GAME` | `CUSTOM` | `PARTY`

**검증 규칙:**
- `type=PARTY`는 허용되지 않습니다. (`400 INVALID_REQUEST`)
- `isPublic=true`만 허용됩니다. (`400 INVALID_REQUEST`)

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

#### DELETE /v1/admin/chat-rooms/{chatRoomId}
공개 채팅방 삭제

**검증 규칙:**
- 존재하지 않는 채팅방: `404 CHAT_ROOM_NOT_FOUND`
- `PARTY` 타입 삭제 시도: `400 INVALID_REQUEST`
- 비공개 채팅방 삭제 시도: `400 INVALID_REQUEST`

**Response (200 OK):**
```json
{
  "success": true,
  "data": null
}
```

---

### 12.4 학교 공지 동기화

기존 `scripts/upload-notices.js`의 역할을 대체합니다.
학교 공지 크롤링 후 DB에 동기화합니다.

#### POST /v1/admin/notices/sync
학교 공지 동기화 실행

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "created": 15,
    "updated": 3,
    "skipped": 42,
    "failed": 2,
    "syncedAt": "2026-02-19T12:00:00"
  }
}
```

**예외:**
- 동기화가 이미 진행 중이면 `409 RESOURCE_CONCURRENT_MODIFICATION`

**운영 정책:**
- 개별 공지 저장 실패는 전체 동기화를 중단하지 않고 `failed`로 집계한 뒤 다음 공지 처리를 계속합니다.

---

### 12.5 학식 메뉴 관리

#### POST /v1/admin/cafeteria-menus
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

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
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
}
```

#### PUT /v1/admin/cafeteria-menus/{weekId}
학식 메뉴 수정

**Request / Response:** `POST /v1/admin/cafeteria-menus`와 동일한 필드를 사용하며, 성공 시 `200 OK`로 전체 학식 메뉴 객체를 반환한다.

#### DELETE /v1/admin/cafeteria-menus/{weekId}
학식 메뉴 삭제

**Response (200 OK):**
```json
{
  "success": true,
  "data": null
}
```

---

### 12.6 학사 일정 관리

#### POST /v1/admin/academic-schedules
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
    "endDate": "2026-04-21",
    "type": "MULTI",
    "isPrimary": true,
    "description": "2026학년도 1학기 중간고사"
  }
}
```

#### PUT /v1/admin/academic-schedules/{scheduleId}
학사 일정 수정

**Request / Response:** `POST /v1/admin/academic-schedules`와 동일한 필드를 사용하며, 성공 시 `200 OK`로 전체 학사 일정 객체를 반환한다.

#### DELETE /v1/admin/academic-schedules/{scheduleId}
학사 일정 삭제

**Response (200 OK):**
```json
{
  "success": true,
  "data": null
}
```

---

### 12.7 강의 관리

#### POST /v1/admin/courses/bulk
학기 강의 일괄 등록 (매 학기 강의 데이터 업로드)

`semester + code + division` 조합을 기준으로 업서트하며, 같은 학기에 기존에 존재하지만 이번 요청에 없는 강의는 삭제한다.
동시 업로드 등으로 유니크 충돌이 발생하면 `409 CONFLICT`를 반환한다.

**Request:**
```json
{
  "semester": "2026-1",
  "courses": [
    {
      "code": "CSE301",
      "division": "001",
      "name": "소프트웨어공학",
      "professor": "홍길동",
      "department": "컴퓨터공학과",
      "credits": 3,
      "grade": 3,
      "category": "전공필수",
      "location": "공학관 301",
      "note": null,
      "schedule": [
        { "dayOfWeek": 1, "startPeriod": 1, "endPeriod": 2 },
        { "dayOfWeek": 3, "startPeriod": 1, "endPeriod": 2 }
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

#### DELETE /v1/admin/courses
학기 강의 전체 삭제

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `semester` | string | 삭제할 학기 (필수, 예: `2026-1`) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "semester": "2026-1",
    "created": 0,
    "updated": 0,
    "deleted": 125
  }
}
```

---

### 12.8 운영 문의/신고 관리

#### GET /v1/admin/inquiries
문의 전체 목록 조회 (관리자)

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `status` | string | 문의 상태 필터 (`PENDING`, `IN_PROGRESS`, `RESOLVED`) |
| `page` | int | 페이지 번호 (기본 0, 0 이상) |
| `size` | int | 페이지 크기 (기본 20, 1~100) |

- `page < 0` 또는 `size < 1 || size > 100`이면 `422 VALIDATION_ERROR`
- `status`가 enum 범위를 벗어나면 `400 INVALID_REQUEST`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "inquiry_uuid",
        "memberId": "user_uuid",
        "type": "BUG",
        "subject": "채팅 화면 오류",
        "content": "채팅 진입 시 앱이 종료됩니다.",
        "status": "PENDING",
        "memo": null,
        "userEmail": "user@sungkyul.ac.kr",
        "userName": "스쿠리유저",
        "userRealname": "홍길동",
        "userStudentId": "20201234",
        "createdAt": "2026-03-05T12:00:00Z",
        "updatedAt": "2026-03-05T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 53,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

#### PATCH /v1/admin/inquiries/{inquiryId}/status
문의 상태 처리 (관리자)

**Request:**
```json
{
  "status": "RESOLVED",
  "memo": "재현 후 수정 배포 완료"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "inquiry_uuid",
    "memberId": "user_uuid",
    "type": "BUG",
    "subject": "채팅 화면 오류",
    "content": "채팅 진입 시 앱이 종료됩니다.",
    "status": "RESOLVED",
    "memo": "재현 후 수정 배포 완료",
    "userEmail": "user@sungkyul.ac.kr",
    "userName": "스쿠리유저",
    "userRealname": "홍길동",
    "userStudentId": "20201234",
    "createdAt": "2026-03-05T12:00:00Z",
    "updatedAt": "2026-03-05T12:30:00Z"
  }
}
```

#### GET /v1/admin/reports
신고 전체 목록 조회 (관리자)

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `status` | string | 신고 상태 필터 (`PENDING`, `REVIEWING`, `ACTIONED`, `REJECTED`) |
| `targetType` | string | 신고 대상 필터 (`POST`, `COMMENT`, `MEMBER`) |
| `page` | int | 페이지 번호 (기본 0, 0 이상) |
| `size` | int | 페이지 크기 (기본 20, 1~100) |

- `page < 0` 또는 `size < 1 || size > 100`이면 `422 VALIDATION_ERROR`
- `status`, `targetType`이 enum 범위를 벗어나면 `400 INVALID_REQUEST`

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "report_uuid",
        "reporterId": "user_uuid",
        "targetType": "POST",
        "targetId": "post_uuid",
        "targetAuthorId": "target_user_uuid",
        "category": "SPAM",
        "reason": "광고성 게시글입니다.",
        "status": "PENDING",
        "action": null,
        "memo": null,
        "createdAt": "2026-03-05T12:10:00Z",
        "updatedAt": "2026-03-05T12:10:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 18,
    "totalPages": 1,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

#### PATCH /v1/admin/reports/{reportId}/status
신고 상태 처리 (관리자)

**Request:**
```json
{
  "status": "ACTIONED",
  "action": "DELETE_POST",
  "memo": "광고성 게시물 삭제 및 사용자 경고"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": "report_uuid",
    "reporterId": "user_uuid",
    "targetType": "POST",
    "targetId": "post_uuid",
    "targetAuthorId": "target_user_uuid",
    "category": "SPAM",
    "reason": "광고성 게시글입니다.",
    "status": "ACTIONED",
    "action": "DELETE_POST",
    "memo": "광고성 게시물 삭제 및 사용자 경고",
    "createdAt": "2026-03-05T12:10:00Z",
    "updatedAt": "2026-03-05T12:20:00Z"
  }
}
```

---

### 12.9 에러 코드

| 에러 코드 | HTTP | 설명 |
|----------|------|------|
| `ADMIN_REQUIRED` | 403 | 관리자 권한 필요 |

---

> 변경 이력
> - 2026-03-07: Support API 계약 동기화
>   - `Report.targetType`를 `POST | COMMENT | MEMBER`로 통일
>   - `Report.status`를 `PENDING | REVIEWING | ACTIONED | REJECTED`로 통일
>   - Admin 문의/신고 목록 응답을 `PageResponse` 전체 필드(`totalPages`, `hasNext`, `hasPrevious`)와 일치하도록 보정
>   - 앱 버전/학식 메뉴 관리자 응답 예시 보강

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
> - 2026-03-05: Phase 3 구현 반영 — 채팅 커서 페이지네이션(`cursorCreatedAt`,`cursorId`) 명시, `lastReadAt` 단조 증가/미읽음 경계(`createdAt > lastReadAt`) 확정, STOMP 경로를 `/app/chat/{chatRoomId}`·`/topic/chat/{chatRoomId}`로 동기화
> - 2026-03-05: Chat 계약 동기화 — `lastMessage.createdAt`/`accountData` 필드로 통일, 비공개 채팅방 접근 정책(REST/WS) 및 STOMP 에러 포맷(`/user/queue/errors`) 명시
> - 2026-03-05: Chat 명세 보완 — 채팅방 요약 `lastMessage.senderName` 예시 추가, STOMP 메시지 `NON_NULL` 직렬화 정책 명시
> - 2026-03-05: Support API 보완 — `GET /v1/cafeteria-menus/{weekId}` 명시 추가
> - 2026-03-05: Admin Support API 추가 — 문의/신고 운영 조회·처리 (`GET/PATCH /v1/admin/inquiries*`, `GET/PATCH /v1/admin/reports*`)
> - 2026-03-05: Admin 권한 정책 반영 — `ROLE_ADMIN + @PreAuthorize` 기반 접근 제어와 `ADMIN_REQUIRED` 에러코드 명시, 공개 채팅방 Admin API 검증 규칙 보강
> - 2026-03-05: Board 계약 동기화 — 댓글 depth 1 제한, 부모 삭제 정책(B: placeholder soft delete), `/v1/members/me/posts|bookmarks` 및 Board 에러코드(`COMMENT_DEPTH_EXCEEDED`, `COMMENT_ALREADY_DELETED`) 반영
> - 2026-03-07: Board/Notice 공통 Comment 정책 구현 반영 — 무제한 depth, flat list 응답, `commentNotifications` / `bookmarkedPostCommentNotifications` 계약 반영
> - 2026-03-08: Phase 8 Notification 계약 반영 — `PARTY_*` canonical enum 정렬, Notification API pagination/FCM token/SSE strict DTO(`ACADEMIC_SCHEDULE`, `academicScheduleId`) 동기화, 학사 일정 알림 설정 필드 추가
