# SKURI Backend - 프로젝트 개요

## 목적
성결대학교 학생 대상 택시 합승/커뮤니티/공지/학사 서비스 백엔드다. 핵심 도메인은 TaxiParty이며, 알림은 핵심 트랜잭션의 부가효과로 분리한다.

## 기술 스택
- Java 21, Spring Boot 4.0.3, Gradle
- MySQL 8.4 (prod), H2 (test)
- Spring Data JPA + Hibernate
- Firebase Admin SDK (ID Token 검증, FCM sender 구현)
- 실시간: WebSocket(STOMP, 채팅), SSE(파티/알림)
- OpenAPI: springdoc Swagger UI + Scalar

## 도메인 구조
- member: 회원, 프로필, 알림 설정, FCM 토큰 API, 계정 라이프사이클(soft delete tombstone, 탈퇴, 재가입 정책)
- taxiparty: 생성/참여/정산/상태 전이, SSE, 정산 snapshot(taxiFare/account/settlementTargetMemberIds/memberSettlements.displayName,leftParty,leftAt) 관리. ARRIVED 이후 일반 멤버 leave는 `party_members`만 제거하고 `member_settlements` snapshot은 유지하며, `confirmSettlement`와 chat ARRIVED payload도 이 snapshot을 기준으로 계속 동작한다. `/v1/members/me/taxi-history` + `/summary`로 MyScreen/TaxiHistoryScreen 전용 history 계약 제공 (dateTime=departureTime, paymentAmount=perPersonAmount, status는 status/endReason+정산 snapshot 기준 COMPLETED/CANCELLED로 매핑, summary는 totalRideCount/ completedRideCount/ savedFareAmount를 함께 제공)
- chat: 채팅방/메시지/WebSocket, 파티 도메인 이벤트 기반 서버 생성 메시지(SYSTEM/ARRIVED/END), 공식 공개방 seed(학교 전체/마인크래프트/학과방)와 공개방 join/leave/create 및 학과 변경 시 학과방 membership 정리 정책 관리. 공개방 create/join은 가입 완료된 active member만 허용한다. 일반 Chat 읽음 처리 API는 timezone 없는 `LocalDateTime` 문자열과 ISO 8601 `Z`/offset `lastReadAt`를 모두 받아 timezone 없는 입력은 `Asia/Seoul` 기준으로 해석한 뒤 `LocalDateTime`으로 정규화해 저장/비교하고, `PATCH /read` 및 detail 응답은 UTC 문자열로 반환한다. 파티 채팅의 join accept/member leave SYSTEM 메시지는 서버가 저장 후 STOMP(`/topic/chat/party:{partyId}`)로 브로드캐스트하며, join accept로 자동 CLOSED 되면 `합류 안내 -> 모집 마감 안내` 순서로 두 SYSTEM 메시지를 연속 생성한다. 채팅 history는 `createdAt DESC`를 유지하되, 서버가 저장 시 부여하는 `chat_messages.messageOrder` tie-breaker로 같은 timestamp에서도 저장 순서를 결정적으로 유지한다. party chat `CHAT_MESSAGE` 알림은 `TEXT`/`IMAGE`뿐 아니라 `ACCOUNT`/`SYSTEM`/`ARRIVED`/`END`도 포함하고, push payload의 canonical 식별자는 `chatRoomId`다.
- board: 게시글/댓글/북마크, 목록 summary에 `isLiked`/`isBookmarked`/`isCommentedByMe` 개인화 플래그를 배치 합성하고 `bookmarkCount`, `thumbnailUrl`을 함께 제공한다. `thumbnailUrl`은 첫 번째 게시글 이미지의 `thumbUrl` 우선, 원본 `url` fallback 규칙을 사용한다. `PATCH /v1/posts/{id}`의 `isAnonymous` 수정 및 `images` 전체 교체 계약을 지원한다.
- notice: 학교 공지 수집/상세/댓글/읽음/북마크, 목록 summary에 `isLiked`/`isBookmarked`/`isCommentedByMe` 개인화 플래그와 `thumbnailUrl`을 배치 합성한다. `thumbnailUrl`은 `bodyHtml`의 첫 번째 `<img src>`를 추출해 제공한다. `GET /v1/members/me/notice-bookmarks` + `POST/DELETE /v1/notices/{id}/bookmark` 지원, `PATCH /v1/notice-comments/{id}` content 수정 지원(익명 여부는 생성 시점 값 유지).
- academic: 강의/시간표/학사 일정
- app: 앱 공지, 회원별 앱 공지 읽음 상태(`app_notice_read_status`)와 `/v1/members/me/app-notices/unread-count`, `POST /v1/members/me/app-notices/{appNoticeId}/read`를 제공한다. 이 unread count는 일반 알림 `/v1/notifications/unread-count`와 별도 source of truth다.
- campus: 캠퍼스 홈 배너 공개/관리자 API, 노출 기간/정렬 normalize, actionType별 이동 규칙, `CAMPUS_BANNER_IMAGE` 업로드 컨텍스트 관리
- support: 문의/신고/버전/학식
- notification: 인앱 인박스, FCM 토큰, SSE, 리마인더 스케줄링, 이벤트 기반 알림 처리

## 인프라/공통
- Phase 9 기준 실행/배포는 `application`, `local`, `local-emulator`, `prod`, `test` 5개 정책 파일 + `.env` 중심으로 정리되었다.
- 로컬 기본 개발은 `local` 또는 `local-emulator` 프로필로 호스트 앱(`bootRun`/IDE)을 실행하고, 필요하면 `docker compose up -d mysql redis`로 MySQL/Redis만 올린다.
- `local`은 프론트와 함께 실제 Firebase ID Token 흐름을 검증하는 통합 테스트용이며, `FIREBASE_PROJECT_ID`와 서비스 계정 파일 경로가 필요하다.
- `local-emulator`는 Firebase Auth Emulator 기반 백엔드 단독 테스트용이며, `FIREBASE_AUTH_EMULATOR_HOST`, `FIREBASE_PROJECT_ID`만 주로 사용하고 자격증명 파일 경로는 비워 두는 것을 기본으로 한다.
- `local`과 `local-emulator`는 각 프로필 파일에서 기본 DB를 `localhost:3306`으로 둔다.
- 운영은 `prod` 프로필 + `OCI 단일 인스턴스에서 docker-compose.prod.yml(app + MySQL + Redis)` 구조를 사용하며, app 컨테이너는 compose 내부 주소 `mysql:3306`으로 MySQL에 접속한다.
- OpenAPI는 `local/local-emulator`에서 노출하고 `prod`에서는 기본 비노출로 운영한다.
- SSE subscribe는 전용 read-only snapshot 서비스에서 DTO payload를 먼저 계산한 뒤 `SseEmitter`를 생성/등록한다. `spring.jpa.open-in-view=false`를 공통으로 강제해 long-lived SSE 요청과 JDBC connection 수명을 분리한다.
- 끊긴 SSE 연결의 ERROR dispatch는 `/error`를 전역 허용하지 않고, 원래 요청이 `/v1/sse/**`이며 disconnected client 계열 예외가 확인된 경우에만 security authorize 단계에서 좁게 우회한다. 일반 API와 SSE 시작 단계의 인증/인가 실패는 그대로 유지한다.
- 공통 Hikari 진단 정책은 `connection-timeout=30s`, `leak-detection-threshold=20s` 기본값이며 `DB_CONNECTION_TIMEOUT_MS`, `DB_LEAK_DETECTION_THRESHOLD_MS`로 환경별 override 한다.
- GitHub Actions CD는 `production` 환경 승인 기반 반자동 배포를 사용하며, `linux/amd64`와 `linux/arm64` 멀티플랫폼 이미지를 빌드한다.
- CD workflow는 `concurrency.group = production-deploy`, `cancel-in-progress = true`로 최신 `main` push만 남기고 이전 run을 자동 취소한다.
- `common.event.AfterCommitApplicationEventPublisher`: 성공한 상태 변경 이후에만 `ApplicationEvent`를 발행한다.
- Phase 10부터 회원 탈퇴는 `members.status=WITHDRAWN`, `withdrawnAt` 기반 soft delete tombstone으로 처리하고, 개인정보 스크럽 + 도메인 후처리를 함께 수행한다.
- 탈퇴 회원은 보호 API에서 `403 MEMBER_WITHDRAWN`으로 차단되며, 동일 Firebase UID는 재활성화하지 않고 `POST /v1/members`에서 `409 WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 반환한다.
- Firebase user 삭제, SSE 연결 종료 같은 외부 후처리는 after-commit 리스너에서 best-effort로 수행한다.
- 채팅 WebSocket은 탈퇴 시 세션을 best-effort로 종료하고, 남은 `/topic/chat/**` delivery도 outbound interceptor로 차단한다.
- Phase 10 이전 운영 DB 업그레이드 시에는 앱 기동 전에 `members.status`를 수동 SQL로 `ACTIVE` 백필한 뒤 새 버전을 실행한다.
- `infra.auth.config.FirebaseConfig`를 재사용하고 Firebase Admin 중복 초기화는 금지한다.
- `infra.notification`에는 `PushSender` 추상화, Firebase 기반 sender, credentials 부재 시 `NoOpPushSender`가 있다.
- Phase 11부터 Admin 공통 인프라는 `infra/auth/config/AdminApiAccess`, `ApiAccessDeniedErrorResolver`, `AdminRequestPaths`, `infra/admin/audit`, `infra/admin/list`를 중심으로 정리됐다.
- 상태 변경 Admin API(`POST`, `PUT`, `PATCH`, `DELETE`)는 `admin_audit_logs`에 `actor_id`, `action`, `target_type`, `target_id`, `diff_before`, `diff_after`, `timestamp`를 저장하고, 조회 `GET` Admin API는 감사 로그 대상에서 제외한다.
- Phase 12부터 공통 이미지 업로드 인프라는 `domain/image` + `infra/storage`로 추가됐다. `/v1/images`는 multipart 업로드 후 `url`, `thumbUrl`, `width`, `height`, `size`, `mime`를 반환하고, 기본 storage provider는 LOCAL 파일시스템이며 `FIREBASE` provider도 선택 가능하다.
- 업로드 context는 `POST_IMAGE`, `CHAT_IMAGE`, `PROFILE_IMAGE`, `APP_NOTICE_IMAGE`, `CAMPUS_BANNER_IMAGE`를 사용하며, `APP_NOTICE_IMAGE`와 `CAMPUS_BANNER_IMAGE`는 관리자 전용이다. 업로드 결과 URL은 Board/Chat/AppNotice/Profile/CampusBanner의 기존 URL 입력 필드에 그대로 재사용한다.
- LOCAL provider는 `GET /uploads/**`(`media.storage.url-prefix`)로 공개 제공되고 잘못된 Bearer 헤더가 있어도 공개 조회를 막지 않는다. FIREBASE provider는 버킷 업로드 후 tokenized download URL을 반환한다. 이미지 업로드는 10MB, JPEG/PNG/WebP, 최대 5000x5000px 및 총 20,000,000 픽셀 제한을 둔다.
- Support Admin 목록 API(`GET /v1/admin/inquiries`, `GET /v1/admin/reports`)는 `PageResponse` + `page=0`/`size=20`/`size<=100` + 고정 정렬 `createdAt,DESC` 규약을 따른다.
- 공통 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`, ErrorCode 중심으로 처리한다.
