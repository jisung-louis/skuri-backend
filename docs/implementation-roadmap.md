# SKURI 백엔드 구현 로드맵

> 최종 수정일: 2026-02-26
> 관련 문서: [도메인 분석](./domain-analysis.md) | [ERD](./erd.md) | [API 명세](./api-specification.md) | [기술 전략](./tech-strategy.md) | [역할 정의](./role-definition.md)

---

## 1. 현재 상태

| 항목 | 상태 |
|------|------|
| Spring Boot | 4.0.3 |
| Java | 21 |
| 빌드 도구 | Gradle |
| 현재 의존성 | JPA, Web MVC, Lombok, MySQL Connector |
| 구현 상태 | Application 클래스만 존재 (빈 프로젝트) |

---

## 2. 구현 순서 총괄

```
Phase 0: 프로젝트 기반 구축
    ↓
Phase 1: Member 도메인 (인증 + 회원)
    ↓
Phase 2: TaxiParty 도메인 (핵심 비즈니스)
    ↓
Phase 3: Chat 도메인 (WebSocket 채팅 엔진)
    ↓
Phase 4: Board 도메인 (커뮤니티 게시판)
    ↓
Phase 5: Notice 도메인 (공지 + RSS 크롤러)
    ↓
Phase 6: Academic 도메인 (학사 정보)
    ↓
Phase 7: Support 도메인 (문의/신고/운영)
    ↓
Phase 8: Notification 인프라 (이벤트 기반 알림)
    ↓
Phase 9: 인프라 및 배포
```

---

## 3. Phase 상세

---

### Phase 0: 프로젝트 기반 구축

> 모든 도메인의 공통 기반을 먼저 구축한다.

#### 0-1. 추가 의존성

| 의존성 | 용도 |
|--------|------|
| `spring-boot-starter-security` | Spring Security (인증/인가 필터 체인) |
| `spring-boot-starter-validation` | Bean Validation (`@Valid`, `@NotBlank` 등) |
| `firebase-admin` | Firebase Admin SDK (ID Token 검증, FCM 발송) |

#### 0-2. 패키지 구조 생성

```
com.skuri.skuri_backend
├── common/
│   ├── entity/
│   │   └── BaseTimeEntity.java
│   ├── dto/
│   │   └── ApiResponse.java
│   │   └── PageResponse.java
│   ├── exception/
│   │   ├── ErrorCode.java              (enum)
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── config/
│       └── JpaAuditingConfig.java
│
├── domain/
│   ├── member/
│   ├── taxiparty/
│   ├── chat/
│   ├── board/
│   ├── notice/
│   ├── academic/
│   └── support/
│
├── infra/
│   ├── auth/
│   ├── notification/
│   ├── storage/
│   └── scheduler/
│
└── api/
    ├── member/
    ├── taxiparty/
    ├── chat/
    ├── board/
    ├── notice/
    ├── academic/
    └── support/
```

#### 0-3. 구현 항목

| # | 항목 | 파일 | 설명 |
|---|------|------|------|
| 1 | BaseTimeEntity | `common/entity/BaseTimeEntity.java` | `createdAt`, `updatedAt` 자동 관리 (`@MappedSuperclass` + JPA Auditing) |
| 2 | JPA Auditing 설정 | `common/config/JpaAuditingConfig.java` | `@EnableJpaAuditing` |
| 3 | ApiResponse | `common/dto/ApiResponse.java` | 공통 응답 래퍼 (`success`, `data`, `message`) |
| 4 | PageResponse | `common/dto/PageResponse.java` | 페이지네이션 응답 (`content`, `page`, `size`, `totalElements`, `hasNext`) |
| 5 | ErrorCode | `common/exception/ErrorCode.java` | 에러 코드 enum (`UNAUTHORIZED`, `NOT_FOUND`, `CONFLICT` 등) |
| 6 | BusinessException | `common/exception/BusinessException.java` | 비즈니스 예외 기본 클래스 |
| 7 | GlobalExceptionHandler | `common/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` 글로벌 예외 처리 |
| 8 | application.yml | `resources/application.yml` | MySQL, JPA, 서버 설정 |

#### 0-4. 완료 기준

- [ ] 빌드 성공 (`./gradlew build`)
- [ ] 애플리케이션 정상 기동
- [ ] 존재하지 않는 API 호출 시 `ApiResponse` 형태의 에러 응답 반환

---

### Phase 1: Member 도메인

> 모든 도메인의 기반. Firebase ID Token 검증 + 회원 관리.

#### 1-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `Member` | `members` | 회원 기본 정보 + `BankAccount`(Embedded) + `NotificationSetting`(Embedded) |
| `LinkedAccount` | `linked_accounts` | 소셜 계정 연결 (Google) |

#### 1-2. 인프라 (Auth)

| # | 항목 | 파일 | 설명 |
|---|------|------|------|
| 1 | Firebase 초기화 | `infra/auth/config/FirebaseConfig.java` | Firebase Admin SDK 초기화 (`@Bean FirebaseApp`) |
| 2 | Token 검증 | `infra/auth/firebase/FirebaseTokenVerifier.java` | `FirebaseAuth.verifyIdToken()` 래핑 |
| 3 | 인증 필터 | `infra/auth/firebase/FirebaseAuthenticationFilter.java` | `OncePerRequestFilter` — ID Token 추출, 검증, SecurityContext 설정 |
| 4 | Security 설정 | `infra/auth/config/SecurityConfig.java` | 필터 체인 (`/v1/app-versions/**`, `/v1/app-notices` permitAll, 나머지 인증 필수) |

#### 1-3. API

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/v1/members` | 회원 가입 (ID Token에서 정보 추출, 멱등) |
| `GET` | `/v1/members/me` | 내 프로필 조회 (lastLogin 갱신) |
| `PUT` | `/v1/members/me` | 프로필 수정 (학번, 학과, 실명 등) |
| `PUT` | `/v1/members/me/bank-account` | 계좌 정보 수정 |
| `PUT` | `/v1/members/me/notification-settings` | 알림 설정 수정 |
| `GET` | `/v1/members/{id}` | 특정 회원 공개 프로필 조회 |

#### 1-4. 완료 기준

- [ ] Firebase ID Token으로 인증 성공/실패 동작
- [ ] `@sungkyul.ac.kr` 이메일 도메인 제한 동작
- [ ] 회원 가입 → 프로필 조회 → 프로필 수정 플로우 동작
- [ ] 인증 없이 보호된 API 호출 시 401 반환

---

### Phase 2: TaxiParty 도메인

> 앱의 핵심 비즈니스. 상태 머신, 동시성 제어, 정산 로직.

#### 2-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `Party` | `parties` | 파티 정보 + `Location`(Embedded) + `Settlement`(Embedded) |
| `PartyMember` | `party_members` | 파티 멤버 (N:M) |
| `PartyTag` | `party_tags` | 파티 태그 |
| `MemberSettlement` | `member_settlements` | 멤버별 정산 상태 |
| `JoinRequest` | `join_requests` | 동승 요청 |

#### 2-2. 핵심 비즈니스 로직

| 로직 | 설명 |
|------|------|
| 파티 상태 머신 | `OPEN → CLOSED → ARRIVED → ENDED` 전이 규칙 |
| 동시성 제어 | Optimistic Lock (`@Version`) — 동시 동승 신청 방어 |
| 정산 | 도착 시 택시비 입력 → 인원수 자동 나눔 → 멤버별 정산 확인 → 전체 완료 시 자동 종료 |
| 자동 종료 | `@Scheduled` 4시간마다 — 12시간 초과 파티 자동 `ENDED` (endReason=TIMEOUT) |
| 권한 분리 | 파티 리더만: 마감/도착/정산/강퇴/종료, 요청자만: 요청 취소 |

#### 2-3. API

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/v1/parties` | 파티 생성 |
| `GET` | `/v1/parties` | 파티 목록 조회 (status, 출발지/도착지 필터) |
| `GET` | `/v1/parties/{id}` | 파티 상세 조회 |
| `PATCH` | `/v1/parties/{id}/close` | 모집 마감 (리더) |
| `PATCH` | `/v1/parties/{id}/reopen` | 모집 재개 (리더) |
| `PATCH` | `/v1/parties/{id}/arrive` | 도착 처리 + 정산 시작 (리더) |
| `PATCH` | `/v1/parties/{id}/end` | 파티 강제 종료 (리더) |
| `DELETE` | `/v1/parties/{id}/members/{memberId}` | 멤버 강퇴 (리더) |
| `DELETE` | `/v1/parties/{id}/members/me` | 파티 탈퇴 (본인) |
| `POST` | `/v1/parties/{partyId}/join-requests` | 동승 요청 |
| `PATCH` | `/v1/join-requests/{id}/accept` | 요청 수락 (리더) |
| `PATCH` | `/v1/join-requests/{id}/decline` | 요청 거절 (리더) |
| `PATCH` | `/v1/join-requests/{id}/cancel` | 요청 취소 (요청자) |
| `GET` | `/v1/parties/{partyId}/join-requests` | 파티 요청 목록 (리더) |
| `GET` | `/v1/members/me/join-requests` | 내 요청 목록 |
| `PATCH` | `/v1/parties/{id}/settlement/members/{memberId}/confirm` | 개별 정산 확인 (리더) |
| `GET` | `/v1/members/me/parties` | 내 파티 목록 |

#### 2-4. 완료 기준

- [ ] 파티 생성 → 동승 요청 → 수락 → 마감 → 도착 → 정산 → 종료 전체 플로우 동작
- [ ] 상태 머신 규칙 위반 시 적절한 에러 반환
- [ ] Optimistic Lock으로 동시 요청 방어
- [ ] 파티 자동 종료 스케줄러 동작

---

### Phase 3: Chat 도메인

> 공개 채팅방 + 파티 채팅 엔진. WebSocket(STOMP) 기반 실시간 통신.

#### 3-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `ChatRoom` | `chat_rooms` | 채팅방 (UNIVERSITY, DEPARTMENT, GAME, CUSTOM, PARTY) |
| `ChatMessage` | `chat_messages` | 채팅 메시지 (TEXT, IMAGE, SYSTEM, ACCOUNT, ARRIVED, END) |
| `ChatRoomMember` | `chat_room_members` | 채팅방 멤버 (lastReadAt, muted) |

#### 3-2. 핵심 구현

| 항목 | 설명 |
|------|------|
| WebSocket 설정 | STOMP + SockJS, Firebase ID Token 인증 |
| ChatService | 공통 채팅 엔진 (메시지 저장, 전송, 읽음 처리) |
| PartyMessageService | 파티 채팅 규칙 (계좌 공유, 도착 메시지, 종료 메시지) — Chat 엔진 사용 |
| 읽음 처리 | `ChatRoomMember.lastReadAt` 기반 미읽음 수 계산 |

#### 3-3. API (REST + WebSocket)

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/v1/chat-rooms` | 채팅방 목록 (타입 필터) |
| `GET` | `/v1/chat-rooms/{id}` | 채팅방 상세 |
| `GET` | `/v1/chat-rooms/{id}/messages` | 메시지 목록 (커서 기반 페이지네이션) |
| `PATCH` | `/v1/chat-rooms/{id}/read` | 읽음 처리 |
| `PATCH` | `/v1/chat-rooms/{id}/mute` | 음소거 토글 |
| WebSocket | `SUBSCRIBE /topic/chat/{chatRoomId}` | 실시간 메시지 수신 |
| WebSocket | `SEND /app/chat/{chatRoomId}` | 메시지 전송 |

#### 3-4. 완료 기준

- [ ] WebSocket 연결 및 실시간 메시지 송수신 동작
- [ ] 파티 채팅방 자동 생성 (파티 생성 시) 동작
- [ ] 읽음/미읽음 처리 동작
- [ ] 파티 채팅 특수 메시지 (계좌, 도착, 종료) 동작

---

### Phase 4: Board 도메인

> 커뮤니티 게시판. CRUD + 익명 + 좋아요/북마크.

#### 4-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `Post` | `posts` | 게시글 |
| `PostImage` | `post_images` | 게시글 이미지 |
| `Comment` | `comments` | 댓글/대댓글 (self-reference) |
| `PostInteraction` | `post_interactions` | 좋아요 + 북마크 통합 |

#### 4-2. 핵심 로직

| 로직 | 설명 |
|------|------|
| 익명 처리 | `anonId` = `{postId}:{userId}`, `anonymousOrder` 서버 계산 (글 단위 순번) |
| 좋아요/북마크 | `PostInteraction` 단일 테이블, 토글 방식 |
| 카운트 관리 | `viewCount`, `likeCount`, `commentCount`, `bookmarkCount` 동기화 |

#### 4-3. API

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/v1/posts` | 게시글 작성 |
| `GET` | `/v1/posts` | 게시글 목록 (카테고리 필터, 페이지네이션) |
| `GET` | `/v1/posts/{id}` | 게시글 상세 (조회수 증가) |
| `PUT` | `/v1/posts/{id}` | 게시글 수정 (작성자) |
| `DELETE` | `/v1/posts/{id}` | 게시글 삭제 (작성자) |
| `POST` | `/v1/posts/{id}/like` | 좋아요 토글 |
| `POST` | `/v1/posts/{id}/bookmark` | 북마크 토글 |
| `GET` | `/v1/posts/{postId}/comments` | 댓글 목록 |
| `POST` | `/v1/posts/{postId}/comments` | 댓글 작성 |
| `PUT` | `/v1/comments/{id}` | 댓글 수정 |
| `DELETE` | `/v1/comments/{id}` | 댓글 삭제 |
| `GET` | `/v1/members/me/posts` | 내 게시글 |
| `GET` | `/v1/members/me/bookmarks` | 내 북마크 |

#### 4-4. 완료 기준

- [ ] 게시글 CRUD + 이미지 첨부 동작
- [ ] 익명 댓글 순번 부여 정확히 동작
- [ ] 좋아요/북마크 토글 + 카운트 동기화 동작

---

### Phase 5: Notice 도메인

> 학교 공지 크롤링 + 앱 공지 관리.

#### 5-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `Notice` | `notices` | 학교 공지 (크롤링) |
| `NoticeComment` | `notice_comments` | 공지 댓글 |
| `NoticeReadStatus` | `notice_read_status` | 읽음 상태 |
| `AppNotice` | `app_notices` | 앱 운영 공지 |

#### 5-2. 핵심 구현

| 항목 | 설명 |
|------|------|
| RSS 크롤러 | `@Scheduled` 평일 8~20시, 10분마다 RSS 피드 파싱 → DB 저장 |
| contentHash | SHA1 해시로 중복 공지 배제 |
| 공지 ID | `Base64(link).replace(/=+$/, '').slice(0, 120)` — 링크 기반 안정 ID |

#### 5-3. API

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/v1/notices` | 공지 목록 (카테고리/학과 필터) |
| `GET` | `/v1/notices/{id}` | 공지 상세 (읽음 처리) |
| `POST` | `/v1/notices/{id}/read` | 읽음 표시 |
| `GET` | `/v1/notices/{noticeId}/comments` | 공지 댓글 목록 |
| `POST` | `/v1/notices/{noticeId}/comments` | 공지 댓글 작성 |
| `DELETE` | `/v1/notice-comments/{id}` | 공지 댓글 삭제 |
| `GET` | `/v1/app-notices` | 앱 공지 목록 (**Public**) |
| `GET` | `/v1/app-notices/{id}` | 앱 공지 상세 |

#### 5-4. 완료 기준

- [ ] RSS 크롤러가 주기적으로 공지 수집 동작
- [ ] 중복 공지 필터링 동작
- [ ] 공지 댓글 + 익명 순번 동작
- [ ] AppNotice 비인증 조회 동작

---

### Phase 6: Academic 도메인

> 읽기 위주의 학사 정보 제공.

#### 6-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `Course` | `courses` + `course_schedules` | 강의 정보 |
| `UserTimetable` | `user_timetables` + `user_timetable_courses` | 사용자 시간표 |
| `AcademicSchedule` | `academic_schedules` | 학사 일정 |

#### 6-2. API

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/v1/courses` | 강의 검색 (학기, 학과, 교수, 키워드) |
| `GET` | `/v1/members/me/timetable` | 내 시간표 조회 |
| `PUT` | `/v1/members/me/timetable` | 시간표 저장 (강의 ID 목록) |
| `GET` | `/v1/academic-schedules` | 학사 일정 목록 |

#### 6-3. 완료 기준

- [ ] 강의 검색 + 시간표 CRUD 동작
- [ ] 학사 일정 조회 동작

---

### Phase 7: Support 도메인

> 문의/신고, 앱 버전, 학식 메뉴 관리.

#### 7-1. 엔티티

| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `Inquiry` | `inquiries` | 문의 |
| `Report` | `reports` | 신고 |
| `AppVersion` | `app_versions` | 앱 버전 (ios, android) |
| `CafeteriaMenu` | `cafeteria_menus` | 학식 메뉴 |

#### 7-2. API

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/v1/inquiries` | 문의 접수 |
| `GET` | `/v1/members/me/inquiries` | 내 문의 목록 |
| `POST` | `/v1/reports` | 신고 접수 |
| `GET` | `/v1/app-versions/{platform}` | 앱 버전 정보 (**Public**) |
| `GET` | `/v1/cafeteria-menus` | 학식 메뉴 (이번 주) |
| `GET` | `/v1/cafeteria-menus/{weekId}` | 특정 주차 학식 메뉴 |

#### 7-3. 완료 기준

- [ ] 문의/신고 접수 동작
- [ ] 앱 버전 비인증 조회 동작
- [ ] 학식 메뉴 조회 동작

---

### Phase 8: Notification 인프라

> 도메인 이벤트 기반 알림 시스템. FCM + 인앱 인박스.

#### 8-1. 구현 범위

| 항목 | 설명 |
|------|------|
| `UserNotification` 엔티티 | 알림 인박스 테이블 |
| `FcmToken` 엔티티 | FCM 토큰 관리 |
| `PushNotificationService` | Firebase Admin SDK로 FCM 발송 |
| `NotificationService` | 인박스 저장 + 조회 |
| `DomainEventNotificationListener` | `@EventListener` + `@Async` 비동기 처리 |

#### 8-2. 도메인 이벤트 → 알림 매핑

| 이벤트 | 알림 타입 | FCM | 인박스 |
|--------|-----------|-----|--------|
| PartyCreatedEvent | PARTY_CREATED | O | X |
| JoinRequestCreatedEvent | JOIN_REQUEST | O | O |
| JoinRequestAcceptedEvent | JOIN_ACCEPTED | O | O |
| JoinRequestDeclinedEvent | JOIN_DECLINED | O | O |
| PartyStatusChangedEvent | PARTY_CLOSED / PARTY_ARRIVED | O | O |
| SettlementCompletedEvent | SETTLEMENT_COMPLETED | O | O |
| MemberKickedEvent | MEMBER_KICKED | O | O |
| ChatMessageCreatedEvent | CHAT_MESSAGE | O | X |
| PostLikedEvent | POST_LIKED | O | O |
| CommentCreatedEvent | COMMENT_CREATED | O | O |
| NoticeCreatedEvent | NOTICE | O | O |
| AppNoticeCreatedEvent | APP_NOTICE | O | O |

#### 8-3. API

| Method | Path | 설명 |
|--------|------|------|
| `GET` | `/v1/notifications` | 알림 인박스 목록 |
| `PATCH` | `/v1/notifications/{id}/read` | 알림 읽음 처리 |
| `PATCH` | `/v1/notifications/read-all` | 전체 읽음 처리 |
| `GET` | `/v1/notifications/unread-count` | 미읽음 수 |
| `POST` | `/v1/members/me/fcm-tokens` | FCM 토큰 등록 |
| `DELETE` | `/v1/members/me/fcm-tokens` | FCM 토큰 해제 |

#### 8-4. 완료 기준

- [ ] 도메인 이벤트 발행 → 비동기 리스너 수신 동작
- [ ] FCM 푸시 발송 동작
- [ ] 알림 인박스 CRUD 동작
- [ ] 알림 설정 반영 (mute, 카테고리별 on/off)

---

### Phase 9: 인프라 및 배포

> Docker, AWS, CI/CD.

#### 9-1. 구현 항목

| # | 항목 | 설명 |
|---|------|------|
| 1 | Dockerfile | Spring Boot 멀티스테이지 빌드 |
| 2 | docker-compose.yml | app + MySQL + Redis 로컬 개발 환경 |
| 3 | Redis 캐시 | 파티 목록, 공지 목록, FCM 토큰 캐시 |
| 4 | AWS 배포 | EC2 + RDS (MySQL) + ElastiCache (Redis) |
| 5 | GitHub Actions | main 브랜치 push → EC2 자동 배포 |
| 6 | Swagger/OpenAPI | API 문서 자동화 |

#### 9-2. 완료 기준

- [ ] `docker-compose up` 으로 로컬 전체 환경 기동
- [ ] AWS 배포 및 정상 동작
- [ ] CI/CD 파이프라인 동작

---

## 4. Phase 간 의존 관계

```
Phase 0 ─── 필수 선행 ──→ Phase 1 (Member)
Phase 1 ─── 필수 선행 ──→ Phase 2 (TaxiParty)
Phase 2 ─── 필수 선행 ──→ Phase 3 (Chat)
Phase 1 ─── 필수 선행 ──→ Phase 4 (Board)
Phase 1 ─── 필수 선행 ──→ Phase 5 (Notice)
Phase 1 ─── 필수 선행 ──→ Phase 6 (Academic)
Phase 1 ─── 필수 선행 ──→ Phase 7 (Support)
Phase 2~7 ── 연동 ──→ Phase 8 (Notification)
전체 ───────────────→ Phase 9 (인프라/배포)
```

**참고:** Phase 4~7 (Board, Notice, Academic, Support)은 서로 독립적이므로 **병렬 구현 가능**합니다.
단, Phase 2 → Phase 3은 파티 채팅 연동 때문에 **순차 구현 필수**입니다.

---

## 5. 각 Phase의 내부 구현 순서 (공통 패턴)

모든 도메인은 아래 순서로 bottom-up 구현합니다:

```
1. Entity + Enum          (도메인 모델)
2. Repository             (데이터 접근)
3. Service                (비즈니스 로직)
4. DTO (Request/Response) (API 계약)
5. Controller             (API 엔드포인트)
6. Event (필요 시)         (도메인 이벤트 발행)
```

---

## 참고 문서

- [도메인 분석](./domain-analysis.md) — 도메인 상세 및 엔티티 설계
- [ERD](./erd.md) — 테이블 스키마 및 인덱스
- [API 명세](./api-specification.md) — 전체 API 상세 스펙
- [기술 전략](./tech-strategy.md) — 기술 선택 근거 및 포트폴리오 전략
- [역할 정의](./role-definition.md) — Spring 백엔드의 책임 범위

---

> **문서 이력**
> - 2026-02-26: 초안 작성
