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
- 관리자 목록은 `query/status/isAdmin/department` 필터와 `joinedAt desc` 고정 정렬, `PageResponse` 규약을 사용한다.
- 관리자 상세는 목록 필드 외에 `photoUrl`, `withdrawnAt`, `bankAccount`, `notificationSetting`을 포함한다.
- 관리자 활동 요약은 ACTIVE 회원만 제공하며, 현재 저장된 post/comment/party/inquiry/report 데이터 기준 count + domain별 recent 5건 read model을 반환한다. 탈퇴 회원은 `409 MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN`으로 비제공 처리한다.
- 관리자 권한 변경은 기존 `members.is_admin` boolean만 갱신하며, 탈퇴 회원은 `409 CONFLICT`, 자기 자신의 권한 변경은 `400 SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED`로 차단한다. 마지막 관리자 수 계산 정책은 후속 결정 전까지 추가하지 않는다.
- 관리자 상세 응답은 `bankAccount`, `notificationSetting`을 유지하지만 admin-role 감사 snapshot은 `id/email/nickname/isAdmin/status` 최소 필드만 저장한다.
