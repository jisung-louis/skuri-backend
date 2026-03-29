# SKURI Backend - 프로젝트 개요

## 목적
성결대학교 학생 대상 택시 합승/커뮤니티/공지/학사 서비스 백엔드다. 핵심 도메인은 TaxiParty이며, 알림과 실시간 기능은 핵심 트랜잭션의 부가효과로 분리한다.

## 기술 스택
- Java 21, Spring Boot 4.0.3, Gradle
- MySQL 8.4 (prod), H2 (test)
- Spring Data JPA + Hibernate
- Firebase Admin SDK (ID Token 검증, FCM sender)
- 실시간: WebSocket(STOMP), SSE
- OpenAPI: springdoc Swagger UI + Scalar

## 도메인 구조
- member: 회원, 프로필, 알림 설정, FCM 토큰, 탈퇴/재가입 정책
- taxiparty: 파티 생성/참여/정산/상태 전이, SSE, 택시 history/summary, 홈 목록 요약 `PartySummaryResponse.participantSummaries`로 현재 멤버 `id/photoUrl/nickname/isLeader`를 함께 제공
- chat: 공개 채팅방/파티 채팅방, SYSTEM/ARRIVED/END 서버 메시지, 공개방 seed, 메시지 payload `senderPhotoUrl`은 `members.photo_url`만 사용
- board: 게시글/댓글/좋아요/북마크, 이미지 썸네일 규칙
- notice: 학교 공지/댓글/읽음/북마크/앱 공지
- academic: 강의/시간표/학사 일정, `GET /v1/timetables/my/semesters`, 직접 입력 강의(`UserTimetableManualCourse`), 시간표 응답 `courses[] + slots[]` + `isOnline` 계약
- campus: 캠퍼스 배너 공개/관리자 API
- support: 문의/신고(게시글/댓글/회원/채팅 메시지/일반 채팅방/택시파티)/앱 버전/법적 문서/학식
- notification: 인앱 인박스, FCM, SSE, 이벤트 기반 알림 처리

## Inquiry Attachment 메모
- 문의 첨부 이미지는 `POST /v1/images?context=INQUIRY_IMAGE` 업로드 결과 메타데이터를 `Inquiry.attachments` JSON 컬럼으로 저장한다.
- `POST /v1/inquiries`의 `attachments`는 optional이며 요청에서 생략하거나 `null`이면 서버에서 빈 배열로 정규화한다.
- 첨부는 최대 3개, 허용 MIME은 `image/jpeg`, `image/png`, `image/webp`다.
- `GET /v1/inquiries/my`, `GET /v1/admin/inquiries`/`PATCH /v1/admin/inquiries/{inquiryId}/status` 응답은 항상 `attachments: []` 형태를 유지하며 null을 반환하지 않는다.
- 회원 탈퇴 후에도 문의 기록과 첨부 이미지 메타데이터는 보존하고, inquiry의 구조화 개인정보만 마스킹한다.

## Legal Document 메모
- `LegalDocument`는 `termsOfUse`, `privacyPolicy` 두 고정 키를 `legal_documents` 테이블에서 관리한다.
- 공개 API는 `GET /v1/legal-documents/{documentKey}`이며 `isActive=true` 문서만 조회 가능하다. 비활성 또는 미존재 문서는 `404 LEGAL_DOCUMENT_NOT_FOUND`를 반환한다.
- 관리자 API는 `GET /v1/admin/legal-documents`, `GET /v1/admin/legal-documents/{documentKey}`, `PUT /v1/admin/legal-documents/{documentKey}`, `DELETE /v1/admin/legal-documents/{documentKey}`다.
- 초기 이용약관/개인정보 처리방침 2건은 `seed_migrations` 기반 1회성 seed migration으로 적재한다. 이후에는 관리자 CRUD로만 수정한다.
- 문서 본문 구조는 프론트 계약과 맞춘 `banner`, `sections`, `footerLines`를 JSON 컬럼으로 저장한다.

## 인프라/공통
- 공통 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`, `ErrorCode` 중심으로 처리한다.
- 로컬 프로필은 `local`, `local-emulator`, 운영은 `prod`, 자동 테스트는 `test`를 사용한다.
- OpenAPI는 로컬에서 노출하고 `prod`에서는 기본 비노출이다.
- 상태 변경 이후 알림/외부 후처리는 after-commit semantics를 따른다.
- Admin API는 `@AdminApiAccess`, `ADMIN_REQUIRED`, `admin_audit_logs` 기반 정책을 사용한다.

## Admin Member API 메모
- `member` 도메인은 관리자 백오피스 회원 관리 API를 제공한다: `GET /v1/admin/members`, `GET /v1/admin/members/{memberId}`, `GET /v1/admin/members/{memberId}/activity`, `PATCH /v1/admin/members/{memberId}/admin-role`.
- 관리자 목록은 `query/status/isAdmin/department` 필터와 `sortBy/sortDirection` 정렬을 지원한다. 기본값은 `joinedAt desc`, null 값은 항상 마지막이며 이름 컬럼은 `realname`을 사용한다. `lastLoginOs`, `currentAppVersion`은 최근 활성 FCM 토큰(`coalesce(last_used_at, created_at)` 최신)의 `fcm_tokens.platform`, `fcm_tokens.app_version`을 같은 대표 토큰 기준으로 사용한다.
- `POST /v1/members/me/fcm-tokens`는 optional `appVersion`을 받는다. 신규 토큰 등록 시 미전송하면 `null`로 저장하고, 같은 토큰 재등록 시 `null` 또는 빈 문자열이면 기존 값을 유지한다.
- 관리자 상세는 목록 필드 외에 `photoUrl`, `withdrawnAt`, `bankAccount`, `notificationSetting`을 포함한다.
- 관리자 활동 요약은 ACTIVE 회원만 제공하며, 현재 저장된 post/comment/party/inquiry/report 데이터 기준 count + domain별 recent 5건 read model을 반환한다. 댓글은 삭제되지 않은 comment이면서 부모 post도 삭제되지 않은 경우만 포함하고, 탈퇴 회원은 `409 MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN`으로 비제공 처리한다.
- 관리자 권한 변경은 기존 `members.is_admin` boolean만 갱신하며, 탈퇴 회원은 `409 CONFLICT`, 자기 자신의 권한 변경은 `400 SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED`로 차단한다. 마지막 관리자 수 계산 정책은 후속 결정 전까지 추가하지 않는다.
- 관리자 상세 응답은 `bankAccount`, `notificationSetting`을 유지하지만 admin-role 감사 snapshot은 `id/email/nickname/isAdmin/status` 최소 필드만 저장한다.


## Admin TaxiParty API 메모
- `taxiparty` 도메인은 관리자 백오피스용 TaxiParty API를 제공한다: `GET /v1/admin/parties`, `GET /v1/admin/parties/{partyId}`, `PATCH /v1/admin/parties/{partyId}/status`, `DELETE /v1/admin/parties/{partyId}/members/{memberId}`, `POST /v1/admin/parties/{partyId}/messages/system`, `GET /v1/admin/parties/{partyId}/join-requests`.
- 관리자 목록은 `page/size/status/departureDate/query` 필터를 지원하고, 기본 정렬은 `departureTime desc, createdAt desc`다. 검색 범위는 출발지/도착지/leader uid/leader nickname이다.
- 관리자 상세는 목록 필드 외에 `leader`, `members`, `pendingJoinRequestCount`, `settlementStatus`, `settlement`, `chatRoomId`, `createdAt/updatedAt/endedAt`를 제공한다. 현재 도메인에 없는 `gender`, `lastStatusChangedAt`는 억지로 만들지 않는다.
- 관리자 상태 변경 액션은 현재 상태 머신을 재사용하는 `CLOSE`, `REOPEN`, `CANCEL`, `END`만 허용한다. 임의 상태 점프는 허용하지 않는다.
- 관리자 멤버 제거는 일반 멤버만 허용하고 leader 제거는 `PARTY_LEADER_REMOVAL_NOT_ALLOWED`로 막는다. `ARRIVED`, `ENDED` 상태에서는 멤버 제거를 허용하지 않는다. 부수효과는 기존 `removeMember` 로직(채팅방 membership sync, leave 시스템 메시지, SSE `KICKED`, `PartyMemberKicked` notification event)을 재사용한다.
- 관리자 시스템 메시지는 party chat room이 있을 때만 생성한다. 내부적으로 `SYSTEM` + `ADMIN_SYSTEM` source를 사용해 leader/member 사칭을 피하고, 응답/표시 기준 `senderName`은 `관리자`, `senderPhotoUrl`은 `null`이다.
- 관리자 join request 조회는 현재 `PENDING`만 `requestedAt(createdAt) desc` 최신순으로 반환한다. 승인/거절 액션은 아직 제공하지 않는다.
- 관리자 write audit(status 변경, 멤버 제거, 시스템 메시지)는 `admin_audit_logs`에 최소 snapshot만 남긴다. 파티 상태 변경은 `id/status/endReason/settlementStatus/endedAt`, 멤버 제거는 `partyId/memberId/isLeader/joinedAt`, 시스템 메시지는 `id/chatRoomId/senderId/senderName/type/source/text/createdAt` 기준으로 기록한다.

## Admin Board API 메모
- `board` 도메인은 관리자 백오피스용 moderation API를 제공한다: `GET /v1/admin/posts`, `GET /v1/admin/posts/{postId}`, `PATCH /v1/admin/posts/{postId}/moderation`, `GET /v1/admin/comments`, `PATCH /v1/admin/comments/{commentId}/moderation`.
- 관리자 목록은 게시글/댓글 모두 `page/size`와 검색 필터를 지원하고 기본 정렬은 `createdAt desc`다. 게시글 필터는 `query/category/moderationStatus/authorId`, 댓글 필터는 `postId/query/moderationStatus/authorId`를 사용한다.
- moderation 상태는 `VISIBLE`, `HIDDEN`, `DELETED`만 지원한다. `HIDDEN`은 `isHidden=true`, `DELETED`는 기존 soft delete를 재사용한다.
- 허용 전이는 게시글/댓글 모두 `VISIBLE <-> HIDDEN`, `VISIBLE/HIDDEN -> DELETED`이고, `DELETED`는 복구하지 않는다. pin/고정 정책과 신고 연계 뷰는 후속 범위다.
- public board는 관리자 moderation을 반영한다. `HIDDEN` 게시글은 public 목록/상세/내 게시글/북마크에서 제외되고, `HIDDEN` 댓글은 thread 구조 유지를 위해 placeholder로 마스킹된다.
- 관리자 write audit은 `admin_audit_logs`에 최소 snapshot만 남긴다. 게시글은 `id/authorId/category/anonymous/hidden/deleted`, 댓글은 `id/postId/authorId/parentId/anonymous/hidden/deleted` 기준으로 기록한다.


## Admin Dashboard API 메모
- 관리자 대시보드 read-model API는 `GET /v1/admin/dashboard/summary`, `GET /v1/admin/dashboard/activity`, `GET /v1/admin/dashboard/recent-items`를 제공한다.
- 모든 집계와 일자 버킷 기준은 `Asia/Seoul`이다. `summary.newMembersToday`는 `members.joinedAt` 기준 오늘 `00:00 ~ generatedAt`, `activity.days`는 `7 | 30`만 허용한다.
- `summary.totalMembers`는 `members` 전체 row 기준이다. soft delete tombstone(`WITHDRAWN`)도 포함하며, ACTIVE-only count는 현재 계약에 포함하지 않는다.
- `recent-items` source는 Inquiry/Report/AppNotice/Party만 사용하고, AppNotice는 `publishedAt <= now`인 게시 공지만 포함한다. 학교 공지 sync 이력이나 운영 action API는 이 read model 범위에 포함하지 않는다.
