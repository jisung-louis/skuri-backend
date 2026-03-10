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
- `domain/chat/websocket/ChatWebSocketSessionRegistry.java`, `ChatSubscriptionAccessInterceptor.java`: 탈퇴 회원 WebSocket 세션 추적/차단
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

## 테스트 포인트
- `src/test/java/com/skuri/skuri_backend/domain/image/controller/ImageControllerContractTest.java`: `/v1/images`의 200/400/401/403/415 contract 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/service/ImageUploadServiceTest.java`: context 권한, 경로 naming, MIME/size validation, 원본/썸네일 저장 검증
- `src/test/java/com/skuri/skuri_backend/domain/image/integration/ImageUploadBoardFlowIntegrationTest.java`: 실제 업로드 후 Board 저장 플로우와 공개 업로드 경로 조회 검증
- `src/test/java/com/skuri/skuri_backend/infra/auth/AdminApiGuardIntegrationTest.java`: 대표 Admin API의 401/403/관리자 성공 guard 검증
- `src/test/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditIntegrationTest.java`: 상태 변경 Admin API의 감사 로그 row/snapshot 검증
- `src/test/java/com/skuri/skuri_backend/infra/openapi/AdminOpenApiConventionTest.java`: `@AdminApiAccess`와 `ERROR_ADMIN_REQUIRED` 예시 재사용 강제

## 프로필 역할
- `application.yaml`: 모든 환경이 공유하는 기본 정책과 공통 datasource 인증정보
- `application-local.yaml`: `localhost:3306` 기반 로컬 통합 테스트 + 실제 Firebase ID Token 흐름 검증용
- `application-local-emulator.yaml`: `localhost:3306` 기반 Firebase Auth Emulator 백엔드 단독 테스트용
- `application-prod.yaml`: compose 내부 `mysql:3306`을 사용하는 OCI 운영 서버용
- `application-test.yaml`: 자동 테스트 전용
