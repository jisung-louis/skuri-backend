# 코드베이스 구조 상세

## 핵심 엔트리포인트
- Phase 10 정책 문서: `docs/member-withdrawal-policy.md`
- 루트 인프라 파일: `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`, `.env.example`
- 프로필 파일: `src/main/resources/application.yaml`, `application-local.yaml`, `application-local-emulator.yaml`, `application-prod.yaml`, `src/test/resources/application-test.yaml`
- 배포 문서: `docs/deployment-guide.md`
- GitHub Actions: `.github/workflows/ci.yml`, `.github/workflows/cd.yml`
- `SkuriBackendApplication.java`

## 도메인 구조 메모
- `domain/member/constant/DepartmentCatalog.java`: 공개방 seed와 프로필 입력에서 맞춰야 하는 성결대학교 학과 canonical 목록과 legacy alias 정규화 기준
- `domain/member/entity/Member.java`: `status`, `withdrawnAt`를 포함한 회원 lifecycle 원장
- `domain/member/service/MemberLifecycleService.java`: 회원 탈퇴 오케스트레이션과 도메인 후처리 진입점
- `domain/member/service/MemberLifecycleEventListener.java`: after-commit 기반 Firebase 삭제/SSE 종료 처리
- `domain/chat/websocket/ChatWebSocketConfig.java`: STOMP endpoint를 `/ws`(SockJS)와 `/ws-native`(native WebSocket)로 분리 등록
- `domain/chat/service/PublicChatRoomSeedMigration.java`: 앱 기동 시 공식 공개방(학교 전체/마인크래프트/학과방)을 MySQL `INSERT IGNORE` 기반으로 idempotent + multi-instance safe seed
- `domain/chat/service/ChatService.java`, `ChatMessageOrderGenerator.java`, `domain/chat/controller/ChatRoomController.java`, `domain/chat/dto/request/UpdateChatRoomReadRequest.java`: 공개방 visibility(joined/not joined summary, 학과방 노출 제한), 공개방 create/join/leave, active member 검증, 메시지/summary 실시간 fan-out, 학과방 membership 정리, 서버 저장 시 `messageOrder`를 부여하고 `getMessages`의 `createdAt DESC + messageOrder` 커서 tie-breaker 해석을 담당한다. `PATCH /v1/chat-rooms/{id}/read`는 메시지 `createdAt`처럼 timezone 없는 `LocalDateTime` 문자열과 ISO 8601 `Z`/offset 문자열을 모두 받고, timezone 없는 입력은 `Asia/Seoul` 기준으로 `Instant`로 변환해 서비스로 전달한다.
- `domain/chat/websocket/ChatWebSocketSessionRegistry.java`, `ChatSubscriptionAccessInterceptor.java`: 탈퇴 회원 WebSocket 세션 추적/차단
- `domain/chat/service/PartyMessageService.java`, `ChatService.java`: 파티 채팅 특수 payload와 서버 생성 메시지 텍스트 정책(ACCOUNT snapshot, ARRIVED/END)을 담당한다. ARRIVED 메시지는 `memberSettlements.displayName/leftParty/leftAt` snapshot까지 포함하고, ARRIVED 이후 멤버 leave 시 `ChatService.syncPartyArrivalMessageSnapshot(...)`으로 저장된 ARRIVED payload를 최신 settlement snapshot으로 갱신한다. join/close/reopen/member leave SYSTEM 메시지 생성 시점과 자동 CLOSED 시 `합류 안내 -> 모집 마감 안내` 순서는 `domain/taxiparty/service/TaxiPartyService.java`가 오케스트레이션한다.
- `domain/taxiparty/entity/SettlementAccountSnapshot.java`, `SettlementTargetSnapshot.java`, `MemberSettlement.java`, `domain/taxiparty/dto/request/ArrivePartyRequest.java`: ARRIVED 정산 snapshot(account/taxiFare/settlementTargetMemberIds + memberSettlements displayName/leftParty/leftAt) 계약과 ARRIVED leave 시 멤버십 제거/정산 snapshot 유지 규칙을 정의한다.
- `domain/taxiparty/controller/PartyController.java`, `domain/taxiparty/service/TaxiPartyService.java`, `domain/taxiparty/dto/response/TaxiHistory*`: `/v1/members/me/taxi-history`, `/v1/members/me/taxi-history/summary`를 담당한다. history는 `ARRIVED/ENDED`만 포함하고 `dateTime=departureTime`, `role=leader 여부`, `paymentAmount=perPersonAmount`, summary는 `totalRideCount=전체 history 개수`, `completedRideCount=COMPLETED 개수`, `savedFareAmount=Σ(taxiFare-perPersonAmount)` 규칙을 서버가 계산한다.
- `domain/board/controller/PostController.java`, `domain/board/controller/CommentController.java`, `domain/board/service/BoardService.java`, `domain/board/repository/PostImageRepository.java`, `domain/board/repository/CommentLikeRepository.java`: 게시글 summary의 `isLiked`/`isBookmarked`/`isCommentedByMe` 개인화 플래그를 현재 페이지 `postIds` 기준 batch query로 합성하고, 첫 이미지의 `thumbUrl -> url` fallback 규칙으로 `thumbnailUrl`을 계산한다. `comment_likes` 저장 모델과 `comments.likeCount`를 함께 관리해 댓글 목록/생성/수정 응답의 `likeCount`/`isLiked`, `POST/DELETE /v1/comments/{commentId}/like`, 회원 탈퇴 시 댓글 좋아요 카운트 보정을 담당한다. `bookmarkCount`와 `PATCH /v1/posts/{postId}`의 `isAnonymous` 수정 + `images` 전체 교체 계약을 관리
- `domain/notice/controller/NoticeController.java`, `MemberNoticeController.java`, `NoticeCommentController.java`, `domain/notice/service/NoticeService.java`, `domain/notice/repository/NoticeCommentLikeRepository.java`, `NoticeThumbnailExtractor.java`: 학교 공지 조회/읽음/좋아요/북마크/댓글 API와 목록 summary의 `isLiked`/`isBookmarked`/`isCommentedByMe` 개인화 플래그 batch 합성, `bodyHtml` 첫 `<img src>` 기반 `thumbnailUrl` 추출, `PATCH /v1/notice-comments/{commentId}` content 수정 계약, `notice_comment_likes` 저장 모델과 `notice_comments.likeCount`, 댓글 `likeCount`/`isLiked` 응답, `POST/DELETE /v1/notice-comments/{commentId}/like`, `notice_bookmarks` 저장 모델, withdrawal cleanup 규칙을 관리
- `domain/app/controller/AppNoticeController.java`, `MemberAppNoticeController.java`, `domain/app/service/AppNoticeService.java`, `domain/app/entity/AppNoticeReadStatus*.java`: 공개 앱 공지 조회와 별도로 회원별 앱 공지 읽음 상태 persistence(`app_notice_read_status`), 미읽음 수 조회, idempotent 읽음 처리, 앱 공지 삭제/회원 탈퇴 시 read-status 정리를 담당한다.
- `domain/notification/service/NotificationService.java`, `NotificationSseSnapshotService.java`, `domain/notification/repository/UserNotificationRepository.java`: 일반 알림 unread 집계(`/v1/notifications/unread-count`, 목록 `unreadCount`, SSE `SNAPSHOT`/`UNREAD_COUNT_CHANGED`)는 `APP_NOTICE`를 제외한 인박스 알림만 대상으로 계산한다. `APP_NOTICE` 인박스 생성 자체와 목록 content는 유지된다.
- `domain/campus/controller/CampusBannerController.java`, `domain/campus/controller/CampusBannerAdminController.java`, `domain/campus/service/CampusBannerService.java`, `domain/campus/service/CampusBannerOrderLock.java`: `campus_banners` 공개/관리자 API, 노출 기간 필터, `displayOrder` normalize, 빈 테이블 첫 생성 경쟁을 막는 순서 작업 직렬화 lock, `actionType` 정합성(`IN_APP`/`EXTERNAL_URL`)을 관리
- `domain/*/controller/*AdminController.java`: Phase 11 기준 공통 `@AdminApiAccess`를 사용하고, 상태 변경 엔드포인트는 `@AdminAudit`로 감사 로그 대상 지정

## Admin 공통 인프라
- `infra/auth/config/AdminApiAccess.java`: Admin controller class-level 메타 어노테이션 (`@PreAuthorize("hasRole('ADMIN')")`)
- `infra/auth/config/AdminRequestPaths.java`: `/v1/admin/**` 경로 판별 유틸리티
- `infra/auth/config/ApiAccessDeniedErrorResolver.java`: `ADMIN_REQUIRED`/기타 403 errorCode 공통 해석
- `infra/admin/list/AdminPageRequestPolicy.java`: Support Admin 목록의 page/size validation 및 공통 기본값/정렬 규약 상수
- `infra/admin/audit/AdminAudit.java`: 감사 로그 대상 메서드 선언 어노테이션
- `infra/admin/audit/AdminAuditHandlerInterceptor.java`, `AdminAuditRequestBodyAdvice.java`, `AdminAuditFilter.java`: 요청/응답 본문과 before/after snapshot을 수집하는 공통 감사 계층
- `infra/admin/audit/AdminAuditLog.java`, `AdminAuditLogRepository.java`, `AdminAuditLogService.java`: `admin_audit_logs` 저장 모델
- `infra/admin/audit/AdminAuditSnapshotFactory.java`: Academic/Chat/App/Campus/Support 도메인 snapshot 생성기


## 인증 인프라
- `infra/auth/firebase/FirebaseAuthenticationFilter.java`: Bearer 토큰을 `AuthenticatedMember`로 변환하고, SSE 스트림의 async 재디스패치에서도 다시 실행되어 Spring Security `AuthorizationFilter`가 동일한 인증 컨텍스트를 보도록 유지한다.

## SSE 운영
- `domain/taxiparty/service/PartySseSnapshotService.java`, `domain/taxiparty/service/JoinRequestSseSnapshotService.java`, `domain/notification/service/NotificationSseSnapshotService.java`: subscribe 시점의 초기 snapshot을 짧은 read-only 트랜잭션에서 DTO payload로 계산한다.
- `domain/notification/service/NotificationSseService.java`, `domain/taxiparty/service/PartySseService.java`, `domain/taxiparty/service/JoinRequestSseService.java`: `SseEmitter` 생성/등록/전송을 snapshot 계산과 분리하고 subscribe/complete/timeout/error + subscriber count 로그를 남긴다.
- `infra/auth/firebase/FirebaseAuthenticationFilter.java`: SSE async 재디스패치에서 request attribute에 캐시한 Authentication을 재사용하고, cache hit/miss를 debug 로그로 남긴다.
- `infra/auth/config/SseDisconnectRequestSupport.java`, `SecurityConfig.java`: disconnected client 예외가 붙은 SSE ERROR dispatch만 security matcher로 permit 처리해 committed-response 상태의 `AccessDenied`/`/error` 재디스패치 로그 노이즈를 줄인다. `/error` 전역 permitAll은 하지 않는다.
- `common/exception/GlobalExceptionHandler.java`: `AsyncRequestNotUsableException`을 `204 No Content`로 별도 처리해 async SSE 재디스패치가 원래 subscribe 경로로 재진입하지 않게 하고, 종료 직후 `ApiResponse` JSON을 쓰려는 2차 실패도 막는다.

## 테스트 포인트
- `src/test/java/com/skuri/skuri_backend/domain/campus/controller/CampusBannerControllerContractTest.java`, `CampusBannerAdminControllerContractTest.java`: 캠퍼스 배너 공개/관리자 contract와 admin guard 회귀 검증
- `src/test/java/com/skuri/skuri_backend/domain/campus/service/CampusBannerServiceTest.java`, `CampusBannerRepositoryDataJpaTest.java`: action 규칙, 순서 normalize, 공개 노출 쿼리 정렬/필터 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/controller/ImageControllerContractTest.java`: `/v1/images`의 200/400/401/403/415/422 contract 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/service/ImageUploadServiceTest.java`: context 권한, 경로 naming, MIME/size/dimension validation, 원본/썸네일 저장 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/integration/ImageUploadBoardFlowIntegrationTest.java`: 실제 업로드 후 Board 저장 플로우와 공개 업로드 경로 조회 검증
- `src/test/java/com/skuri/skuri_backend/infra/storage/FirebaseStorageRepositoryTest.java`: Firebase bucket 업로드 metadata(download token)/download URL/delete 동작 검증
- `src/test/java/com/skuri/skuri_backend/infra/auth/AdminApiGuardIntegrationTest.java`: 대표 Admin API의 401/403/관리자 성공 guard 검증
- `src/test/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditIntegrationTest.java`: 상태 변경 Admin API의 감사 로그 row/snapshot 검증
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApi*Schemas.java`: 각 도메인의 성공 응답 `Show schema`가 concrete `data` 타입을 노출하도록 하는 OpenAPI 전용 wrapper schema 모음\n- `src/test/java/com/skuri/skuri_backend/infra/openapi/AdminOpenApiConventionTest.java`: `@AdminApiAccess`와 `ERROR_ADMIN_REQUIRED` 예시 재사용 강제\n- `src/test/java/com/skuri/skuri_backend/infra/openapi/OpenApiSuccessSchemaCoverageIntegrationTest.java`: `/v3/api-docs` 전체 success response를 스캔해 대상 API가 다시 generic `data`로 후퇴하지 않는지 검증\n
## 프로필 역할
- `application.yaml`: 모든 환경이 공유하는 기본 정책과 공통 datasource 인증정보
- `application-local.yaml`: `localhost:3306` 기반 로컬 통합 테스트 + 실제 Firebase ID Token 흐름 검증용
- `application-local-emulator.yaml`: `localhost:3306` 기반 Firebase Auth Emulator 백엔드 단독 테스트용
- `application-prod.yaml`: compose 내부 `mysql:3306`을 사용하는 OCI 운영 서버용
- `application-test.yaml`: 자동 테스트 전용
