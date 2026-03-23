# 코드베이스 구조 상세

## 핵심 엔트리포인트
- Phase 10 정책 문서: `docs/member-withdrawal-policy.md`
- 루트 인프라 파일: `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`, `.env.example`
- 프로필 파일: `src/main/resources/application.yaml`, `application-local.yaml`, `application-local-emulator.yaml`, `application-prod.yaml`, `src/test/resources/application-test.yaml`
- 배포 문서: `docs/deployment-guide.md`
- GitHub Actions: `.github/workflows/ci.yml`, `.github/workflows/cd.yml`
- `SkuriBackendApplication.java`

## 도메인 구조 메모
- `domain/member/entity/Member.java`: `status`, `withdrawnAt`를 포함한 회원 lifecycle 원장
- `domain/member/service/MemberLifecycleService.java`: 회원 탈퇴 오케스트레이션과 도메인 후처리 진입점
- `domain/member/service/MemberLifecycleEventListener.java`: after-commit 기반 Firebase 삭제/SSE 종료 처리
- `domain/chat/websocket/ChatWebSocketConfig.java`: STOMP endpoint를 `/ws`(SockJS)와 `/ws-native`(native WebSocket)로 분리 등록
- `domain/chat/websocket/ChatWebSocketSessionRegistry.java`, `ChatSubscriptionAccessInterceptor.java`: 탈퇴 회원 WebSocket 세션 추적/차단
- `domain/chat/service/PartyMessageService.java`: 파티 채팅 특수 payload와 서버 생성 메시지 텍스트 정책(ACCOUNT snapshot, join/close/reopen/member leave SYSTEM, ARRIVED/END) 생성기
- `domain/taxiparty/entity/SettlementAccountSnapshot.java`, `domain/taxiparty/dto/request/ArrivePartyRequest.java`: ARRIVED 정산 snapshot(account/taxiFare/settlementTargetMemberIds) 계약 정의
- `domain/*/controller/*AdminController.java`: Phase 11 기준 공통 `@AdminApiAccess`를 사용하고, 상태 변경 엔드포인트는 `@AdminAudit`로 감사 로그 대상 지정

## Admin 공통 인프라
- `infra/auth/config/AdminApiAccess.java`: Admin controller class-level 메타 어노테이션 (`@PreAuthorize("hasRole('ADMIN')")`)
- `infra/auth/config/AdminRequestPaths.java`: `/v1/admin/**` 경로 판별 유틸리티
- `infra/auth/config/ApiAccessDeniedErrorResolver.java`: `ADMIN_REQUIRED`/기타 403 errorCode 공통 해석
- `infra/admin/list/AdminPageRequestPolicy.java`: Support Admin 목록의 page/size validation 및 공통 기본값/정렬 규약 상수
- `infra/admin/audit/AdminAudit.java`: 감사 로그 대상 메서드 선언 어노테이션
- `infra/admin/audit/AdminAuditHandlerInterceptor.java`, `AdminAuditRequestBodyAdvice.java`, `AdminAuditFilter.java`: 요청/응답 본문과 before/after snapshot을 수집하는 공통 감사 계층
- `infra/admin/audit/AdminAuditLog.java`, `AdminAuditLogRepository.java`, `AdminAuditLogService.java`: `admin_audit_logs` 저장 모델
- `infra/admin/audit/AdminAuditSnapshotFactory.java`: Academic/Chat/App/Support 도메인 snapshot 생성기


## 인증 인프라
- `infra/auth/firebase/FirebaseAuthenticationFilter.java`: Bearer 토큰을 `AuthenticatedMember`로 변환하고, SSE 스트림의 async 재디스패치에서도 다시 실행되어 Spring Security `AuthorizationFilter`가 동일한 인증 컨텍스트를 보도록 유지한다.

## SSE 운영
- `domain/notification/service/NotificationSseService.java`, `domain/taxiparty/service/PartySseService.java`: 하트비트/이벤트 전송 실패 시 subscriber만 제거하고 이미 깨진 `SseEmitter`에 `complete()`를 다시 호출하지 않는다.
- `common/exception/GlobalExceptionHandler.java`: `AsyncRequestNotUsableException`을 `204 No Content`로 별도 처리해 async SSE 재디스패치가 원래 subscribe 경로로 재진입하지 않게 하고, 종료 직후 `ApiResponse` JSON을 쓰려는 2차 실패도 막는다.

## 테스트 포인트
- `src/test/java/com/skuri/skuri_backend/domain/image/controller/ImageControllerContractTest.java`: `/v1/images`의 200/400/401/403/415/422 contract 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/service/ImageUploadServiceTest.java`: context 권한, 경로 naming, MIME/size/dimension validation, 원본/썸네일 저장 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/integration/ImageUploadBoardFlowIntegrationTest.java`: 실제 업로드 후 Board 저장 플로우와 공개 업로드 경로 조회 검증
- `src/test/java/com/skuri/skuri_backend/infra/storage/FirebaseStorageRepositoryTest.java`: Firebase bucket 업로드 metadata(download token)/download URL/delete 동작 검증
- `src/test/java/com/skuri/skuri_backend/infra/auth/AdminApiGuardIntegrationTest.java`: 대표 Admin API의 401/403/관리자 성공 guard 검증
- `src/test/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditIntegrationTest.java`: 상태 변경 Admin API의 감사 로그 row/snapshot 검증
- `src/test/java/com/skuri/skuri_backend/infra/openapi/AdminOpenApiConventionTest.java`: `@AdminApiAccess`와 `ERROR_ADMIN_REQUIRED` 예시 재사용 강제

## 프로필 역할
- `application.yaml`: 모든 환경이 공유하는 기본 정책과 공통 datasource 인증정보
- `application-local.yaml`: `localhost:3306` 기반 로컬 통합 테스트 + 실제 Firebase ID Token 흐름 검증용
- `application-local-emulator.yaml`: `localhost:3306` 기반 Firebase Auth Emulator 백엔드 단독 테스트용
- `application-prod.yaml`: compose 내부 `mysql:3306`을 사용하는 OCI 운영 서버용
- `application-test.yaml`: 자동 테스트 전용
