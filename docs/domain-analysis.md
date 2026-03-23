# Spring 백엔드 도메인 분석

> 최종 수정일: 2026-03-10
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
| 1 | **Member** | Core | 회원 프로필, 계정 정보, 알림 설정 | Member, NotificationSetting, LinkedAccount, BankAccount |
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
  - Member
    - uid, email, nickname, studentId, department
    - photoURL, realname, isAdmin, joinedAt, lastLogin
  - NotificationSetting
    - allNotifications, partyNotifications, noticeNotifications
    - boardLikeNotifications, commentNotifications, bookmarkedPostCommentNotifications
    - systemNotifications
    - academicScheduleNotifications, academicScheduleDayBeforeEnabled, academicScheduleAllEventsEnabled
    - noticeNotificationsDetail (카테고리별 상세 설정)
  - LinkedAccount
    - provider, providerId, email, providerDisplayName, photoURL
  - BankAccount
    - bankName, accountNumber, accountHolder, hideName

특이사항:
  - 모든 도메인에서 참조하는 핵심 엔티티
  - FCM 토큰 관리는 인프라(Notification)로 이동
  - 인증 필터에서 `email` 도메인(`@sungkyul.ac.kr`)을 강제 검증
  - 보호 API는 활성 회원(`members.status = ACTIVE`)만 접근 가능하며, 탈퇴 회원은 `403 MEMBER_WITHDRAWN`으로 차단
  - 회원 생성 시 `nickname`은 기본값 `스쿠리 유저`로 저장
  - 회원 생성 시 `realname`은 provider 프로필 이름(`linked_accounts.provider_display_name`)으로 초기화
  - 회원 생성 시 `members.photo_url`은 `null`, 소셜 프로필 이미지(`picture`)는 `linked_accounts.photo_url`에만 저장
  - `linked_accounts.provider`는 `GOOGLE`, `PASSWORD`, `UNKNOWN`을 사용
  - 소셜 로그인(`GOOGLE`)이 아닌 경우 `linked_accounts`는 `provider`를 제외한 provider 부가 컬럼을 `null`로 저장
  - `linked_accounts.providerId`는 provider 계정 식별자(예: `firebase.identities[<sign_in_provider>][0]`)를 저장하며, 비소셜 로그인에서는 `null`
  - 회원 탈퇴는 hard delete 대신 soft delete tombstone(`status`, `withdrawnAt`)으로 관리
  - 탈퇴 시 `members` row는 보존하되 개인정보를 스크럽하고, `linked_accounts`는 전량 삭제
  - 탈퇴한 동일 Firebase UID는 `POST /v1/members`에서 재활성화하지 않고 `409 WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 반환
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

실시간 채널 책임 분리:
  - Party SSE (`/v1/sse/parties`): 파티 카드 목록/상태 전이/멤버 변동 브로드캐스트
  - JoinRequest SSE (`/v1/sse/parties/{partyId}/join-requests`): 파티 리더용 동승 요청 리스트 실시간 갱신
  - JoinRequest SSE (`/v1/sse/members/me/join-requests`): 요청자 본인 요청 상태 실시간 갱신
  - Chat WebSocket: 채팅 메시지/채팅방 요약 실시간 갱신

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

정산 정책:
  - 앱 내 결제/송금 기능은 제공하지 않으며, 향후에도 제공하지 않음
  - 정산 상태(`memberSettlements`) 변경은 파티 리더만 가능
  - 일반 멤버는 자신의 정산 상태를 직접 변경할 수 없음
  - `perPersonAmount`는 `taxiFare / 정산대상인원` 정수 나눗셈(버림)으로 계산
  - 정수 나눗셈으로 생기는 잔여 1원 단위 금액은 서버에서 분배하지 않음(리더 현장 정산 정책)

상태 머신:
  Party:
    OPEN → CLOSED       (리더: 모집 마감)
    CLOSED → OPEN       (리더: 모집 재개)
    OPEN|CLOSED 내 정보 수정 (리더: departureTime/detail만)
    OPEN|CLOSED → ARRIVED  (리더: 도착 처리 → 정산 시작)
    ARRIVED 상태에서 멤버 정산 완료 처리 (모든 멤버 완료 시 settlementStatus=COMPLETED)
    ARRIVED → ENDED     (리더 종료 요청(`/end`) → endReason=FORCE_ENDED, 미정산 멤버 있어도 가능)
    OPEN|CLOSED → ENDED     (리더 취소)
    OPEN|CLOSED|ARRIVED → ENDED (스케줄러 timeout 자동 종료)

  JoinRequest: PENDING → ACCEPTED | DECLINED | CANCELED
    - CANCELED: 요청자 본인만 취소 가능 (PENDING 상태에서만)
    - 리더는 DECLINE으로 거절 (CANCEL 아님)
    - ACCEPTED 처리로 멤버가 정원(`maxMembers`)에 도달하면 Party 상태를 자동으로 CLOSED 전이

동시성 제어:
  - Party 엔티티의 `@Version` 기반 Optimistic Lock으로 동시 동승 요청/수락 충돌을 방어
  - 같은 사용자의 파티 생성/동승 요청/수락 경로는 `members` row lock으로 직렬화하여 `ALREADY_IN_PARTY` 불변식을 보존
  - 같은 사용자의 동승 요청은 `members` row lock 하에서 `PENDING` 존재 여부를 확인해 동일 파티 live request 중복을 차단하고, 취소/거절 이후 재요청 이력은 허용
  - 충돌 시 `PARTY_CONCURRENT_MODIFICATION` 에러로 재시도 유도

저장소 설계:
  - `PartyMember`, `PartyTag`, `MemberSettlement`는 `Party` aggregate 내부 컬렉션으로 관리
  - 영속화는 `PartyRepository` 단일 저장으로 처리(cascade + orphanRemoval)
  - 하위 엔티티 전용 Repository는 현재 운영 코드에서 사용하지 않음

파티 수정 정책:
  - `PATCH /v1/parties/{id}`는 `departureTime`, `detail`만 허용 (화이트리스트)
  - `OPEN`, `CLOSED` 상태에서만 수정 가능
  - `CLOSED` 상태에서 수정해도 상태 자동 변경 없음 (`reopen`으로만 모집 재개)

파티 자동 종료 정책 (@Scheduled):
  - 실행 주기: 4시간마다
  - 기준: createdAt 기준 12시간 초과한 파티
  - 대상: ENDED가 아닌 모든 파티 (OPEN, CLOSED, ARRIVED 포함)
  - 처리: status=ENDED, endReason=TIMEOUT
  - 구현: Firebase Cloud Functions onSchedule → Spring @Scheduled 대체

endReason 종류:
  - ARRIVED: (레거시) 과거 자동 종료 정책에서 사용된 종료 사유
  - FORCE_ENDED: 리더 종료 요청(`/end`)으로 종료 (미정산 멤버 있을 수 있음)
  - CANCELLED: 리더 취소
  - TIMEOUT: 자동 종료 (12시간 초과)
  - WITHDRAWED: 리더 탈퇴로 인한 종료

회원 탈퇴 연계 정책:
  - 리더 탈퇴 시 active party는 `ENDED + WITHDRAWED`로 종료
  - 리더 탈퇴와 동시에 해당 파티의 `PENDING` join request는 `DECLINED`로 정리
  - 일반 멤버 탈퇴는 `OPEN`, `CLOSED` 상태에서만 자동 이탈 허용
  - 일반 멤버가 `ARRIVED` 파티에 속해 있으면 정산 회피 방지를 위해 회원 탈퇴를 거부
  - 탈퇴 회원이 요청자인 `PENDING` join request는 `CANCELED`로 정리

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
  - 채팅방 목록 실시간: `/user/queue/chat-rooms` 사용자 전용 요약 채널 1개 구독
  - 채팅방 상세 실시간: `/topic/chat/{chatRoomId}` 방 단위 구독
  - 채팅방 메시지 전송: `/app/chat/{chatRoomId}`
  - STOMP 에러 수신: `/user/queue/errors` (`errorCode/message/timestamp`)
  - WS 인가: CONNECT 인증 후에도 SEND/SUBSCRIBE 시 채팅방 멤버십을 서버에서 추가 검증
  - 미읽음 계산: `message.createdAt > lastReadAt` 기준 (동일 시각은 읽음)
  - `lastReadAt`는 서버 현재 시각과 마지막 메시지 시각을 상한으로 clamp하여 미래 시각 입력으로 인한 unread 왜곡을 방지
  - 방별 다중 구독(모든 방 topic 동시 구독)은 연결 수/브로드캐스트 비용 증가로 사용하지 않음
  - 회원 탈퇴 시 `chat_room_members`는 전부 정리하고 `chat_rooms.member_count`를 즉시 동기화
  - 탈퇴 회원의 기존 WebSocket 세션은 best-effort로 종료하고, 남아 있는 `/topic/chat/**` outbound delivery도 차단
  - 과거 `chat_messages.senderName`은 Phase 10에서 일괄 수정하지 않고 이력 보존을 우선
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
    - parentId (self-reference), isDeleted
    - depth 제한 없음 (무제한 self-reference)
    - 조회 응답은 flat list + `parentId` + `depth`
    - 부모 삭제 정책(B): 부모는 placeholder("삭제된 댓글입니다")로 soft delete, 자식은 유지

  anonymousOrder 계산 규칙:
    - 게시글(postId) 단위로 Map<anonId, order> 관리
    - 댓글 작성 시 anonId가 Map에 없으면 → 새 순번 부여 (현재 Map.size() + 1)
    - 이미 Map에 있으면 → 기존 순번 재사용
    - 댓글 삭제 후 순번 재계산 없음 (삭제된 댓글도 번호 영구 보존)
    - isAnonymous = false이면 anonymousOrder = null
  - PostInteraction
    - userId, postId, isLiked, isBookmarked
    - 좋아요/북마크 카운트 동기화는 Post aggregate와 같은 트랜잭션에서 처리

조회/정렬:
  - 게시글 정렬: latest/popular/mostCommented/mostViewed
  - 내 작성글: GET /v1/members/me/posts
  - 내 북마크글: GET /v1/members/me/bookmarks

카테고리:
  - GENERAL (일반)
  - QUESTION (질문)
  - REVIEW (후기)
  - ANNOUNCEMENT (공지)

회원 탈퇴 연계 정책:
  - 게시글/댓글 본문은 보존
  - `authorId`, `authorName`, `authorProfileImage`는 탈퇴 사용자 익명화 값으로 치환
  - 탈퇴 회원의 좋아요/북마크 기록은 삭제하고 `likeCount`, `bookmarkCount`를 보정
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
  - scheduledRSSFetch (10분 주기, 평일 08:00~19:50)
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
    - title, rssPreview, summary, link, postedAt, category
    - department, author, source
    - rssFingerprint (레거시 링크/날짜 기반 변경 감지용)
    - detailHash (상세 HTML/첨부 변경 감지용)
    - contentHash (실제 내용 기반 dedup용)
    - bodyText (plain text), bodyHtml (HTML), attachments[]
    - detailCheckedAt (상세 재검증 시각)
    - viewCount, likeCount, commentCount
  - NoticeComment
    - id, noticeId, userId, userDisplayName
    - content, isAnonymous, anonId (= "{noticeId}:{userId}")
    - anonymousOrder (서버 계산: Board Comment의 anonymousOrder 계산 규칙과 동일, noticeId 단위 Map 관리)
    - parentId, isDeleted
    - depth 제한 없음 (무제한 self-reference)
    - 조회 응답은 Board Comment와 동일하게 flat list + `parentId` + `depth`
    - 부모 삭제 정책: Board Comment와 동일하게 placeholder soft delete
  - NoticeReadStatus
    - userId, noticeId, isRead, readAt
  - NoticeLike
    - userId, noticeId
    - 공지 좋아요 중복 방지 및 likeCount 동기화 용도
  - AppNotice
    - id, title, content
    - category (UPDATE, MAINTENANCE, EVENT, GENERAL)
    - priority (HIGH, NORMAL, LOW)
    - imageUrls[], actionUrl, publishedAt

동기화 정책:
  - 스케줄: 평일 08:00~19:50, 10분 주기, Asia/Seoul
  - 링크 기반 안정 ID는 유지한다.
  - `rssFingerprint`는 레거시(`title|fullLink|rawDate`) 기준을 유지한다.
  - `contentHash`는 링크를 제외한 실제 내용 + 상세 본문/첨부 기반으로 계산해 중복을 배제한다.
  - `rssPreview`는 RSS `description/content/contentSnippet` fallback으로 수집한 미리보기 텍스트다.
  - `rssPreview`는 RSS 길이 제한 때문에 중간에서 잘릴 수 있으며, AI 요약을 의미하지 않는다.
  - `summary`는 추후 AI가 생성한 공지 요약을 저장하기 위한 예약 필드다. 현재 공개 API에는 노출하지 않는다.
  - `bodyHtml`은 상세 페이지 `.view-con`에서 수집한 HTML 원문이며, RN 앱이 웹 구조를 최대한 유지해 렌더링할 수 있도록 그대로 저장한다.
  - `bodyText`는 `bodyHtml`에서 태그를 제거하고 줄바꿈/표 셀 구분을 정규화한 내부 텍스트다.
  - 성결대학교 사이트의 TLS 체인 이슈로 인해, 현재 Spring 구현은 공지 RSS/상세 크롤링 경로에서만 TLS 인증서 검증을 비활성화한다.
  - 개별 공지 저장 실패는 전체 동기화를 중단하지 않고 `failed`로 집계한 뒤 다음 공지 처리를 계속한다.
  - 상세 재크롤링 조건:
    - 신규 공지
    - RSS 메타 변경
    - `detailHash` 없음
    - 마지막 상세 검증 시점이 24시간 초과
  - 관리자 수동 sync는 상세 재크롤링을 강제로 수행한다.

회원 탈퇴 연계 정책:
  - 공지 본문은 회원과 독립적인 외부 데이터이므로 영향 없음
  - `NoticeComment`는 본문을 유지하고 `userId`, `userDisplayName`만 익명화
  - `NoticeLike`, `NoticeReadStatus`는 탈퇴 회원 기준으로 정리

향후 확장 준비:
  - AI 요약은 `summary` 컬럼에 저장하고, `contentHash`가 바뀌면 기존 AI 요약을 무효화한다.
  - RAG/공지 챗봇은 `bodyText`를 기준으로 chunking/embedding을 수행하고, `title/category/postedAt/link`를 citation 메타데이터로 사용한다.

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
    - id, userId, semester
    - unique(userId, semester)
    - courses[] via UserTimetableCourse
    - 같은 시간표 내 동일 강의 중복 추가 금지
    - 시간표 강의 추가 시 dayOfWeek/startPeriod/endPeriod overlap 차단
    - 조회 응답은 `courses[] + slots[]` 구조로 프론트 렌더링을 지원
    - `GET /v1/timetables/my`는 semester 미지정 시 현재 날짜 기준 `2~7월 -> yyyy-1`, `8~12월 -> yyyy-2`, `1월 -> 전년도 yyyy-2` 규칙으로 현재 학기를 해석
    - 실제 학교 학기 시작은 3월/9월이지만, 수강신청/시간표 준비 수요를 반영해 스쿠리 학기 기준을 한 달 앞당겨 사용
  - UserTimetableCourse
    - timetableId, courseId
  - AcademicSchedule
    - id, title, startDate, endDate
    - type (SINGLE, MULTI), isPrimary, description
  - Course 검색
    - semester/department/professor/dayOfWeek/grade 필터 지원
    - search는 강의명/과목코드/카테고리/교수/강의실/비고를 대상으로 한다

학사 일정 알림 정책 (후속 구현):
  - 기본 트리거 기준일은 `startDate`다.
  - 기본 발송 시각은 당일 오전 09:00이다.
  - 기본 대상은 중요 일정(`isPrimary = true`)이다.
  - 사용자 옵션으로 전날 오전 09:00 추가 발송과 모든 일정 대상 확장을 허용한다.
  - 실제 발송은 Notification 인프라(FCM + 인앱 인박스) Phase에서 처리한다.

회원 탈퇴 연계 정책:
  - `user_timetables`는 탈퇴 회원 기준으로 전량 삭제
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
    - status (PENDING, IN_PROGRESS, RESOLVED), adminMemo
  - Report
    - id, targetType (POST, COMMENT, MEMBER)
    - targetId, targetAuthorId, category, reason
    - reporterId, status (PENDING, REVIEWING, ACTIONED, REJECTED)
    - action, adminMemo
    - duplicate policy: unique(reporterId, targetType, targetId)
  - AppVersion
    - platform (ios, android)
    - minimumVersion, forceUpdate, message
    - title, showButton, buttonText, buttonUrl
    - fallback policy: 저장 데이터가 없으면 `minimumVersion=1.0.0`, `forceUpdate=false`, `showButton=false`
  - CafeteriaMenu
    - weekId, weekStart, weekEnd
    - menus: Map<date, Map<restaurant, items[]>>
  - AdminAuditLog
    - id, actorId, action, targetType, targetId
    - diffBefore (JSON snapshot), diffAfter (JSON snapshot), timestamp
    - 상태 변경 Admin API(`POST`, `PUT`, `PATCH`, `DELETE`)를 공통 interceptor/filter 계층에서 자동 기록
    - `targetId`는 UUID 외에 semester/platform/weekId 같은 운영 식별자도 허용

운영 공통 규약:
  - `/v1/admin/**`는 공통 인가 어노테이션(`@AdminApiAccess`)과 `ADMIN_REQUIRED` 표준 응답으로 보호
  - 문의/신고 목록은 `AdminPageRequestPolicy` 기준 `page=0`, `size=20`, `size<=100`, 정렬 `createdAt DESC`를 사용
  - CSV export와 자유 검색은 Phase 11에서 문서 규약만 정리하고 런타임 API는 추가하지 않음

회원 탈퇴 연계 정책:
  - inquiry/report record는 운영 추적 목적상 보존
  - inquiry의 구조화 개인정보(`userEmail`, `userName`, `userRealname`, `userStudentId`)만 마스킹
  - 자유서술 `content` 전체 자동 마스킹은 Phase 10 범위에서 제외
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
| **Auth** | Firebase Auth (Google Sign-In) | Spring Security + Firebase Admin SDK (ID Token 검증, SecurityContext 설정) |
| **Push** | FCM + Cloud Functions | FCM (Firebase Admin SDK) |
| **Notification** | userNotifications 컬렉션 + users.fcmTokens[] | `user_notifications` + `fcm_tokens` 테이블 + 이벤트 리스너 |
| **Storage** | Firebase Storage | `StorageRepository` 추상화 + LOCAL 파일시스템 기본 구현, FIREBASE provider 포함 후속 S3/OCI/GCS provider 확장 |
| **Realtime** | Firestore onSnapshot | SSE + WebSocket (STOMP over SockJS `/ws` + native `/ws-native`) |
| **Scheduler** | Cloud Functions onSchedule | Spring Scheduler / Quartz |
| **Audit** | adminAuditLogs 컬렉션 | Spring AOP + AuditLog 테이블 |

실시간 채널 분리 정책:
- SSE:
  - `/v1/sse/parties` (파티 목록/상태)
  - `/v1/sse/parties/{partyId}/join-requests` (파티 리더용 동승요청 목록/상태)
  - `/v1/sse/members/me/join-requests` (요청자 본인 동승요청 상태)
  - 알림, 게시물 목록/조회수
- WebSocket: 채팅(목록 요약 `/user/queue/chat-rooms`, 상세 메시지 `/topic/chat/{chatRoomId}`, 전송 `/app/chat/{chatRoomId}`)
  - 파티 멤버 변화(수락/탈퇴/강퇴)는 `chat_room_members`와 `chat_rooms.member_count`를 즉시 동기화한다.

### 5.2 Notification 인프라 상세

```
역할: 도메인 이벤트를 수신하여 사용자에게 알림 전달

구성요소:
  - UserNotification (인앱 알림 인박스)
  - FcmToken (멀티 디바이스 토큰 저장)
  - PushNotificationService (FCM 전송)
  - DomainEventNotificationListener (이벤트 수신)
  - NotificationSseService (실시간 알림/미읽음 수 SSE)
  - AcademicScheduleReminderScheduler (09:00 Asia/Seoul 리마인더 발행)

UserNotification 엔티티:
  - id, userId, type, title, message
  - data (JSON), isRead, readAt, createdAt

알림 타입:
  - PARTY_CREATED, PARTY_JOIN_REQUEST, PARTY_JOIN_ACCEPTED
  - PARTY_JOIN_DECLINED, PARTY_CLOSED, PARTY_ARRIVED
  - PARTY_ENDED, MEMBER_KICKED, SETTLEMENT_COMPLETED
  - CHAT_MESSAGE, POST_LIKED, COMMENT_CREATED
  - NOTICE, APP_NOTICE, ACADEMIC_SCHEDULE

정책 원칙:
  - Spring Notification 인프라는 현행 RN + Firebase Cloud Functions 운영 정책을 기본으로 이관한다.
  - 저장 모델은 Firestore가 아니라 MySQL `user_notifications`, `fcm_tokens`를 사용한다.
  - 상태 변경 성공 이후 `after-commit`으로 이벤트를 발행한다.
  - 인앱 저장 실패/푸시 실패는 핵심 비즈니스 트랜잭션을 롤백시키지 않는다.
  - 회원 탈퇴 시 `user_notifications`, `fcm_tokens`는 전량 삭제하고 회원 개인 SSE 연결도 종료한다.
  - Phase 8 런타임은 `allNotifications`를 마스터 토글로 정규화해 적용한다. 단, `AppNoticePriority.HIGH`와 파티 채팅 알림은 문서화된 예외 정책을 따른다.
  - FCM push payload는 특정 RN legacy type에 종속되지 않고, canonical `NotificationType` + 리소스 식별자(`partyId`, `noticeId` 등)를 사용한다.
  - 플랫폼별 알림 표현(sound/channel)은 `PushPresentationProfile`로 분리한다. 현재 `PARTY`, `CHAT`, `NOTICE`, `DEFAULT` 프로필을 사용한다.
  - 서버는 클라이언트 route/screen 이름을 payload에 넣지 않으며, 클라이언트가 `type + data`를 기준으로 이동 대상을 결정한다.

현행 운영 정책 기준:
  - `PARTY_CREATED`: 생성자 제외 전체 사용자 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 미생성
  - `PARTY_JOIN_REQUEST`: 파티 리더 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 생성
  - `PARTY_JOIN_ACCEPTED` / `PARTY_JOIN_DECLINED`: 요청자 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 생성
  - `PARTY_CLOSED`: 리더 제외 파티 멤버 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 미생성
  - `PARTY_ARRIVED`: 리더 제외 파티 멤버 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 생성
  - `SETTLEMENT_COMPLETED`: 파티 전체 멤버 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 생성
  - `MEMBER_KICKED`: 강퇴된 멤버 대상, 자진 이탈과 리더 제외, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 생성
  - `PARTY_ENDED`: 리더 제외 파티 멤버 대상, `allNotifications` + `partyNotifications` 반영, 인앱 인박스 생성
  - `CHAT_MESSAGE`(공개 채팅): 채팅방 멤버 대상, `allNotifications` + 채팅방 mute 반영, 인앱 인박스 미생성
  - `CHAT_MESSAGE`(파티 채팅): 파티 멤버 대상, 채팅 mute 중심 parity를 유지하고 전역 토글은 현재 미반영, 인앱 인박스 미생성
  - `POST_LIKED`: 게시글 작성자 대상, 자기 좋아요 제외, `allNotifications` + `boardLikeNotifications` 반영, 인앱 인박스 생성
  - `COMMENT_CREATED`(게시글): 게시글 작성자, 부모 댓글 작성자, 게시글 북마크 사용자 대상, 자기 자신 제외, `allNotifications` + `commentNotifications` + `bookmarkedPostCommentNotifications` 반영, 중복 대상자는 1회 dedupe 후 인앱 인박스 생성
  - `COMMENT_CREATED`(공지): 현재 `Notice.author`는 문자열 필드만 있어 공지 작성자 식별이 불가능하므로, 부모 댓글 작성자 대상 답글 알림만 발송하며 `allNotifications` + `commentNotifications`를 반영
  - `NOTICE`: `allNotifications` + `noticeNotifications` + `noticeNotificationsDetail` 반영
  - `APP_NOTICE`: 일반 공지는 `allNotifications` + `systemNotifications` 반영, `AppNoticePriority.HIGH`는 설정 무시 강제 발송
  - `ACADEMIC_SCHEDULE`: `allNotifications` + `academicScheduleNotifications` 반영, 기본은 중요 일정(`isPrimary=true`)만 대상

Phase 8 댓글 알림 정책:
  - Board/Notice 공통 댓글 알림 설정은 `commentNotifications`를 사용한다.
  - `COMMENT_CREATED`(게시글)은 게시글 작성자/부모 댓글 작성자 외에도 "해당 게시글을 북마크한 사용자"를 수신 대상에 포함한다.
  - 게시글 북마크 기반 댓글 알림은 `bookmarkedPostCommentNotifications`로 분리한다.
  - 동일 사용자가 여러 수신 조건에 동시에 해당하면 푸시/인앱 인박스는 1회만 생성한다.

학사 일정 리마인더 정책:
  - 이벤트명: `AcademicScheduleReminder`
  - 기본 발송 시각: 오전 09:00 (Asia/Seoul)
  - 기본 대상: 중요 일정(`isPrimary = true`)
  - 사용자 옵션:
    - 전날 오전 09:00 추가
    - 모든 일정 대상 확장
  - 멀티데이 일정은 `startDate`를 기준으로 리마인더를 계산한다.
  - 구현 시 `NotificationSetting`은 최소 아래 설정을 추가한다:
    - `academicScheduleNotifications`
    - `academicScheduleDayBeforeEnabled`
    - `academicScheduleAllEventsEnabled`
```

---

## 6. 패키지 구조

> 현재 코드베이스(Phase 7 Support 구현 반영 시점) 기준 구조입니다.

```
com.skuri.skuri_backend
├── common
│   ├── config
│   ├── dto
│   ├── entity
│   └── exception
│
├── domain
│   ├── app
│   │   └── controller
│   │       └── AppNoticeController.java
│   │
│   ├── support
│   │   ├── controller
│   │   │   ├── AppVersionController.java
│   │   │   ├── InquiryController.java
│   │   │   ├── ReportController.java
│   │   │   ├── CafeteriaMenuController.java
│   │   │   ├── InquiryAdminController.java
│   │   │   ├── ReportAdminController.java
│   │   │   ├── AppVersionAdminController.java
│   │   │   └── CafeteriaMenuAdminController.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── entity
│   │   ├── exception
│   │   ├── repository
│   │   └── service
│   │
│   ├── member
│   │   ├── controller
│   │   │   └── MemberController.java
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── entity
│   │   │   ├── Member.java
│   │   │   ├── LinkedAccount.java
│   │   │   ├── LinkedAccountProvider.java
│   │   │   ├── BankAccount.java
│   │   │   └── NotificationSetting.java
│   │   ├── exception
│   │   ├── repository
│   │   │   ├── MemberRepository.java
│   │   │   ├── MemberRepositoryCustom.java
│   │   │   ├── MemberRepositoryImpl.java
│   │   │   └── LinkedAccountRepository.java
│   │   └── service
│   │
│   ├── notification
│   │   ├── controller
│   │   ├── dto
│   │   │   ├── request
│   │   │   └── response
│   │   ├── entity
│   │   ├── event
│   │   ├── exception
│   │   ├── model
│   │   ├── repository
│   │   ├── scheduler
│   │   └── service
│   │
│   └── taxiparty
│       ├── controller
│       │   ├── PartyController.java
│       │   ├── JoinRequestController.java
│       │   └── PartySseController.java
│       ├── dto
│       │   ├── request
│       │   └── response
│       ├── entity
│       │   ├── Party.java
│       │   ├── PartyMember.java
│       │   ├── PartyTag.java
│       │   ├── MemberSettlement.java
│       │   └── JoinRequest.java
│       ├── exception
│       ├── repository
│       ├── scheduler
│       │   ├── PartySseHeartbeatScheduler.java
│       │   └── PartyTimeoutScheduler.java
│       └── service
│           ├── TaxiPartyService.java
│           ├── PartySseService.java
│           ├── JoinRequestSseService.java
│           ├── PartyTimeoutBatchService.java
│           └── PartyTimeoutCommandService.java
│
└── infra
	    └── auth
	        ├── config
	        │   ├── ApiAccessDeniedHandler.java
	        │   ├── ApiAuthenticationEntryPoint.java
	        │   ├── FirebaseAuthProperties.java
	        │   ├── SecurityConfig.java
	        │   ├── FirebaseConfig.java
	        │   ├── FirebaseCredentialsCondition.java
	        │   └── FirebaseAuthEnvironmentGuard.java
        └── firebase
            ├── AuthenticatedMember.java
            ├── AuthenticatedMemberSupport.java
            ├── DisabledFirebaseTokenVerifier.java
            ├── EmailDomainRestrictedException.java
            ├── FirebaseAdminTokenVerifier.java
            ├── FirebaseAuthenticationFilter.java
            ├── FirebaseTokenClaims.java
            ├── FirebaseTokenVerifier.java
            └── InvalidFirebaseTokenException.java
```

> `chat`, `board`, `notice`, `academic`, `support`, `notification` 패키지는
> 로드맵 Phase 2 이후 순차 구현 시 같은 `domain/*`, `infra/*` 규칙으로 확장한다.

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

    @Column(name = "nickname")
    private String nickname;
    private String studentId;
    private String department;
    private String photoUrl;
    private String realname;
    private boolean isAdmin;

    @Enumerated(EnumType.STRING)
    private MemberStatus status; // ACTIVE, WITHDRAWN

    @Embedded
    private BankAccount bankAccount;

    @Embedded
    private NotificationSetting notificationSetting;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;
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
    private boolean commentNotifications = true;
    private boolean bookmarkedPostCommentNotifications = true;
    private boolean systemNotifications = true;
    private boolean academicScheduleNotifications = true;
    private boolean academicScheduleDayBeforeEnabled = true;
    private boolean academicScheduleAllEventsEnabled = false;

    @Convert(converter = BooleanMapJsonConverter.class)
    @Column(columnDefinition = "JSON")
    private Map<String, Boolean> noticeNotificationsDetail;
}

@Entity
@Table(
    name = "linked_accounts",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_linked_account_member_provider", columnNames = {"member_id", "provider"})
    }
)
public class LinkedAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private LinkedAccountProvider provider;  // GOOGLE, PASSWORD, UNKNOWN

    @Column(name = "provider_id")
    private String providerId;

    private String email;

    @Column(name = "provider_display_name")
    private String providerDisplayName;

    // 소셜 계정 프로필 이미지 (비소셜 로그인은 null)
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
    private MessageType type;
    private String text;
    private String senderId;
    private String senderName;
    private LocalDateTime createdAt;
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

- [x] **Phase 1: 기반 구축**
  - [x] Spring Boot 프로젝트 셋업
  - [x] MySQL 스키마 설계
  - [x] Spring Security + Firebase Admin SDK (ID Token 검증) 설정
  - [x] Member 도메인 구현
    - [x] Firebase ID Token 검증 필터/인증 컨텍스트 구성 (서버 토큰 발급 없음)
    - [x] `members.isAdmin` 기반 `ROLE_ADMIN` authority 부여 + `@PreAuthorize("hasRole('ADMIN')")` 적용
    - [x] 공개 API(`GET /v1/app-versions/**`, `GET /v1/app-notices/**`, `GET /v3/api-docs/**`, `GET /swagger-ui/**`, `GET /scalar/**`) permitAll
    - [x] 보호 API 미인증 요청 401, 이메일 도메인 불일치 403, 관리자 API 비권한 요청 `403 ADMIN_REQUIRED`

- [ ] **Phase 2: 핵심 비즈니스**
  - [x] TaxiParty 도메인 구현
  - [x] Chat 도메인 구현 (WebSocket)
  - [x] 도메인 이벤트 + Notification 인프라
  - [x] FCM 푸시 연동

- [x] **Phase 3: 부가 기능**
  - [x] Notice 도메인 + RSS 크롤러
  - [x] Board 도메인 (무제한 depth 댓글, flat list 조회, 부모 삭제 정책 B 반영)
  - [x] Academic 도메인
  - [x] Support 도메인

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
> - 2026-03-05: Board 도메인 구현 반영 — 댓글 depth 1 제한, 부모 placeholder soft delete 정책(B), 내 게시글/북마크 조회 책임 추가
> - 2026-03-07: Board/Notice 공통 Comment 정책 구현 반영 — 무제한 depth, flat list 응답, 댓글 알림 설정 분리
> - 2026-03-08: Phase 8 Notification 인프라 반영 — RDB 저장 모델(`user_notifications`, `fcm_tokens`), after-commit 이벤트, 학사 일정 알림 설정, `PARTY_*` canonical enum 동기화
> - 2026-03-09: Phase 10 Member 라이프사이클 반영 — soft delete tombstone, 동일 UID 재가입 차단, TaxiParty/Chat/Board/Notice/Support/Notification/Academic 탈퇴 정합성 정책 추가
> - 2026-03-10: Phase 11 Admin 공통 인프라 반영 — `AdminAuditLog` 엔티티를 `actorId/action/targetType/targetId/diffBefore/diffAfter/timestamp` 구조로 구체화하고, Support 운영 목록/인가 공통 규약을 반영
