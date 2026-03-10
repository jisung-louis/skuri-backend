# SKURI Backend

성결대학교 학생 대상 서비스 **SKURI Taxi**의 Spring 백엔드 저장소입니다.

## 1. 서비스 소개

SKURI는 성결대학교 학생들이 더 편하게 이동하고 소통할 수 있도록 만든 캠퍼스 라이프 서비스입니다.

- 택시 동승 파티를 만들고 함께 이동
- 실시간 채팅으로 동승/커뮤니티 소통
- 학교 공지, 게시판, 학사/생활 정보 제공

상세 제품 배경: [project-overview.md](docs/project-overview.md)

## 2. 핵심 기능(제품 관점)

1. 택시 동승: 파티 생성/참여, 채팅, 정산
2. 학교 공지: 공지 열람, 필터, 댓글
3. 커뮤니티 게시판: 글/댓글, 좋아요, 북마크
4. 채팅: 공개 채팅방, 실시간 메시지
5. 학교 생활 정보: 시간표, 학식, 학사 일정
6. 운영 지원: 문의 접수, 신고 접수, 앱 버전 관리

대상 사용자: `@sungkyul.ac.kr` 계정을 가진 성결대학교 학생

## 3. 저장소 현재 상태(백엔드 관점)

현재 저장소는 **Phase 11(운영 공통 인프라 / Admin 공통)** 까지 구현이 완료된 상태입니다.

- 공통 계층
  - 공통 응답/페이지 DTO: `ApiResponse`, `PageResponse`
  - 공통 예외 처리: `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`
  - 공통 엔티티 기반: `BaseTimeEntity`, `JpaAuditingConfig`
- 인증/보안
  - Firebase ID Token 검증 기반 인증
  - `@sungkyul.ac.kr` 이메일 도메인 정책 적용
  - `members.isAdmin` 기반 `ROLE_ADMIN` authority 부여
  - Admin API 권한 실패 시 `403 ADMIN_REQUIRED` 표준 응답
  - `local-emulator` 프로필 지원 (Firebase Auth Emulator)
- Member 도메인
  - 회원 가입/프로필 조회/프로필 수정
- TaxiParty 도메인 (Phase 2)
  - 파티/동승요청/정산/상태머신/권한 규칙
  - Optimistic Lock(`@Version`) 기반 동시성 충돌 방어
  - 12시간 초과 파티 자동 종료 스케줄러
  - SSE: 파티 목록/상태, 동승요청(리더/요청자) 실시간 구독
- Chat 도메인 (Phase 3)
  - 채팅방/메시지/읽음 처리 API
  - STOMP + SockJS 기반 실시간 메시지 송수신
  - WebSocket 목적지별 인가(SEND/SUBSCRIBE 멤버십 검증)
  - 관리자 공개 채팅방 API: `POST/DELETE /v1/admin/chat-rooms`
- Board 도메인 (Phase 4)
  - 게시글/댓글 CRUD API
  - 익명 댓글 `anonId`/`anonymousOrder` 서버 계산
  - 좋아요/북마크 상호작용 + 카운트 동기화
  - Board/Notice 공통 댓글 정책 반영: 무제한 depth 저장, flat list 응답, 부모 삭제 placeholder soft delete
- Notice 도메인 (Phase 5)
  - 학교 공지 RSS 수집 + 상세 크롤링 + 관리자 수동 sync API
  - 공지 목록/상세/읽음/좋아요/댓글 API
  - `rssPreview` / `bodyHtml` / `bodyText` 저장 구조 + `summary`(향후 AI 요약 예약)
  - AppNotice Public 조회 + 관리자 CRUD API
- Academic 도메인 (Phase 6)
  - 강의 검색 API: `semester`, `department`, `professor`, `search`, `dayOfWeek`, `grade` 필터 지원
  - 내 시간표 조회/강의 추가/강의 삭제 API
  - 시간표 무결성 규칙: `user_id + semester` 1개 시간표, 동일 강의 중복 추가 차단, 시간 겹침 차단
  - 학사 일정 조회 API + 관리자 CRUD API
  - 관리자 학기 강의 bulk 업서트/전체 삭제 API
  - 시간표 응답 구조: `courses[] + slots[]` (`color` 제외, RN 렌더링 책임은 프론트엔드)
- Support 도메인 (Phase 7)
  - 문의 접수/내 문의 조회 API + 관리자 문의 목록/상태 처리 API
  - 신고 접수 API + 관리자 신고 목록/상태 처리 API
  - 앱 버전 공개 조회 API + 관리자 버전 업데이트 API
  - 앱 버전 저장 데이터가 없으면 기본 `minimumVersion=1.0.0`, `forceUpdate=false`, `showButton=false` 응답
  - 학식 메뉴 주간 조회 API + 관리자 등록/수정/삭제 API
  - Support Admin API는 `ROLE_ADMIN` + `403 ADMIN_REQUIRED`, 앱 버전 조회는 `permitAll`
- Notification 인프라 (Phase 8)
  - 인앱 알림 inbox, SSE, FCM 토큰 등록/해제 API
  - Firebase 자격증명 부재 시 `NoOpPushSender` fallback
  - after-commit 이벤트 기반 알림 발행
- Member 알림 설정
  - `commentNotifications`, `bookmarkedPostCommentNotifications` 등 현재 정책 반영
- 인프라 / 배포 (Phase 9)
  - 멀티스테이지 `Dockerfile`
  - 로컬 `docker-compose.yml` (`app + MySQL + Redis`)
  - 5개 실행 정책 파일 (`application`, `local`, `local-emulator`, `prod`, `test`)
  - env 기반 `local` 프로필, health/info, OpenAPI 운영 노출 정책
  - GitHub Actions CD 초안 (`production` 환경 승인 + 멀티플랫폼 이미지 배포 기준)
  - 배포/운영/롤백 가이드 문서
- Member 라이프사이클 (Phase 10)
  - `DELETE /v1/members/me` 기반 탈퇴 API
  - soft delete tombstone + 개인정보 스크럽 + 도메인 후처리
  - 탈퇴 회원 접근 차단 및 동일 Firebase UID 재가입 차단
- 운영 공통 Admin 인프라 (Phase 11)
  - `@AdminApiAccess` 기반 공통 인가
  - `admin_audit_logs` 감사 로그 저장 인프라
  - Support Admin 목록 공통 규약 (`PageResponse`, `page=0`, `size=20`, `size<=100`, `createdAt,DESC`)
- 다음 예정 (Phase 12)
  - 이미지/미디어 업로드 인프라
  - 현재 게시판/채팅/앱 공지는 업로드된 파일의 URL만 입력받고, 서버 경유 업로드 API(`/v1/images`)는 아직 미구현
- CI
  - GitHub Actions `build` 체크 (`./gradlew clean build`)

핵심 패키지:

```text
src/main/java/com/skuri/skuri_backend
├── common
├── domain/academic
├── domain/app
├── domain/board
├── domain/chat
├── domain/member
├── domain/notice
├── domain/support
├── domain/taxiparty
├── infra/auth
└── infra/openapi
```

## 4. 기술 스택

- Java 21
- Spring Boot 4.0.3
- Gradle
- Spring Web MVC, Spring Data JPA, Validation
- MySQL

## 5. 빠른 시작 (권장: Docker Compose)

### 5-1. 사전 준비

- Docker Engine 또는 Docker Desktop
- Docker Compose Plugin
- (선택) JDK 21 (`./gradlew`로 호스트 실행 시)
- (선택) Firebase CLI (`local-emulator` 사용 시)

```bash
git clone https://github.com/jisung-louis/skuri-backend.git
cd skuri-backend
```

### 5-2. `.env` 준비

권장 방식은 루트의 `.env.example`를 기준으로 `.env`를 준비하는 것입니다.

- `application.yaml`은 공통 기본값, `local`은 실제 Firebase 기반 로컬 통합 테스트, `local-emulator`는 Firebase Auth Emulator 기반 백엔드 단독 테스트, `prod`는 운영 서버, `test`는 자동 테스트 전용입니다.
- 로컬 기본 DB는 `localhost:3306`입니다. 다만 Docker MySQL을 `3307` 같은 다른 포트로 띄웠다면 `DB_URL` 환경변수로 덮어쓸 수 있습니다.
- `local`은 실제 Firebase를 사용하므로 `FIREBASE_PROJECT_ID`와 `FIREBASE_CREDENTIALS_PATH`(또는 `GOOGLE_APPLICATION_CREDENTIALS`)가 필요합니다.
- `local-emulator`는 실제 Firebase 서비스 계정 파일이 없어도 동작해야 하므로 `FIREBASE_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`는 비워 두는 것을 권장합니다.
- 현재 기본 `docker-compose.yml`은 Firebase 자격증명 파일을 자동 마운트하지 않습니다. 실제 Firebase 인증까지 검증하려면 앱은 호스트에서 `bootRun`으로 실행하는 편이 더 쉽습니다.
- `.env`는 "실제 값", `application-*.yaml`은 "환경별 정책"을 담당합니다.
- 브라우저에서 REST API를 호출하는 관리자 페이지가 있으면 `API_ALLOWED_ORIGIN_PATTERNS`에 허용 Origin을 넣어야 합니다.
- WebSocket `/ws` 허용 Origin은 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS`로 별도 관리합니다.

### 5-3. 전체 환경 기동

```bash
docker compose up -d --build
```

평소 개발은 아래처럼 DB/Redis만 Docker로 띄우고, 앱은 IntelliJ 또는 `bootRun`으로 호스트에서 직접 실행하는 방식을 가장 권장합니다.

```bash
docker compose up -d mysql redis
```

### 5-4. 호스트에서 앱만 실행하고 싶을 때

```bash
set -a
source .env
set +a
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

- `local`은 프론트가 실제 Firebase 로그인 후 받은 ID Token으로 API를 호출하는 흐름을 재현합니다.
- `local`은 실제 Firebase 자격증명 파일이 필요합니다.
- IntelliJ 실행 설정에서는 `.env` 파일 경로를 환경 변수 칸에 넣지 말고, 필요한 값을 `KEY=value` 형식으로 직접 입력해야 합니다.
- 예시:

```bash
DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 \
DB_USERNAME=root \
DB_PASSWORD=1234 \
FIREBASE_PROJECT_ID=sktaxi-acb4c \
FIREBASE_CREDENTIALS_PATH=/Users/<user>/skuri-backend/serviceAccountKey.json \
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`local-emulator`를 사용하려면:

```bash
export FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
export FIREBASE_PROJECT_ID=sktaxi-acb4c
export FIREBASE_CREDENTIALS_PATH=
export GOOGLE_APPLICATION_CREDENTIALS=
./bin/start-firebase-auth-emulator.sh
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```

- 위 예시는 호스트에서 앱을 직접 실행할 때 기준입니다.
- `local-emulator`는 Firebase Auth Emulator가 발급한 토큰으로 백엔드만 빠르게 검증하는 프로필입니다.
- 이 프로필은 실제 Firebase 자격증명 파일이 없어도 동작해야 하므로 `FIREBASE_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`는 비워 두는 것이 안전합니다.
- `local-emulator`에서는 전체 `.env`를 그대로 로드하지 말고, emulator에 필요한 값만 별도로 주입하는 편이 안전합니다.
- Docker 컨테이너에서 emulator를 바라볼 때는 보통 `FIREBASE_AUTH_EMULATOR_HOST=host.docker.internal:9099` 처럼 호스트 주소를 사용해야 합니다.
- `local-emulator`의 기본 스키마 정책은 `update`이며, 로컬 개발 DB를 재생성하지 않습니다. 스키마 초기화가 필요하면 emulator 전용 DB를 따로 사용하세요.

### 5-5. 실행 확인

```bash
curl http://localhost:8080/actuator/health
./gradlew build
./gradlew test
```

- OpenAPI 확인 (`local`, `local-emulator` 기본 노출):
  - `http://localhost:8080/swagger-ui/index.html`
  - `http://localhost:8080/scalar`

자세한 `.env`, 운영 서버, CD, 롤백 절차는 [deployment-guide.md](docs/deployment-guide.md)를 참고합니다.

## 6. CI / CD (GitHub Actions)

워크플로: [.github/workflows/ci.yml](.github/workflows/ci.yml)
- 배포 가이드: [deployment-guide.md](docs/deployment-guide.md)

- 트리거: `main` 대상 PR, `main` push
- 필수 체크 이름: `build`
- 실행 명령: `./gradlew clean build --no-daemon`

CD 워크플로: `.github/workflows/cd.yml`

- 트리거: `main` push, 수동 실행
- 흐름: Docker 이미지 빌드/GHCR push (`linux/amd64,linux/arm64`) → `production` 환경 승인 대기 → 승인 후 OCI 서버 배포
- 배포 후 smoke check: `health`, `GET /v1/app-versions/android`, admin REST CORS preflight, prod OpenAPI 비노출
- 새 `main` push가 오면 이전 CD run은 자동 취소되고 최신 run만 남깁니다.
- 반자동 배포를 원하면 GitHub `production` Environment의 `Required reviewers`를 반드시 설정해야 합니다.

필수 Secrets:

- `CI_DB_NAME`
- `CI_DB_USER`
- `CI_DB_PASSWORD`
- `CI_DB_ROOT_PASSWORD`

주의: 위 값은 로컬 DB 계정이 아니라 **CI 전용 DB 값**을 사용합니다.

CD용 Secrets와 서버 준비 항목은 [deployment-guide.md](docs/deployment-guide.md)에 따로 정리했습니다.

## 7. 브랜치/PR 운영 규칙

상세 규칙: [AGENTS.md](AGENTS.md)

- `main` 직접 작업 금지 (PR 필수)
- 기능/버그 단위 브랜치 사용
- Squash merge 기본 사용
- 머지 전 `build` 체크 통과 필수

## 8. 문서 안내

- 제품/서비스 맥락: [project-overview.md](docs/project-overview.md)
- API 계약: [api-specification.md](docs/api-specification.md)
- 도메인 분석: [domain-analysis.md](docs/domain-analysis.md)
- ERD: [erd.md](docs/erd.md)
- 역할 정의: [role-definition.md](docs/role-definition.md)
- 배포/운영 가이드: [deployment-guide.md](docs/deployment-guide.md)
- Postman 회귀 컬렉션: [postman_collection.json](etc/postman_collection.json)
- Phase 7 Support 수동 검증: `etc/postman_collection.json > 07. Support`
- Phase 6 Academic 수동 검증: `etc/postman_collection.json > 06. Academic`
- Academic Postman 예약 학기: `2099-1`, `2099-2`, `2100-1`
- 초창기 점검표: [early-stage-checklist.md](docs/early-stage-checklist.md)

구현 단계 계획이 필요할 때만 참고:
- [implementation-roadmap.md](docs/implementation-roadmap.md)

### API 계약/실시간 보안 정책

- REST 계약의 런타임 기준: `/v3/api-docs`
- 운영(`prod`)에서는 기본적으로 OpenAPI UI/JSON을 비노출로 운영
- 사람이 읽는 명세 문서(`docs/api-specification.md`)는 코드 변경과 같은 PR에서 동기화
- WebSocket은 CONNECT 인증 + 목적지별 인가를 모두 적용
- 브라우저 REST API CORS 허용 Origin은 `API_ALLOWED_ORIGIN_PATTERNS`, WebSocket CORS 허용 Origin은 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS` 또는 프로필 설정으로 관리 (`*` 금지)

### Serena Memory 동기화 규칙 (Codex + Serena)

- Serena 메모리는 `.serena/memories/*.md`에 저장되며, 코드/기능/정책이 바뀌면 같은 작업 단위에서 함께 갱신합니다.
- 변경 유형별 최소 동기화 대상:
  - 아키텍처/도메인 변경: `project_overview`, `codebase_structure`
  - 규칙/정책 변경: `code_style_and_conventions`, `task_completion_checklist`
  - 실행/검증 명령 변경: `suggested_commands`
- PR 설명에는 "Serena Memory 동기화 내역"을 함께 기록합니다.

## 9. 보안 원칙

- `application.yaml`에는 민감정보를 직접 넣지 않고 환경변수 참조만 사용
- 비밀번호/토큰/키 파일은 커밋 금지
- 로컬/CI/운영 비밀값은 반드시 분리
- 로컬/운영 런타임 값은 `.env`로 관리하되, CI/CD 보관소는 GitHub Secrets를 유지
- Firebase 서비스 계정 JSON은 서버 파일로 보관하고 `GOOGLE_APPLICATION_CREDENTIALS` 경로만 주입
