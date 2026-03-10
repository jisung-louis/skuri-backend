

# 백엔드 역할 정의서 v1.0
(Firebase Serverless → Spring Migration)

> 최종 수정일: 2026-03-10
> 관련 문서: [도메인 분석](./domain-analysis.md) | [API 명세](./api-specification.md) | [구현 로드맵](./implementation-roadmap.md) | [Member 탈퇴 정책](./member-withdrawal-policy.md)

## 1. 문서 목적

본 문서는 Firebase 기반 서버리스 백엔드를 사용 중인 현재 시스템을  
Spring 기반 백엔드로 점진적으로 마이그레이션하기에 앞서,  
Spring 백엔드의 역할과 책임 범위를 명확히 정의하는 것을 목적으로 한다.

이를 통해 다음을 달성한다.

- 프론트엔드(React Native), Spring 백엔드, Firebase 간의 역할 분리
- 마이그레이션 범위 및 원칙 명확화
- 향후 요구사항 명세 및 API 설계의 기준점 제공

---

## 2. 현재 시스템 개요 (AS-IS)

### 2.1 기술 스택

- Client: React Native
- Backend: Firebase Serverless
  - Firestore
  - Cloud Functions
  - Firebase Authentication
  - Firebase Cloud Messaging (FCM)

### 2.2 현재 역할 구조

| 구성 요소 | 역할 |
|---------|-----|
| React Native | 화면 렌더링, 사용자 입력 처리, Firestore 직접 CRUD |
| Firestore | 데이터 저장소 + 실시간 데이터 동기화 |
| Cloud Functions | 이벤트 기반 처리 (알림 발송, 자동 작업) |
| Firebase Auth | 사용자 인증 |
| FCM | 푸시 알림 전송 |

---

## 3. 목표 시스템 개요 (TO-BE)

### 3.1 마이그레이션 목표

- Firebase Serverless 구조를 Spring 기반 중앙 백엔드 구조로 전환
- FCM을 제외한 모든 비즈니스 로직을 Spring으로 이전
- 데이터 및 비즈니스 규칙의 단일 진실의 출처를 Spring 백엔드로 통합

### 3.2 기술 스택 (목표)

- Client: React Native
- Backend: Spring Boot
- Database: MySQL (RDB)
- Authentication: Firebase Authentication
- Notification: Firebase Cloud Messaging (FCM)
- Real-time Communication:
  - SSE (Server-Sent Events)
  - WebSocket

---

## 4. Spring 백엔드의 역할 정의

### 4.1 비즈니스 로직의 최종 판단자

Spring 백엔드는 모든 도메인 규칙의 최종 판단을 수행한다.

- 데이터 생성/수정/삭제에 대한 유효성 검증
- 도메인 상태 전이 규칙 관리

예:
- 파티 생성 가능 조건
- 파티 인원 제한
- 파티 상태 변경 규칙
- 사용자 권한 검증

### 4.2 데이터 접근의 단일 관문

- 클라이언트는 데이터베이스에 직접 접근하지 않는다.
- 모든 데이터 접근은 Spring API를 통해 수행된다.
- Spring 백엔드는 MySQL과의 상호작용을 책임진다.

### 4.3 이벤트 기반 후처리

Firebase Cloud Functions에서 수행하던 역할을  
Spring 내부의 이벤트 기반 구조로 대체한다.

- 비즈니스 핵심 로직과 부가 효과(알림, 로그 등)를 분리한다.
- 도메인 이벤트를 통해 후처리를 수행한다.
- 회원 탈퇴처럼 외부 시스템(Firebase Auth) 정리가 필요한 경우에도, 핵심 트랜잭션과 after-commit 후처리를 분리한다.

---

## 5. 클라이언트(React Native)의 역할

클라이언트는 다음 책임을 유지한다.

- 화면 렌더링
- 사용자 입력 처리
- 사용자 경험(UX) 개선 로직
  - 로딩 처리
  - optimistic UI
  - 버튼 비활성화 등

단, 데이터의 최종 상태 및 비즈니스 규칙 판단은 수행하지 않는다.

---

## 6. 비즈니스 규칙을 서버에서 관리한다는 의미

본 프로젝트에서 비즈니스 규칙이란  
서비스 도메인에서 “이 행위가 가능한지 / 불가능한지”를 판단하는 규칙을 의미한다.

- 클라이언트는 사용자의 의도를 요청으로 전달한다.
- 서버는 요청의 가능 여부를 판단한다.
- 서버의 판단 결과가 항상 최종 결과이다.

이는 “클라이언트에는 화면만 있다”는 의미는 아니다.

---

## 7. 인증(Authentication) 및 권한(Authorization) 구조

### 7.1 인증 방식

- 인증(Authentication)은 Firebase Authentication에 위임한다.
- 클라이언트는 Firebase Auth를 통해 로그인한다.
- 로그인 성공 시 Firebase에서 ID Token(JWT)을 발급받는다.

### 7.2 서버 인증 흐름

1. 클라이언트는 Firebase Auth로 발급받은 ID Token을  
   HTTP 요청의 Authorization 헤더에 포함하여 서버에 전달한다.
2. Spring 백엔드는 Firebase Admin SDK를 사용해 토큰을 검증한다.
3. 검증 성공 시 토큰에 포함된 uid를 사용자 식별자로 사용한다.
4. 검증된 토큰의 `email`이 `@sungkyul.ac.kr`가 아니면 요청을 거부한다. (403)
5. Auth Emulator는 `local-emulator` 프로필에서만 허용하며, 운영/일반 local 프로필에서 emulator host가 감지되면 기동을 차단한다.
6. `members.status = WITHDRAWN`인 회원은 보호 API에서 `403 MEMBER_WITHDRAWN`으로 차단한다.
7. 예외적으로 `POST /v1/members`는 탈퇴한 동일 UID가 명시적인 `409 WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 받을 수 있도록 통과시킨다.

Spring 백엔드는 Access Token / Refresh Token을 직접 발급하거나 관리하지 않는다.

### 7.3 권한(Authorization)

- 인증된 사용자(uid)를 기준으로
- 도메인별 권한 검증을 Spring의 비즈니스 로직에서 수행한다.
- 기본 정책은 “모든 API 인증 필요”이며, 아래 공개 API만 예외로 `permitAll` 처리한다.
  - `GET /v1/app-versions/**`
  - `GET /v1/app-notices/**`
  - `GET /uploads/**` (`MEDIA_STORAGE_PROVIDER=LOCAL`일 때 `media.storage.url-prefix` 기준 공개 업로드 파일 조회)
  - `GET /v3/api-docs/**`
  - `GET /swagger-ui/**`, `GET /swagger-ui.html`
  - `GET /scalar/**`
- 보호 API에 인증 정보가 없거나 유효하지 않으면 401을 반환한다.
- 관리자 권한 정책:
  - 인증 필터에서 `uid` 기준 `members.isAdmin` 조회 후 `true`면 `ROLE_ADMIN` authority를 부여한다.
  - Admin API는 공통 메타 어노테이션(`@AdminApiAccess`, 내부적으로 `@PreAuthorize("hasRole('ADMIN')")`)으로 보호한다.
  - Admin 경로(`/v1/admin/**`)에 비관리자 접근 시 `403 ADMIN_REQUIRED`를 반환한다.
  - 상태 변경 Admin API(`POST`, `PUT`, `PATCH`, `DELETE`)는 `admin_audit_logs`에 `actor/action/target/diff`를 기록하고, `target_id`는 raw 입력이 아니라 canonical 운영 키로 저장한다. 감사 실패가 비즈니스 응답을 500으로 만들지 않도록 best-effort로 처리한다.
  - Academic 관리자 전용 예시:
    - `POST /v1/admin/academic-schedules`
    - `PUT /v1/admin/academic-schedules/{scheduleId}`
    - `DELETE /v1/admin/academic-schedules/{scheduleId}`
    - `POST /v1/admin/courses/bulk`
    - `DELETE /v1/admin/courses`
- Support API 접근 정책:
  - 공개 조회:
    - `GET /v1/app-versions/{platform}`
  - 인증 사용자:
    - `POST /v1/inquiries`
    - `GET /v1/inquiries/my`
    - `POST /v1/reports`
    - `GET /v1/cafeteria-menus`
    - `GET /v1/cafeteria-menus/{weekId}`
  - 관리자 전용:
    - `GET /v1/admin/inquiries`
    - `PATCH /v1/admin/inquiries/{inquiryId}/status`
    - `GET /v1/admin/reports`
    - `PATCH /v1/admin/reports/{reportId}/status`
    - `PUT /v1/admin/app-versions/{platform}`
    - `POST /v1/admin/cafeteria-menus`
    - `PUT /v1/admin/cafeteria-menus/{weekId}`
    - `DELETE /v1/admin/cafeteria-menus/{weekId}`
  - 운영 데이터 노출:
    - Inquiry의 구조화 개인정보(`userEmail`, `userName`, `userRealname`, `userStudentId`)는 관리자에게만 노출한다.
    - 회원 탈퇴 이후에는 동일 필드에 탈퇴 마스킹 정책을 적용한 값만 조회한다.
    - 자유서술 `content`는 운영 추적을 위해 자동 마스킹하지 않는다.
- Academic API 접근 정책:
  - `GET /v1/courses`
  - `GET /v1/timetables/my`
  - `POST /v1/timetables/my/courses`
  - `DELETE /v1/timetables/my/courses/{courseId}`
  - `GET /v1/academic-schedules`
  - 위 경로는 모두 인증이 필요하며, 시간표 API는 인증된 사용자 본인 기준으로만 동작한다.
- 파티 권한 예시:
  - 리더 전용:
    - `PATCH /v1/parties/{id}`
    - `PATCH /v1/parties/{id}/close`
    - `PATCH /v1/parties/{id}/reopen`
    - `PATCH /v1/parties/{id}/arrive`
    - `PATCH /v1/parties/{id}/end`
    - `POST /v1/parties/{id}/cancel`
    - `PATCH /v1/parties/{id}/settlement/members/{memberId}/confirm`
    - `DELETE /v1/parties/{id}/members/{memberId}`
    - `PATCH /v1/join-requests/{id}/accept`, `PATCH /v1/join-requests/{id}/decline`
  - 요청자 본인: `PATCH /v1/join-requests/{id}/cancel`
  - 멤버 본인: `DELETE /v1/parties/{id}/members/me`
- 게시판 권한 예시:
  - 게시글 작성자 전용:
    - `PATCH /v1/posts/{postId}`
    - `DELETE /v1/posts/{postId}`
    - 위반 시 `403 NOT_POST_AUTHOR`
  - 댓글 작성자 전용:
    - `PATCH /v1/comments/{commentId}`
    - `DELETE /v1/comments/{commentId}`
    - 위반 시 `403 NOT_COMMENT_AUTHOR`
  - 내 데이터 전용:
    - `GET /v1/members/me/posts`
    - `GET /v1/members/me/bookmarks`
- Notice 권한 예시:
  - 공개 조회:
    - `GET /v1/app-notices`
    - `GET /v1/app-notices/{appNoticeId}`
  - 공지 댓글 작성자 전용:
    - `DELETE /v1/notice-comments/{commentId}`
    - 위반 시 `403 NOT_NOTICE_COMMENT_AUTHOR`
  - 관리자 전용:
    - `POST /v1/admin/notices/sync`
    - `POST /v1/admin/app-notices`
    - `PATCH /v1/admin/app-notices/{appNoticeId}`
    - `DELETE /v1/admin/app-notices/{appNoticeId}`
    - 위반 시 `403 ADMIN_REQUIRED`
- Image 업로드 권한 예시:
  - 인증 사용자:
    - `POST /v1/images` with `context=POST_IMAGE`
    - `POST /v1/images` with `context=CHAT_IMAGE`
    - `POST /v1/images` with `context=PROFILE_IMAGE`
  - 관리자 전용:
    - `POST /v1/images` with `context=APP_NOTICE_IMAGE`
    - 위반 시 `403 ADMIN_REQUIRED`
  - 공개 조회:
    - LOCAL provider에서는 업로드 결과 URL(`GET /uploads/**`)을 클라이언트가 그대로 표시할 수 있도록 공개 제공한다.
    - LOCAL provider의 공개 경로는 잘못된/만료된 Bearer 토큰이 헤더에 있어도 인증 실패로 차단하지 않는다.
    - FIREBASE provider에서는 Firebase Storage download URL을 그대로 응답한다.

---

## 8. 실시간 통신 설계 범위

### 8.1 실시간 제공 대상 기능

- 파티 목록
- 파티 상태
- 채팅
- 알림
- 게시물 목록
- 게시물 조회수

### 8.2 통신 방식 분리

| 기능 | 통신 방식 |
|----|----|
| 파티 목록 / 상태 | SSE |
| 알림 | SSE |
| 게시물 목록 / 조회수 | SSE |
| 채팅방 목록 요약 (이름/인원/마지막 메시지/미읽음) | WebSocket (`/user/queue/chat-rooms`) |
| 채팅방 상세 메시지 | WebSocket (`/topic/chat/{chatRoomId}`) |
| 채팅방 메시지 전송 | WebSocket (`/app/chat/{chatRoomId}`) |
| 채팅 에러 이벤트 | WebSocket (`/user/queue/errors`) |

운영 원칙:
- 채팅방 목록 화면은 사용자 요약 채널 1개만 구독한다.
- 채팅방 상세 화면은 입장한 방 topic만 구독하고, 화면 이탈 시 해제한다.
- 미읽음 계산은 `message.createdAt > lastReadAt` 기준을 사용한다.
- `lastReadAt`는 과거 값 요청을 무시해 단조 증가를 보장하고, 미래 시각 요청은 서버 현재 시각과 마지막 메시지 시각을 상한으로 clamp한다.
- 모든 채팅방 topic 동시 구독 방식은 서버 fan-out/모바일 리소스 비용 증가로 금지한다.
- 비공개 채팅방(PARTY 포함)은 멤버만 topic 구독/메시지 전송이 가능해야 한다.

### 8.3 실시간 통신 인증

- REST API: 매 요청마다 ID Token 전달
- SSE / WebSocket: 연결 시작 시 ID Token 전달
- 연결 인증 성공 후에는 추가 토큰 검증 없이 통신을 유지한다.
- 단, WebSocket은 연결 이후에도 목적지(`/app/chat/{chatRoomId}`, `/topic/chat/{chatRoomId}`)별 권한 검증을 수행한다.

---

## 9. 마이그레이션 전략

- 기능(도메인) 단위 점진적 마이그레이션을 적용한다.
- Firebase와 Spring 백엔드는 일정 기간 공존할 수 있다.

---

## 10. 실패 허용 정책

- 비즈니스 핵심 로직은 실패 시 전체 요청을 실패로 처리한다.
- 알림, 로그 등 부가 효과는 실패하더라도 핵심 로직에는 영향을 주지 않는다.
- 알림 부가 효과는 상태 변경 성공 이후 `after-commit` 이벤트로 실행한다.
- FCM 자격증명이 없는 로컬/테스트 환경에서는 no-op sender 또는 mock으로 대체 가능해야 하며, 이 경우에도 애플리케이션 기동과 테스트는 유지되어야 한다.
- Optimistic Lock 충돌은 정상 경쟁 상황으로 간주하되, `409` 에러율/배치 충돌률 임계치를 초과하면 운영 알림을 발생시킨다.
  - 상세 임계치: `implementation-roadmap.md`의 "Phase 2 > 2-5 운영 모니터링 기준" 준수

---

## 11. Firebase에 잔존하는 역할

Spring 마이그레이션 이후에도 Firebase는 다음 역할을 유지한다.

- Firebase Authentication
- Firebase Cloud Messaging (FCM)

---

## 12. 정리

Spring 백엔드는 본 프로젝트에서 다음과 같은 역할을 수행한다.

- 비즈니스 규칙의 최종 판단자
- 데이터 접근의 단일 관문
- 실시간 데이터 제공자
- 인증 결과를 신뢰하고 권한을 판단하는 서버

---

## 13. 다음 단계

본 문서를 기준으로 다음 작업을 진행한다.

1. 요구사항 명세서 작성
2. API 명세 (REST / SSE / WebSocket)
3. 도메인 모델링 및 패키지 구조 설계
