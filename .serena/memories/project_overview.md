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
- member: 회원, 프로필, 알림 설정, FCM 토큰 API
- taxiparty: 생성/참여/정산/상태 전이, SSE
- chat: 채팅방/메시지/WebSocket
- board: 게시글/댓글/북마크
- notice: 학교 공지 수집/상세/댓글/읽음
- academic: 강의/시간표/학사 일정
- app: 앱 공지
- support: 문의/신고/버전/학식
- notification: 인앱 인박스, FCM 토큰, SSE, 리마인더 스케줄링, 이벤트 기반 알림 처리

## 인프라/공통
- Phase 9 기준 실행/배포는 `application`, `local`, `local-emulator`, `prod`, `test` 5개 정책 파일 + `.env` 중심으로 정리되었다.
- 로컬 기본 개발은 `local` 또는 `local-emulator` 프로필로 호스트 앱(`bootRun`/IDE)을 실행하고, 필요하면 `docker compose up -d mysql redis`로 MySQL/Redis만 올린다.
- `local`은 프론트와 함께 실제 Firebase ID Token 흐름을 검증하는 통합 테스트용이며, `FIREBASE_PROJECT_ID`와 서비스 계정 파일 경로가 필요하다.
- `local-emulator`는 Firebase Auth Emulator 기반 백엔드 단독 테스트용이며, `FIREBASE_AUTH_EMULATOR_HOST`, `FIREBASE_PROJECT_ID`만 주로 사용하고 자격증명 파일 경로는 비워 두는 것을 기본으로 한다.
- `local`과 `local-emulator`는 각 프로필 파일에서 기본 DB를 `localhost:3306`으로 둔다.
- 운영은 `prod` 프로필 + `OCI 단일 인스턴스에서 docker-compose.prod.yml(app + MySQL + Redis)` 구조를 사용하며, app 컨테이너는 호스트 포트가 아니라 compose 내부 주소 `mysql:3306`으로 MySQL에 접속한다.
- OpenAPI는 `local/local-emulator`에서 노출하고 `prod`에서는 기본 비노출로 운영한다.
- GitHub Actions CD는 `production` 환경 승인 기반 반자동 배포를 사용하며, `linux/amd64`와 `linux/arm64` 멀티플랫폼 이미지를 빌드한다.
- CD workflow는 `concurrency.group = production-deploy`, `cancel-in-progress = true`로 최신 `main` push만 남기고 이전 run을 자동 취소한다.
- `common.event.AfterCommitApplicationEventPublisher`: 성공한 상태 변경 이후에만 `ApplicationEvent`를 발행한다.
- `infra.auth.config.FirebaseConfig`를 재사용하고 Firebase Admin 중복 초기화는 금지한다.
- `infra.notification`에는 `PushSender` 추상화, Firebase 기반 sender, credentials 부재 시 `NoOpPushSender`가 있다.
- 공통 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`, ErrorCode 중심으로 처리한다.
