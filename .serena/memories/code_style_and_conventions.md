# 코드 스타일 & 컨벤션

## 아키텍처
- Controller는 요청 검증과 응답 변환만 담당한다.
- Service가 비즈니스 규칙, 상태 전이, 권한 판단을 담당한다.
- 부가효과(알림, SSE fan-out, push)는 핵심 트랜잭션과 분리한다.
- SSE subscribe 메서드는 long-lived request 수명을 트랜잭션/JPA 세션 수명과 섞지 않는다. 초기 snapshot은 전용 read-only 서비스에서 DTO payload로 계산한 뒤 emitter를 생성/등록/전송한다.
- 상태 변경 후 알림 발행은 `AfterCommitApplicationEventPublisher`로 after-commit semantics를 보장한다.
- 회원 탈퇴는 hard delete 대신 tombstone(`status=WITHDRAWN`, `withdrawnAt`) + 개인정보 스크럽을 기본으로 한다.
- 탈퇴로 인한 외부 후처리(Firebase 삭제, SSE 연결 종료)는 핵심 트랜잭션 안에서 직접 처리하지 않고 after-commit 리스너로 분리한다.
- 같은 Firebase UID의 탈퇴 회원은 재활성화하지 않는다. `POST /v1/members`는 활성 회원에만 멱등이고, withdrawn UID에는 `WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 반환한다.
- `NOT NULL` lifecycle 컬럼을 운영 DB에 추가할 때는 앱 기동 전에 수동 마이그레이션으로 legacy row를 먼저 채운다.
- Admin controller는 class-level `@AdminApiAccess`를 사용하고 raw `@PreAuthorize("hasRole('ADMIN')")`를 반복하지 않는다.
- Campus 배너는 `app-notices`를 재사용하지 않고 별도 `domain/campus`로 관리한다. `displayOrder`는 생성/삭제/재정렬 후에도 1부터 시작하는 연속값을 유지하며, 순서 변경 계열 작업(create/delete/reorder)은 빈 테이블 첫 생성 경쟁까지 막기 위해 공통 lock으로 직렬화한다. PATCH는 누락 필드와 명시적 `null`을 구분한다.
- 상태 변경 Admin API는 `@AdminAudit`로 감사 대상을 선언하고, 감사 로직은 interceptor/filter 공통 계층에서 처리한다.
- `@AdminAudit`의 `targetId`와 snapshot lookup은 서비스가 쓰는 canonical 키(`semester=2026-1`, `platform=ios`) 기준으로 맞추고, request body 기반 값은 공통 request-body cache를 통해 preHandle 단계에서 복원한다.
- 감사 로그 실패는 warn 수준으로 남기고 비즈니스 API를 500으로 깨지 않게 best-effort로 처리한다.
- Support Admin 목록은 `AdminPageRequestPolicy` 기준 `page=0`, `size=20`, `size<=100`, 정렬 `createdAt,DESC` 규약을 유지한다.
- 이미지 업로드 context는 도메인별로 분리하고, Campus 배너 이미지는 `CAMPUS_BANNER_IMAGE` 관리자 전용 context + `campus-banners/YYYY/MM/DD` 경로를 사용한다.
- 일반 Chat 읽음 처리 외부 계약은 ISO 8601 UTC(`Instant`)로 유지하고, 내부 `ChatRoomMember.lastReadAt` 비교/저장은 `Asia/Seoul` 기준 `LocalDateTime`으로 정규화한다. unread의 source of truth는 서버 저장값이다.

## 응답/예외
- 모든 REST 응답은 `ApiResponse<T>`를 사용한다.
- OpenAPI 2xx 성공 응답은 Scalar `Show schema`에서 `data`의 concrete type이 보이도록 raw `ApiResponse.class`만 두지 않는다. 필요하면 도메인별 OpenAPI 전용 wrapper schema를 사용한다.
- 예외는 `BusinessException + ErrorCode`로 표현하고 `GlobalExceptionHandler`에서 일관 처리한다.
- Admin 403 판별은 `ApiAccessDeniedErrorResolver`로 공통화하고 `/v1/admin/**`는 `ADMIN_REQUIRED`를 반환한다.
- Notification 전용 최소 ErrorCode는 `NOTIFICATION_NOT_FOUND`, `NOT_NOTIFICATION_OWNER`를 사용한다.

## 운영/환경변수
- 프로필 파일은 정책, `.env`/Secrets는 실제 값을 담당한다.
- `application.yaml`은 공통 설정과 datasource 공통 인증정보(username/password/driver-class), `application-local.yaml`/`application-local-emulator.yaml`/`application-prod.yaml`은 프로필별 `datasource.url`과 인증 정책을 담당한다.
- 공통 JPA 정책은 `spring.jpa.open-in-view=false`이며, 커넥션 진단 기본값은 `spring.datasource.hikari.connection-timeout=30000`, `spring.datasource.hikari.leak-detection-threshold=20000`이다. 환경별 override는 `DB_CONNECTION_TIMEOUT_MS`, `DB_LEAK_DETECTION_THRESHOLD_MS`를 사용한다.
- `application-local.yaml`, `application-local-emulator.yaml`은 민감값 없이 정책만 담고 Git으로 추적한다.
- 로컬 기본 프로필은 `local`, Firebase Emulator 검증은 `local-emulator`, 운영은 `prod`, 자동 테스트는 `test`를 사용한다.
- `local`은 실제 Firebase 기반 통합 테스트용이므로 `FIREBASE_PROJECT_ID`와 서비스 계정 파일 경로를 환경변수로 받아야 한다.
- `local-emulator`는 Firebase Auth Emulator 기반 백엔드 단독 테스트용이며 `FIREBASE_AUTH_EMULATOR_HOST`, `FIREBASE_PROJECT_ID`만 주로 사용하고 `FIREBASE_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`는 비워 두는 것을 기본으로 한다.
- IntelliJ 실행 설정의 환경 변수 칸에는 `.env` 파일 경로를 넣지 않고 `KEY=value` 형식만 입력한다. `local-emulator`에서 prod용 Firebase 경로가 섞이면 환경 가드가 즉시 실패하도록 유지한다.
- 두 로컬 프로필의 기본 DB는 `localhost:3306`이며, Docker MySQL(`3307`) 같은 다른 포트를 쓰고 싶으면 `DB_URL`로 덮어쓴다.
- `local-emulator`는 기본적으로 로컬 DB 스키마를 재생성하지 않도록 유지하고, 초기화가 필요하면 별도 DB를 사용한다.
- 기본 `docker-compose.yml`은 Firebase 자격증명 파일을 자동 마운트하지 않으므로, Docker에서 실제 Firebase 인증까지 검증하려면 별도 volume mount 또는 호스트 `bootRun`이 필요하다.
- Firebase 서비스 계정 JSON은 서버 파일로 보관하고 `GOOGLE_APPLICATION_CREDENTIALS` 경로만 주입한다.
- `prod`에서는 OpenAPI UI/JSON을 기본 비노출로 운영하고, health/info만 최소 공개한다.
- 운영 app는 `docker-compose.prod.yml`에서 `${APP_HOST_BIND:-127.0.0.1}:${APP_HOST_PORT:-8080}:8080` loopback 바인딩을 유지하고, Nginx만 `127.0.0.1:8080` 으로 프록시하도록 운영한다.
- 운영 MySQL에 관리자 도구로 접근해야 하면 `docker-compose.prod.yml`에서 `127.0.0.1:3307:3306` 같은 loopback 바인딩만 허용하고 SSH 터널을 통해 접속한다. 공용 바인딩은 사용하지 않는다.
- 브라우저 기반 관리자 페이지의 REST API CORS 허용 Origin은 `API_ALLOWED_ORIGIN_PATTERNS`로, WebSocket `/ws` Origin은 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS`로 분리 관리한다.
- CD의 admin REST CORS smoke check는 `CD_SMOKE_CORS_ORIGIN`을 우선 사용하고, 비어 있으면 `API_ALLOWED_ORIGIN_PATTERNS`의 첫 번째 exact origin을 재사용한다.
- CD workflow는 `production-deploy` concurrency 그룹으로 최신 run만 유지하고 이전 run은 자동 취소한다.
