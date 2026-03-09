# 코드 스타일 & 컨벤션

## 아키텍처
- Controller는 요청 검증과 응답 변환만 담당한다.
- Service가 비즈니스 규칙, 상태 전이, 권한 판단을 담당한다.
- 부가효과(알림, SSE fan-out, push)는 핵심 트랜잭션과 분리한다.
- 상태 변경 후 알림 발행은 `AfterCommitApplicationEventPublisher`로 after-commit semantics를 보장한다.

## 응답/예외
- 모든 REST 응답은 `ApiResponse<T>`를 사용한다.
- 예외는 `BusinessException + ErrorCode`로 표현하고 `GlobalExceptionHandler`에서 일관 처리한다.
- Notification 전용 최소 ErrorCode는 `NOTIFICATION_NOT_FOUND`, `NOT_NOTIFICATION_OWNER`를 사용한다.

## 운영/환경변수
- 프로필 파일은 정책, `.env`/Secrets는 실제 값을 담당한다.
- `application.yaml`은 공통 설정과 datasource 공통 인증정보(username/password/driver-class), `application-local.yaml`/`application-local-emulator.yaml`/`application-prod.yaml`은 프로필별 `datasource.url`과 인증 정책을 담당한다.
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
- 브라우저 기반 관리자 페이지의 REST API CORS 허용 Origin은 `API_ALLOWED_ORIGIN_PATTERNS`로, WebSocket `/ws` Origin은 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS`로 분리 관리한다.
