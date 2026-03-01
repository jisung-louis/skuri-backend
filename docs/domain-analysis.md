# Spring 백엔드 도메인 분석

> 최종 수정일: 2026-02-03
> 분석 기준: Firestore 컬렉션, Cloud Functions 트리거, Context/Hook 구조

본 문서는 현재 Firebase 기반 SKURI Taxi 앱을 Spring Boot + MySQL 백엔드로 마이그레이션하기 위한 **도메인 분석 결과**입니다.

---

## 목차

1. [분석 개요](#1-분석-개요)
2. [도메인 목록](#2-도메인-목록)
3. [도메인 상세](#3-도메인-상세)
4. [도메인 간 관계](#4-도메인-간-관계)
5. [인프라 계층](#5-인프라-계층)
6. [패키지 구조](#6-패키지-구조)
7. [핵심 엔티티 설계](#7-핵심-엔티티-설계)
8. [마이그레이션 우선순위](#8-마이그레이션-우선순위)

---

## 1. 분석 개요

### 1.1 분석 원칙

- **화면(UI) 기준이 아닌 "비즈니스 개념" 기준**으로 도메인 식별
- Firestore 컬렉션, Cloud Functions 트리거, Context/Hook 구조를 주요 근거로 사용
- 기술적 관심사(Auth, FCM, 실시간 통신)는 도메인이 아닌 **인프라**로 분리

### 1.2 분석 대상

| 소스 | 분석 항목 |
|------|----------|
| `docs/firestore-data-structure.md` | Firestore 컬렉션 구조 (6개 섹션, 20+ 컬렉션) |
| `functions/src/index.ts` | Cloud Functions 트리거 (15+ 함수) |
| `src/hooks/` | Firestore 구독 훅 (50+ 훅) |
| `src/contexts/` | 전역 상태 관리 (AuthContext, JoinRequestContext 등) |
| `src/types/` | TypeScript 타입 정의 |

### 1.3 설계 결정 사항

| 항목 | 결정 | 근거 |
|------|------|------|
| 파티 채팅 | TaxiParty 도메인에서 규칙 관리, Chat 엔진 사용 | 계좌 공유, 도착/정산 메시지 등 파티 고유 규칙 존재 |
| Minecraft 연동 | 별도 도메인 분리 안함 | 확장 계획 없음 |
| Settlement(정산) | Party 내부에 임베디드 | 실제 결제/송금 API 연동 계획 없음 |
| Notification | 인프라 계층으로 유지 | 자체 비즈니스 규칙 없음, 이벤트 결과 전달용 |

---

## 2. 도메인 목록

### 2.1 최종 도메인 (7개 + 인프라)

| # | 도메인 | 유형 | 핵심 책임 | 주요 엔티티 |
|---|--------|------|----------|------------|
| 1 | **Member** | Core | 회원 프로필, 계정 정보, 알림 설정 | User, NotificationSetting, LinkedAccount, BankAccount |
| 2 | **TaxiParty** | Core | 택시 동승 모집, 요청 처리, 정산, 파티 채팅 규칙 | Party, JoinRequest, Settlement, PartyMessage |
| 3 | **Chat** | Supporting | 공개 채팅방 관리, 메시지 교환 (채팅 엔진) | ChatRoom, ChatMessage, ChatRoomMember |
| 4 | **Board** | Supporting | 게시글 CRUD, 댓글, 좋아요/북마크 | Post, Comment, PostInteraction |
| 5 | **Notice** | Supporting | 학교 공지 크롤링/조회, 앱 공지 | Notice, NoticeComment, AppNotice, NoticeReadStatus |
| 6 | **Academic** | Generic | 강의 정보, 시간표, 학사 일정 | Course, UserTimetable, AcademicSchedule |
| 7 | **Support** | Generic | 문의/신고 접수, 앱 버전, 학식 메뉴 | Inquiry, Report, AppVersion, CafeteriaMenu |
| - | **Notification** | Infra | 도메인 이벤트 기반 알림 인박스 | UserNotification |

### 2.2 도메인 유형 정의

- **Core**: 앱의 핵심 비즈니스 가치를 제공하는 도메인
- **Supporting**: Core 도메인을 지원하는 도메인
- **Generic**: 범용적이고 교체 가능한 도메인
- **Infra**: 도메인 횡단 관심사, 기술적 인프라

---

## 3. 도메인 상세

### 3.1 Member (회원)

```
책임: 사용자 인증 후 프로필 관리, 계좌 정보, 알림 설정

Firestore 컬렉션:
  - users/{uid}
  - users/{uid}.notificationSettings
  - users/{uid}/chatRoomNotifications/{chatRoomId}

Hooks:
  - useUserProfile
  - useAccountInfo
  - useNotificationSettings

엔티티:
  - User
    - uid, email, displayName, studentId, department
    - photoURL, realname, isAdmin, joinedAt, lastLogin
  - NotificationSetting
    - allNotifications, partyNotifications, noticeNotifications
    - boardLikeNotifications, boardCommentNotifications
    - systemNotifications
    - noticeNotificationsDetail (카테고리별 상세 설정)
  - LinkedAccount
    - provider, providerId, email, displayName, photoURL
  - BankAccount
    - bankName, accountNumber, accountHolder, hideName

특이사항:
  - 모든 도메인에서 참조하는 핵심 엔티티
  - FCM 토큰 관리는 인프라(Notification)로 이동
```

### 3.2 TaxiParty (택시 파티)

```
책임: 택시 동승 파티 전체 라이프사이클 관리

Firestore 컬렉션:
  - parties/{partyId}
  - joinRequests/{requestId}
  - chats/{partyId}/messages/{messageId}
  - chats/{partyId}/notificationSettings/{uid}

Cloud Functions:
  - onPartyCreate (파티 생성 알림)
  - onJoinRequestCreate (동승 요청 알림)
  - onJoinRequestUpdate (승인/거절 알림)
  - onPartyStatusUpdate (상태 변경 알림)
  - onSettlementComplete (정산 완료 알림)
  - onPartyMemberKicked (강퇴 알림)
  - onPartyEnded (파티 종료 알림)
  - cleanupOldParties (12시간 초과 파티 정리)

Hooks:
  - useParties, useParty, useMyParty
  - useJoinRequest, useJoinRequestStatus, usePendingJoinRequest
  - usePartyActions

엔티티:
  - Party
    - id, leaderId, departure, destination, departureTime
    - maxMembers, members[], tags[], detail
    - status (OPEN → CLOSED → ARRIVED → ENDED)
    - endReason (ARRIVED, CANCELLED, TIMEOUT, WITHDRAWED)
    - settlement (Embedded)
  - JoinRequest
    - id, partyId, leaderId, requesterId
    - status (PENDING → ACCEPTED | DECLINED | CANCELED)
  - Settlement (Embedded)
    - status (PENDING, COMPLETED)
    - perPersonAmount
    - memberSettlements (Map<memberId, MemberSettlement>)
  - MemberSettlement (Embedded)
    - settled, settledAt

상태 머신:
  Party:
    OPEN → CLOSED       (리더: 모집 마감)
    CLOSED → OPEN       (리더: 모집 재개)
    OPEN|CLOSED → ARRIVED  (리더: 도착 처리 → 정산 시작)
    ARRIVED → ENDED     (모든 멤버 정산 완료 시 자동 → endReason=ARRIVED)
    ARRIVED → ENDED     (리더 강제 종료 → endReason=FORCE_ENDED, 미정산 멤버 있어도 가능)
    OPEN|CLOSED|ARRIVED → ENDED (자동 종료 또는 취소)

  JoinRequest: PENDING → ACCEPTED | DECLINED | CANCELED
    - CANCELED: 요청자 본인만 취소 가능 (PENDING 상태에서만)
    - 리더는 DECLINE으로 거절 (CANCEL 아님)

파티 자동 종료 정책 (@Scheduled):
  - 실행 주기: 4시간마다
  - 기준: createdAt 기준 12시간 초과한 파티
  - 대상: ENDED가 아닌 모든 파티 (OPEN, CLOSED, ARRIVED 포함)
  - 처리: status=ENDED, endReason=TIMEOUT
  - 구현: Firebase Cloud Functions onSchedule → Spring @Scheduled 대체

endReason 종류:
  - ARRIVED: 모든 멤버 정산 완료로 정상 종료
  - FORCE_ENDED: 리더 강제 종료 (미정산 멤버 있을 수 있음)
  - CANCELLED: 리더 취소
  - TIMEOUT: 자동 종료 (12시간 초과)
  - WITHDRAWED: 리더 탈퇴로 인한 종료

파티 채팅 특수 메시지:
  - ACCOUNT: 계좌 정보 공유
  - ARRIVED: 도착 알림 (요금, 1인당 금액)
  - END: 파티 종료 알림
```

### 3.3 Chat (채팅)

```
책임: 공개 채팅방 관리 및 채팅 엔진 제공

Firestore 컬렉션:
  - chatRooms/{chatRoomId}
  - chatRooms/{chatRoomId}/messages/{messageId}
  - users/{uid}/chatRoomStates/{chatRoomId}

Cloud Functions:
  - onChatRoomMessageCreated (채팅 알림)
  - syncMinecraftChatMessage (MC 연동)

Hooks:
  - useChatRooms, useChatRoom
  - useChatMessages, useChatActions
  - useChatRoomNotifications, useChatRoomStates

엔티티:
  - ChatRoom
    - id, name, type (UNIVERSITY, DEPARTMENT, GAME, CUSTOM, PARTY)
    - department, description, createdBy
    - memberCount, messageCount, isPublic, maxMembers
    - lastMessage (Embedded)
    - members[]는 별도 ChatRoomMember 엔티티로 관리 (lastReadAt, muted 필드 포함)
  - ChatMessage
    - id, chatRoomId, senderId, senderName
    - text, type (TEXT, IMAGE, SYSTEM, ACCOUNT, ARRIVED, END)
    - createdAt (서버 타임스탬프)
    - clientCreatedAt (클라이언트 타임스탬프 — Optimistic UI용 보조 필드, 서버 저장 X)
    - accountData, arrivalData (파티 채팅 전용)
    - direction, source, minecraftUuid (MC 연동용)
  - ChatRoomMember
    - userId, chatRoomId, lastReadAt, muted

채팅방 타입:
  - UNIVERSITY: 전체 채팅방
  - DEPARTMENT: 학과별 채팅방
  - GAME: 게임(Minecraft) 채팅방
  - CUSTOM: 사용자 생성 채팅방
  - PARTY: 택시 파티 채팅방 (TaxiParty에서 관리)

역할:
  - 파티 채팅: TaxiParty 도메인이 규칙 관리, Chat은 엔진만 제공
  - 공개 채팅: Chat 도메인이 전체 관리
```

### 3.4 Board (게시판)

```
책임: 커뮤니티 게시글 및 상호작용 관리

Firestore 컬렉션:
  - boardPosts/{postId}
  - boardComments/{commentId}
  - userBoardInteractions/{userId}_{postId}

Hooks:
  - useBoardPosts, useBoardPost
  - useBoardComments, useBoardWrite, useBoardEdit
  - usePostActions, useUserBoardInteractions
  - useBoardCategoryCounts

엔티티:
  - Post
    - id, title, content, authorId, authorName, authorProfileImage
    - isAnonymous, anonId, category
    - viewCount, likeCount, commentCount, bookmarkCount
    - isPinned, isDeleted, images[], createdAt, updatedAt
  - Comment
    - id, postId, content, authorId, authorName, authorProfileImage
    - isAnonymous, anonId (= "{postId}:{userId}", 글 단위 익명 식별자)
    - anonymousOrder (서버 계산, 아래 규칙 참조)
    - parentId (대댓글용), isDeleted

  anonymousOrder 계산 규칙:
    - 게시글(postId) 단위로 Map<anonId, order> 관리
    - 댓글 작성 시 anonId가 Map에 없으면 → 새 순번 부여 (현재 Map.size() + 1)
    - 이미 Map에 있으면 → 기존 순번 재사용
    - 댓글 삭제 후 순번 재계산 없음 (삭제된 댓글도 번호 영구 보존)
    - isAnonymous = false이면 anonymousOrder = null
  - PostInteraction
    - userId, postId, isLiked, isBookmarked

카테고리:
  - GENERAL (일반)
  - QUESTION (질문)
  - REVIEW (후기)
  - ANNOUNCEMENT (공지)
```

### 3.5 Notice (공지사항)

```
책임: 학교 공지 수집 및 앱 공지 관리

Firestore 컬렉션:
  - notices/{noticeId}
  - notices/{noticeId}/readBy/{uid}
  - noticeComments/{commentId}
  - appNotices/{noticeId}

Cloud Functions:
  - scheduledRSSFetch (10분 주기, 평일 8-20시)
  - onNoticeCreated (새 공지 알림)
  - onAppNoticeCreated (앱 공지 알림)

Hooks:
  - useNotices, useNotice, useRecentNotices
  - useNoticeComments, useNoticeLike
  - useAppNotice, useAppNotices
  - useNoticeSettings

엔티티:
  - Notice
    - id (Base64(link).replace(/=+$/, '').slice(0, 120) — 링크 기반 안정 ID)
    - title, content, link, postedAt, category
    - department, author, source, contentHash (중복 배제용)
    - contentDetail (HTML), contentAttachments[]
    - viewCount, likeCount, commentCount
  - NoticeComment
    - id, noticeId, userId, userDisplayName
    - content, isAnonymous, anonId (= "{noticeId}:{userId}")
    - anonymousOrder (서버 계산: Board Comment의 anonymousOrder 계산 규칙과 동일, noticeId 단위 Map 관리)
    - replyCount, parentId, isDeleted
  - NoticeReadStatus
    - userId, noticeId, isRead, readAt
  - AppNotice
    - id, title, content
    - category (UPDATE, MAINTENANCE, EVENT, GENERAL)
    - priority (HIGH, NORMAL, LOW)
    - imageUrls[], actionUrl, publishedAt

학교 공지 카테고리 (14개):
  새소식, 학사, 학생, 장학/등록/학자금, 입학,
  취업/진로개발/창업, 공모/행사, 교육/글로벌, 일반,
  입찰구매정보, 사회봉사센터, 장애학생지원센터, 생활관, 비교과
```

### 3.6 Academic (학사 정보)

```
책임: 강의/시간표/학사일정 정보 제공

Firestore 컬렉션:
  - courses/{courseId}
  - userTimetables/{docId}
  - academicSchedules/{scheduleId}

Hooks:
  - useTimetable
  - useAcademicSchedules
  - useCourseSearch

엔티티:
  - Course
    - id, grade, category, code, division, name
    - credits, professor, schedule[], location
    - note, semester, department
  - CourseSchedule (Embedded)
    - dayOfWeek (1-5), startPeriod, endPeriod
  - UserTimetable
    - id, userId, semester, courseIds[]
  - AcademicSchedule
    - id, title, startDate, endDate
    - type (SINGLE, MULTI), isPrimary, description
```

### 3.7 Support (지원/운영)

```
책임: 문의/신고 접수, 앱 버전 관리, 학식 메뉴

Firestore 컬렉션:
  - inquiries/{docId}
  - reports/{reportId}
  - appVersion/{platform}
  - cafeteriaMenus/{weekId}
  - adminAuditLogs/{logId}
  - qr_logs/{logId}

Hooks:
  - useSubmitInquiry
  - useCafeteriaMenu

엔티티:
  - Inquiry
    - id, type (FEATURE, BUG, ACCOUNT, SERVICE, OTHER)
    - subject, content, userId, userEmail, userName
    - userRealname, userStudentId
    - status (PENDING, IN_PROGRESS, RESOLVED)
  - Report
    - id, targetType (POST, COMMENT, CHAT_MESSAGE, PROFILE)
    - targetId, targetAuthorId, category
    - reporterId, status
  - AppVersion
    - platform (ios, android)
    - minimumVersion, forceUpdate, message
    - icon, title, showButton, buttonText, buttonUrl
  - CafeteriaMenu
    - weekId, weekStart, weekEnd
    - menus: Map<date, Map<restaurant, items[]>>
  - AdminAuditLog
    - id, adminId, action, targetType, targetId
    - detail (JSON), createdAt
    (Spring AOP로 Admin API 호출 시 자동 기록)
```

---

## 4. 도메인 간 관계

### 4.1 관계 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                          INFRASTRUCTURE                              │
│  ┌──────────────┐  ┌───────────────────┐  ┌──────────────────────┐  │
│  │     Auth     │  │   Notification    │  │      WebSocket       │  │
│  │  (Security)  │  │  (FCM + Inbox)    │  │   (실시간 채팅)      │  │
│  └──────────────┘  └─────────▲─────────┘  └──────────▲───────────┘  │
│                              │ Domain Events          │              │
└──────────────────────────────┼────────────────────────┼──────────────┘
                               │                        │
┌──────────────────────────────┼────────────────────────┼──────────────┐
│                          DOMAIN LAYER                                │
│                              │                        │              │
│  ┌────────────┐         ┌────┴────────┐         ┌────┴────┐         │
│  │   Member   │◄────────│  TaxiParty  │────────►│   Chat  │         │
│  │            │         │             │  uses   │ (Engine)│         │
│  │ - 프로필   │         │ - 파티 관리 │         │         │         │
│  │ - 계좌     │         │ - 동승 요청 │         │ - 채팅방│         │
│  │ - 알림설정 │         │ - 정산      │         │ - 메시지│         │
│  └─────▲──────┘         │ - 파티채팅  │         └────▲────┘         │
│        │                │   (규칙)    │              │              │
│        │                └─────────────┘              │              │
│        │                                             │              │
│  ┌─────┴──────┐    ┌──────────┐    ┌────────────────┴───┐          │
│  │   Board    │    │  Notice  │    │      Academic      │          │
│  │            │    │          │    │                    │          │
│  │ - 게시글   │    │ - 공지   │    │ - 강의/시간표     │          │
│  │ - 댓글     │    │ - 댓글   │    │ - 학사일정        │          │
│  │ - 좋아요   │    │ - 앱공지 │    └────────────────────┘          │
│  └────────────┘    └──────────┘                                     │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │                         Support                                 │ │
│  │     문의 / 신고 / 앱버전 / 학식메뉴                            │ │
│  └────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2 의존 관계 설명

| 관계 | 유형 | 설명 |
|------|------|------|
| 모든 도메인 → Member | 약결합 | userId/authorId 참조만, 필요시 조회 |
| TaxiParty → Chat | 사용 | 파티 채팅은 Chat 엔진 사용, 규칙은 TaxiParty에서 관리 |
| TaxiParty ↔ PartyChat | 강결합 | 파티 생성/종료 시 채팅방도 함께 생성/비활성화 |
| Board, Notice → Member | 약결합 | 작성자 참조만 |
| Academic, Support | 독립 | 공용 데이터, 다른 도메인과 직접 의존 없음 |

### 4.3 도메인 이벤트

| 도메인 | 이벤트 | 수신자 |
|--------|--------|--------|
| TaxiParty | PartyCreatedEvent | Notification (푸시) |
| TaxiParty | JoinRequestCreatedEvent | Notification |
| TaxiParty | JoinRequestAcceptedEvent | Notification |
| TaxiParty | JoinRequestDeclinedEvent | Notification |
| TaxiParty | PartyStatusChangedEvent | Notification |
| TaxiParty | SettlementCompletedEvent | Notification |
| TaxiParty | MemberKickedEvent | Notification |
| Chat | ChatMessageCreatedEvent | Notification |
| Board | PostLikedEvent | Notification |
| Board | CommentCreatedEvent | Notification |
| Notice | NoticeCreatedEvent | Notification |
| Notice | AppNoticeCreatedEvent | Notification |

---

## 5. 인프라 계층

### 5.1 인프라 구성 요소

| 인프라 | 현재 기술 | Spring 마이그레이션 |
|--------|----------|-------------------|
| **Auth** | Firebase Auth (Google Sign-In) | Spring Security + Firebase Admin SDK (ID Token 검증) |
| **Push** | FCM + Cloud Functions | FCM (Firebase Admin SDK) |
| **Notification** | userNotifications 컬렉션 | UserNotification 테이블 + 이벤트 리스너 |
| **Storage** | Firebase Storage | AWS S3 또는 GCS |
| **Realtime** | Firestore onSnapshot | WebSocket (STOMP + SockJS) |
| **Scheduler** | Cloud Functions onSchedule | Spring Scheduler / Quartz |
| **Audit** | adminAuditLogs 컬렉션 | Spring AOP + AuditLog 테이블 |

### 5.2 Notification 인프라 상세

```
역할: 도메인 이벤트를 수신하여 사용자에게 알림 전달

구성요소:
  - UserNotification (인앱 알림 인박스)
  - PushNotificationService (FCM 전송)
  - DomainEventNotificationListener (이벤트 수신)

UserNotification 엔티티:
  - id, userId, type, title, message
  - data (JSON), isRead, readAt, createdAt

알림 타입:
  - PARTY_CREATED, PARTY_JOIN_REQUEST, PARTY_JOIN_ACCEPTED
  - PARTY_JOIN_DECLINED, PARTY_CLOSED, PARTY_ARRIVED
  - PARTY_ENDED, MEMBER_KICKED, SETTLEMENT_COMPLETED
  - CHAT_MESSAGE, POST_LIKED, COMMENT_CREATED
  - NOTICE, APP_NOTICE
```

---

## 6. 패키지 구조

```
com.skuri.taxi
├── domain
│   ├── member
│   │   ├── entity
│   │   │   ├── Member.java
│   │   │   ├── NotificationSetting.java
│   │   │   ├── LinkedAccount.java
│   │   │   └── BankAccount.java
│   │   ├── repository
│   │   ├── service
│   │   └── dto
│   │
│   ├── taxiparty
│   │   ├── entity
│   │   │   ├── Party.java
│   │   │   ├── JoinRequest.java
│   │   │   ├── Settlement.java          # @Embeddable
│   │   │   └── MemberSettlement.java    # @Embeddable
│   │   ├── repository
│   │   ├── service
│   │   │   ├── PartyService.java
│   │   │   ├── JoinRequestService.java
│   │   │   ├── SettlementService.java
│   │   │   └── PartyMessageService.java # Chat 엔진 사용
│   │   ├── event
│   │   │   ├── PartyCreatedEvent.java
│   │   │   ├── JoinRequestEvent.java
│   │   │   ├── PartyStatusChangedEvent.java
│   │   │   └── SettlementCompletedEvent.java
│   │   └── dto
│   │
│   ├── chat
│   │   ├── entity
│   │   │   ├── ChatRoom.java
│   │   │   ├── ChatMessage.java
│   │   │   └── ChatRoomMember.java
│   │   ├── repository
│   │   ├── service
│   │   │   └── ChatService.java         # 공통 채팅 엔진
│   │   ├── event
│   │   │   └── ChatMessageCreatedEvent.java
│   │   └── dto
│   │
│   ├── board
│   │   ├── entity
│   │   │   ├── Post.java
│   │   │   ├── Comment.java
│   │   │   └── PostInteraction.java
│   │   ├── repository
│   │   ├── service
│   │   ├── event
│   │   │   ├── PostLikedEvent.java
│   │   │   └── CommentCreatedEvent.java
│   │   └── dto
│   │
│   ├── notice
│   │   ├── entity
│   │   │   ├── Notice.java
│   │   │   ├── NoticeComment.java
│   │   │   ├── NoticeReadStatus.java
│   │   │   └── AppNotice.java
│   │   ├── repository
│   │   ├── service
│   │   ├── crawler
│   │   │   └── NoticeCrawlerService.java
│   │   ├── event
│   │   │   └── NoticeCreatedEvent.java
│   │   └── dto
│   │
│   ├── academic
│   │   ├── entity
│   │   │   ├── Course.java
│   │   │   ├── CourseSchedule.java       # @Embeddable
│   │   │   ├── UserTimetable.java
│   │   │   └── AcademicSchedule.java
│   │   ├── repository
│   │   ├── service
│   │   └── dto
│   │
│   └── support
│       ├── entity
│       │   ├── Inquiry.java
│       │   ├── Report.java
│       │   ├── AppVersion.java
│       │   └── CafeteriaMenu.java
│       ├── repository
│       ├── service
│       └── dto
│
├── infra
│   ├── auth
│   │   ├── config
│   │   │   └── SecurityConfig.java
│   │   └── firebase
│   │       ├── FirebaseTokenVerifier.java
│   │       └── FirebaseAuthenticationFilter.java
│   │
│   ├── notification
│   │   ├── entity
│   │   │   └── UserNotification.java
│   │   ├── repository
│   │   │   └── UserNotificationRepository.java
│   │   ├── service
│   │   │   ├── NotificationService.java
│   │   │   └── PushNotificationService.java
│   │   └── listener
│   │       └── DomainEventNotificationListener.java
│   │
│   ├── storage
│   │   └── FileStorageService.java
│   │
│   ├── websocket
│   │   ├── config
│   │   │   └── WebSocketConfig.java
│   │   └── handler
│   │       └── ChatWebSocketHandler.java
│   │
│   └── scheduler
│       ├── NoticeScheduler.java
│       └── PartyCleanupScheduler.java
│
├── common
│   ├── entity
│   │   └── BaseTimeEntity.java
│   ├── exception
│   ├── response
│   └── util
│
└── api
    ├── member
    │   └── MemberController.java
    ├── taxiparty
    │   ├── PartyController.java
    │   └── JoinRequestController.java
    ├── chat
    │   └── ChatController.java
    ├── board
    │   └── BoardController.java
    ├── notice
    │   └── NoticeController.java
    ├── academic
    │   └── AcademicController.java
    └── support
        └── SupportController.java
```

---

## 7. 핵심 엔티티 설계

### 7.1 Member 도메인

```java
@Entity
@Table(name = "members")
public class Member extends BaseTimeEntity {
    @Id
    private String id;  // Firebase UID 또는 자체 UUID

    @Column(nullable = false, unique = true)
    private String email;

    private String displayName;
    private String studentId;
    private String department;
    private String photoUrl;
    private String realname;
    private boolean isAdmin;

    @Embedded
    private BankAccount bankAccount;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LinkedAccount> linkedAccounts = new ArrayList<>();

    @Embedded
    private NotificationSetting notificationSetting;
}

@Embeddable
public class BankAccount {
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private boolean hideName;
}

@Embeddable
public class NotificationSetting {
    private boolean allNotifications = true;
    private boolean partyNotifications = true;
    private boolean noticeNotifications = true;
    private boolean boardLikeNotifications = true;
    private boolean boardCommentNotifications = true;
    private boolean systemNotifications = true;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "JSON")
    private Map<String, Boolean> noticeNotificationsDetail;
}

@Entity
@Table(name = "linked_accounts")
public class LinkedAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String provider;  // google
    private String providerId;
    private String email;
    private String displayName;
    private String photoUrl;
}
```

### 7.2 TaxiParty 도메인

```java
@Entity
@Table(name = "parties")
public class Party extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String leaderId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "departure_name")),
        @AttributeOverride(name = "lat", column = @Column(name = "departure_lat")),
        @AttributeOverride(name = "lng", column = @Column(name = "departure_lng"))
    })
    private Location departure;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name", column = @Column(name = "destination_name")),
        @AttributeOverride(name = "lat", column = @Column(name = "destination_lat")),
        @AttributeOverride(name = "lng", column = @Column(name = "destination_lng"))
    })
    private Location destination;

    private LocalDateTime departureTime;
    private int maxMembers;

    @ElementCollection
    @CollectionTable(name = "party_members", joinColumns = @JoinColumn(name = "party_id"))
    private List<PartyMember> members = new ArrayList<>();

    // PartyMember (Embeddable)
    // @Column(name = "member_id") String memberId;
    // @Column(name = "joined_at") LocalDateTime joinedAt;

    @ElementCollection
    @CollectionTable(name = "party_tags", joinColumns = @JoinColumn(name = "party_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    private String detail;

    @Enumerated(EnumType.STRING)
    private PartyStatus status = PartyStatus.OPEN;

    @Enumerated(EnumType.STRING)
    private EndReason endReason;

    private LocalDateTime endedAt;

    @Embedded
    private Settlement settlement;

    // === 비즈니스 메서드 ===

    public void close() {
        validateLeaderAction();
        this.status = PartyStatus.CLOSED;
    }

    public void arrive(int taxiFare) {
        validateLeaderAction();
        this.status = PartyStatus.ARRIVED;
        this.settlement = Settlement.create(taxiFare, this.members);
    }

    public void end(EndReason reason) {
        this.status = PartyStatus.ENDED;
        this.endReason = reason;
        this.endedAt = LocalDateTime.now();
    }

    public void addMember(String memberId) {
        if (this.members.size() >= this.maxMembers) {
            throw new PartyFullException();
        }
        this.members.add(memberId);
    }

    public void removeMember(String memberId) {
        this.members.remove(memberId);
    }

    public boolean isMember(String memberId) {
        return this.members.contains(memberId);
    }

    public boolean isLeader(String memberId) {
        return this.leaderId.equals(memberId);
    }
}

@Embeddable
public class Location {
    private String name;
    private Double lat;
    private Double lng;
}

public enum PartyStatus {
    OPEN, CLOSED, ARRIVED, ENDED
}

public enum EndReason {
    ARRIVED, FORCE_ENDED, CANCELLED, TIMEOUT, WITHDRAWED
}

// ⚠️ JPA 주의: @Embeddable 내부 @ElementCollection은 JPA 표준 비호환.
// Party 엔티티에서 직접 @ElementCollection으로 member_settlements를 관리하거나,
// Settlement를 별도 @Entity로 분리하는 방식으로 구현 시 변경 필요.
@Embeddable
public class Settlement {
    @Enumerated(EnumType.STRING)
    private SettlementStatus status = SettlementStatus.PENDING;

    private Integer perPersonAmount;

    @ElementCollection
    @CollectionTable(name = "member_settlements", joinColumns = @JoinColumn(name = "party_id"))
    @MapKeyColumn(name = "member_id")
    private Map<String, MemberSettlement> memberSettlements = new HashMap<>();

    // members: 리더를 제외한 멤버만 포함 (리더는 수금자이므로 정산 대상 아님)
    // 선행 조건: nonLeaderMembers.size() > 0 (API /arrive에서 NO_MEMBERS_TO_SETTLE로 사전 검증)
    public static Settlement create(int taxiFare, List<String> nonLeaderMembers) {
        if (nonLeaderMembers.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_MEMBERS_TO_SETTLE);
        }
        Settlement settlement = new Settlement();
        settlement.perPersonAmount = taxiFare / nonLeaderMembers.size();
        settlement.status = SettlementStatus.PENDING;
        nonLeaderMembers.forEach(m -> settlement.memberSettlements.put(m, new MemberSettlement()));
        return settlement;
    }

    public void markSettled(String memberId) {
        MemberSettlement ms = memberSettlements.get(memberId);
        if (ms != null) {
            ms.markSettled();
        }
        checkAllSettled();
    }

    private void checkAllSettled() {
        boolean allSettled = memberSettlements.values().stream()
            .allMatch(MemberSettlement::isSettled);
        if (allSettled) {
            this.status = SettlementStatus.COMPLETED;
        }
    }
}

@Embeddable
public class MemberSettlement {
    private boolean settled = false;
    private LocalDateTime settledAt;

    public void markSettled() {
        this.settled = true;
        this.settledAt = LocalDateTime.now();
    }
}

@Entity
@Table(name = "join_requests")
public class JoinRequest extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String partyId;

    @Column(nullable = false)
    private String leaderId;

    @Column(nullable = false)
    private String requesterId;

    @Enumerated(EnumType.STRING)
    private JoinRequestStatus status = JoinRequestStatus.PENDING;

    public void accept() {
        validatePending();
        this.status = JoinRequestStatus.ACCEPTED;
    }

    public void decline() {
        validatePending();
        this.status = JoinRequestStatus.DECLINED;
    }

    public void cancel() {
        validatePending();
        this.status = JoinRequestStatus.CANCELED;
    }

    private void validatePending() {
        if (this.status != JoinRequestStatus.PENDING) {
            throw new AlreadyProcessedException();
        }
    }
}

public enum JoinRequestStatus {
    PENDING, ACCEPTED, DECLINED, CANCELED
}
```

### 7.3 Chat 도메인

```java
@Entity
@Table(name = "chat_rooms")
public class ChatRoom extends BaseTimeEntity {
    @Id
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    private String department;
    private String description;
    private String createdBy;
    private boolean isPublic;
    private Integer maxMembers;

    // members는 chat_room_members 테이블의 별도 ChatRoomMember 엔티티로 관리
    // (lastReadAt, muted 필드가 필요하므로 @ElementCollection 대신 @OneToMany 사용)
    private int memberCount;
    private int messageCount;

    @Embedded
    private LastMessage lastMessage;

    // 팩토리 메서드
    public static ChatRoom forParty(String partyId) {
        ChatRoom room = new ChatRoom();
        room.id = "party:" + partyId;
        room.type = ChatRoomType.PARTY;
        room.isPublic = false;
        return room;
    }
}

// ChatRoomMember — chat_room_members 테이블 매핑
@Entity
@Table(name = "chat_room_members")
public class ChatRoomMember {
    @Id @GeneratedValue
    private Long id;

    private String chatRoomId;
    private String userId;
    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;
    private boolean muted;
}

public enum ChatRoomType {
    UNIVERSITY, DEPARTMENT, GAME, CUSTOM, PARTY
}

@Embeddable
public class LastMessage {
    private String text;
    private String senderId;
    private String senderName;
    private LocalDateTime timestamp;
}

@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String chatRoomId;

    @Column(nullable = false)
    private String senderId;

    private String senderName;

    @Lob
    private String text;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.TEXT;

    // 파티 채팅 전용 (nullable)
    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "JSON")
    private AccountData accountData;

    @Convert(converter = JsonConverter.class)
    @Column(columnDefinition = "JSON")
    private ArrivalData arrivalData;

    // Minecraft 연동용 (nullable)
    @Enumerated(EnumType.STRING)
    private MessageDirection direction;

    private String source;
    private String minecraftUuid;
}

public enum MessageType {
    TEXT, IMAGE, SYSTEM,
    ACCOUNT, ARRIVED, END  // 파티 채팅 전용
}

public enum MessageDirection {
    MC_TO_APP, APP_TO_MC, SYSTEM
}
```

### 7.4 TaxiParty ↔ Chat 협력 구조

```java
@Service
@RequiredArgsConstructor
public class PartyMessageService {
    private final ChatService chatService;
    private final PartyRepository partyRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 파티 채팅방 생성 (파티 생성 시 호출)
     */
    public void createPartyChatRoom(Party party) {
        ChatRoom chatRoom = ChatRoom.forParty(party.getId(), party.getMembers());
        chatService.createRoom(chatRoom);
    }

    /**
     * 일반 메시지 전송 (Chat 엔진에 위임)
     */
    public void sendMessage(String partyId, String senderId, String text) {
        validatePartyMember(partyId, senderId);
        chatService.sendMessage("party:" + partyId, senderId, text, MessageType.TEXT);
    }

    /**
     * 계좌 공유 메시지 (파티 도메인 규칙)
     */
    public void sendAccountMessage(String partyId, String senderId, BankAccount account) {
        Party party = validatePartyMember(partyId, senderId);

        ChatMessage message = ChatMessage.builder()
            .chatRoomId("party:" + partyId)
            .senderId(senderId)
            .type(MessageType.ACCOUNT)
            .accountData(AccountData.from(account))
            .build();

        chatService.sendMessage(message);
    }

    /**
     * 도착 메시지 (파티 상태 변경 + 메시지)
     */
    @Transactional
    public void sendArrivalMessage(String partyId, String leaderId, int taxiFare) {
        Party party = partyRepository.findById(partyId)
            .orElseThrow(PartyNotFoundException::new);

        if (!party.isLeader(leaderId)) {
            throw new NotPartyLeaderException();
        }

        // 1. 파티 상태 변경
        party.arrive(taxiFare);

        // 2. 도착 메시지 전송
        int perPerson = taxiFare / party.getMembers().size();
        ChatMessage message = ChatMessage.builder()
            .chatRoomId("party:" + partyId)
            .senderId(leaderId)
            .type(MessageType.ARRIVED)
            .arrivalData(ArrivalData.builder()
                .taxiFare(taxiFare)
                .perPerson(perPerson)
                .memberCount(party.getMembers().size())
                .build())
            .build();

        chatService.sendMessage(message);

        // 3. 이벤트 발행 → 알림 전송
        eventPublisher.publishEvent(new PartyArrivedEvent(
            partyId,
            party.getMembers(),
            taxiFare,
            perPerson
        ));
    }

    private Party validatePartyMember(String partyId, String memberId) {
        Party party = partyRepository.findById(partyId)
            .orElseThrow(PartyNotFoundException::new);

        if (!party.isMember(memberId)) {
            throw new NotPartyMemberException();
        }
        return party;
    }
}
```

---

## 8. 마이그레이션 우선순위

### 8.1 단계별 마이그레이션 계획

| 순서 | 도메인 | 우선순위 | 이유 | 예상 복잡도 |
|------|--------|---------|------|------------|
| 1 | **Member** | 필수 | 모든 도메인의 기반, Auth와 함께 | 중 |
| 2 | **TaxiParty** | 필수 | 앱의 핵심 비즈니스 | 상 |
| 3 | **Chat** | 필수 | TaxiParty와 연동, WebSocket | 상 |
| 4 | **Notice** | 높음 | 스케줄러(RSS) 포함, 독립적 | 중 |
| 5 | **Board** | 중간 | 비교적 단순한 CRUD | 중 |
| 6 | **Academic** | 낮음 | 읽기 위주, 낮은 복잡도 | 하 |
| 7 | **Support** | 낮음 | 관리 기능, 마지막 | 하 |

### 8.2 마이그레이션 체크리스트

- [ ] **Phase 1: 기반 구축**
  - [ ] Spring Boot 프로젝트 셋업
  - [ ] MySQL 스키마 설계
  - [ ] Spring Security + Firebase Admin SDK (ID Token 검증) 설정
  - [ ] Member 도메인 구현
    - [ ] Firebase ID Token 검증 필터/인증 컨텍스트 구성 (서버 토큰 발급 없음)

- [ ] **Phase 2: 핵심 비즈니스**
  - [ ] TaxiParty 도메인 구현
  - [ ] Chat 도메인 구현 (WebSocket)
  - [ ] 도메인 이벤트 + Notification 인프라
  - [ ] FCM 푸시 연동

- [ ] **Phase 3: 부가 기능**
  - [ ] Notice 도메인 + RSS 크롤러
  - [ ] Board 도메인
  - [ ] Academic 도메인
  - [ ] Support 도메인

- [ ] **Phase 4: 데이터 마이그레이션**
  - [ ] Firestore → MySQL 데이터 이관 스크립트
  - [ ] 점진적 트래픽 전환
  - [ ] Firebase Functions 비활성화

---

## 참고 문서

- [Firestore 데이터 구조](../firestore-data-structure.md)
- [백엔드 스펙](../SKTaxi-backend-spec.md)
- [역할 정의](./role-definition.md)

---

> **문서 이력**
> - 2026-02-03: 초안 작성 (도메인 분석 완료)
