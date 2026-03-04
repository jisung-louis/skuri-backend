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

대상 사용자: `@sungkyul.ac.kr` 계정을 가진 성결대학교 학생

## 3. 저장소 현재 상태(백엔드 관점)

현재 저장소는 **Phase 2(TaxiParty)** 까지 구현된 상태입니다.

- 공통 계층
  - 공통 응답/페이지 DTO: `ApiResponse`, `PageResponse`
  - 공통 예외 처리: `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`
  - 공통 엔티티 기반: `BaseTimeEntity`, `JpaAuditingConfig`
- 인증/보안
  - Firebase ID Token 검증 기반 인증
  - `@sungkyul.ac.kr` 이메일 도메인 정책 적용
  - `local-emulator` 프로필 지원 (Firebase Auth Emulator)
- Member 도메인
  - 회원 가입/프로필 조회/프로필 수정
- TaxiParty 도메인 (Phase 2)
  - 파티/동승요청/정산/상태머신/권한 규칙
  - Optimistic Lock(`@Version`) 기반 동시성 충돌 방어
  - 12시간 초과 파티 자동 종료 스케줄러
  - SSE: 파티 목록/상태, 동승요청(리더/요청자) 실시간 구독
- CI
  - GitHub Actions `build` 체크 (`./gradlew clean build`)

핵심 패키지:

```text
src/main/java/com/skuri/skuri_backend
├── common
├── domain/member
├── domain/taxiparty
└── infra/auth
```

### 3-1. Phase별 구현 완료 API 요약 (Phase 0~2)

> 상세 계약/예시는 [docs/api-specification.md](docs/api-specification.md)를 기준으로 확인합니다.

| Phase | 도메인 | 구현 상태 | 구현 완료 API 요약 |
|------|------|------|------|
| Phase 0 | Public/Common | 완료 | `GET /v1/app-versions/**`, `GET /v1/app-notices`, OpenAPI 노출(`GET /v3/api-docs/**`, `GET /swagger-ui/**`, `GET /scalar/**`) |
| Phase 1 | Member/Auth | 완료 | `POST /v1/members`, `GET /v1/members/me`, `PATCH /v1/members/me`, `POST /v1/members/me/fcm-tokens`, `DELETE /v1/members/me/fcm-tokens` |
| Phase 2 | TaxiParty | 완료 | 파티 생성/조회/수정/마감/재개/도착/종료/취소, 멤버 강퇴/탈퇴, 동승 요청 생성/수락/거절/취소/조회, 정산 확인, 내 파티/요청 조회 |
| Phase 2 | TaxiParty Realtime(SSE) | 완료 | `GET /v1/sse/parties`, `GET /v1/sse/parties/{partyId}/join-requests`, `GET /v1/sse/members/me/join-requests` |

## 4. 기술 스택

- Java 21
- Spring Boot 4.0.3
- Gradle
- Spring Web MVC, Spring Data JPA, Validation
- MySQL

## 5. 빠른 시작 (초보자용)

### 5-1. 사전 준비

- JDK 21 설치
- MySQL 실행
- (선택) Firebase CLI 설치 (`local-emulator` 사용 시 필요)

```bash
git clone https://github.com/jisung-louis/skuri-backend.git
cd skuri-backend
```

### 5-2. 로컬 DB 준비

```sql
CREATE DATABASE skuri CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 5-3. 로컬 설정 (둘 중 하나)

방법 A. `local` 프로필 (실제 Firebase 검증)

- 파일: `src/main/resources/application-local.yaml`
- `credentials-path`에 로컬 서비스 계정 키 경로 지정
- `serviceAccountKey.json`은 `.gitignore` 대상 (커밋 금지)

```yaml
spring:
  config:
    activate:
      on-profile: local
firebase:
  auth:
    credentials-path: /Users/you/skuri-backend/serviceAccountKey.json
    project-id: your_firebase_project_id
```

방법 B. `local-emulator` 프로필 (Firebase Auth Emulator 검증)

```bash
export FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
export FIREBASE_PROJECT_ID=sktaxi-acb4c
./bin/start-firebase-auth-emulator.sh
```

- `application-local-emulator.yaml`에서 `firebase.auth.use-emulator=true`를 사용
- 서버 종료 시 emulator PID 파일을 기준으로 자동 종료되도록 설정 가능

### 5-4. 실행/검증

`local` 프로필 실행:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`local-emulator` 프로필 실행:

```bash
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```

빌드/테스트:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew build
SPRING_PROFILES_ACTIVE=local ./gradlew test
```

Swagger/Scalar 확인:

- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/scalar`

## 6. CI (GitHub Actions)

워크플로: [.github/workflows/ci.yml](.github/workflows/ci.yml)

- 트리거: `main` 대상 PR, `main` push
- 필수 체크 이름: `build`
- 실행 명령: `./gradlew clean build --no-daemon`

필수 Secrets:

- `CI_DB_NAME`
- `CI_DB_USER`
- `CI_DB_PASSWORD`
- `CI_DB_ROOT_PASSWORD`

주의: 위 값은 로컬 DB 계정이 아니라 **CI 전용 DB 값**을 사용합니다.

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
- 초창기 점검표: [early-stage-checklist.md](docs/early-stage-checklist.md)

구현 단계 계획이 필요할 때만 참고:
- [implementation-roadmap.md](docs/implementation-roadmap.md)

## 9. 보안 원칙

- `application.yaml`에는 민감정보를 직접 넣지 않고 환경변수 참조만 사용
- 비밀번호/토큰/키 파일은 커밋 금지
- 로컬/CI/운영 비밀값은 반드시 분리
