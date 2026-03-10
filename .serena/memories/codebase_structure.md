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

## 프로필 역할
- `application.yaml`: 모든 환경이 공유하는 기본 정책과 공통 datasource 인증정보
- `application-local.yaml`: `localhost:3306` 기반 로컬 통합 테스트 + 실제 Firebase ID Token 흐름 검증용
- `application-local-emulator.yaml`: `localhost:3306` 기반 Firebase Auth Emulator 백엔드 단독 테스트용
- `application-prod.yaml`: compose 내부 `mysql:3306`을 사용하는 OCI 운영 서버용
- `application-test.yaml`: 자동 테스트 전용
