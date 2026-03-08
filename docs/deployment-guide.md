# SKURI Phase 9 배포 / 운영 가이드

## 1. 확정 전략

| 항목 | 결정 |
|------|------|
| 로컬 실행 | `docker-compose.yml`로 `app + MySQL + Redis` 기동 |
| 운영 구조 | `EC2 1대(app 컨테이너) + RDS MySQL` |
| Redis 운영 반영 | 이번 Phase에서는 미도입. 로컬 컨테이너와 환경변수만 준비 |
| 프로필 | `application / local / local-emulator / dev / prod / test` |
| CD 방식 | 반자동. `main` 반영 후 GitHub `production` 환경 승인 시 배포 |
| OpenAPI 노출 | `local/dev` 노출, `prod` 기본 비노출 |
| Firebase 자격증명 | 서버 파일 + `GOOGLE_APPLICATION_CREDENTIALS` 경로 주입 |

## 2. 환경변수 원칙

- 로컬 개발: `.env`
- 운영 서버: 서버 안의 `.env`
- GitHub Actions: GitHub Secrets
- 프로필 파일(`application-*.yaml`)은 환경별 정책을 공유하고, `.env`는 실제 값을 주입한다.
- 실제 비밀값은 Git 저장소에 넣지 않는다.
- Firebase 서비스 계정 JSON은 `.env`에 본문을 넣지 않고 파일로 두고 경로만 `.env`에 넣는다.

예시:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<rds-endpoint>:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<db-user>
DB_PASSWORD=<db-password>
APP_HOST_PORT=8080
OPENAPI_ENABLED=false
CHAT_WS_ALLOWED_ORIGIN_PATTERNS=https://api.skuri.example
FIREBASE_CREDENTIALS_FILE=/opt/skuri/secrets/firebase-admin.json
GOOGLE_APPLICATION_CREDENTIALS=/opt/skuri/secrets/firebase-admin.json
```

## 3. 로컬 실행

1. 루트의 `.env.example`를 기준으로 `.env`를 준비한다.
2. 필요하면 Firebase 관련 값을 채운다.
3. 아래 명령으로 전체 환경을 올린다. (`docker compose`는 `.env`를 자동으로 읽는다.)

```bash
docker compose up -d --build
```

호스트에서 앱만 직접 실행하려면 `.env`를 먼저 쉘 환경변수로 로드한다.

```bash
set -a
source .env
set +a
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

확인 포인트:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/scalar`

참고:

- 인증이 필요한 API까지 테스트하려면 실제 Firebase 자격증명 또는 `local-emulator` 설정이 추가로 필요하다.
- 현재 기본 `docker-compose.yml`은 Firebase 자격증명 파일을 자동 마운트하지 않는다. Docker에서 실제 Firebase 인증까지 검증하려면 자격증명 파일 volume mount를 별도로 추가하거나, 앱은 호스트에서 `bootRun`으로 실행한다.
- Redis는 지금 단계에서 앱 로직에 연결되지 않으므로 컨테이너 준비 수준이다.
- `dev` 프로필은 팀 공유 개발 서버용으로 남겨두고, 로컬 개발은 `local`을 기준으로 사용한다.

## 4. 운영 서버 준비

운영 서버는 다음을 미리 준비해야 한다.

- Docker Engine
- Docker Compose Plugin
- 앱 배포 디렉터리
  - 기본값: `/opt/skuri/app`
- 운영 `.env`
  - 기본 위치: `/opt/skuri/app/.env`
- Firebase 서비스 계정 JSON
  - 예: `/opt/skuri/secrets/firebase-admin.json`

운영 서버에 두는 파일:

- `/opt/skuri/app/.env`
- `/opt/skuri/app/docker-compose.prod.yml`
- `/opt/skuri/secrets/firebase-admin.json`

## 5. GitHub Actions CD 흐름

반자동 배포 흐름은 다음과 같다.

1. `main`에 push 또는 merge
2. `CD` workflow가 자동 시작
3. Docker 이미지를 GHCR에 push
4. `Deploy To Production` job이 `production` 환경 승인 대기 상태로 멈춤
5. GitHub에서 `Review deployments`
6. `Approve and deploy`
7. EC2에 접속해서 최신 이미지 pull 및 재기동
8. 서버 내부에서 `/actuator/health` smoke check

중요:

- 이 흐름이 진짜 반자동이 되려면 GitHub Repository Settings에서 `production` Environment에 `Required reviewers`를 설정해야 한다.
- Environment 보호 규칙이 없으면 deploy job은 자동으로 바로 진행된다.

## 6. GitHub Secrets

`production` Environment 기준으로 아래 Secrets를 준비한다.

- `PROD_EC2_HOST`
- `PROD_EC2_USERNAME`
- `PROD_EC2_SSH_KEY`
- `PROD_EC2_SSH_PORT` (선택, 기본값 `22`)
- `PROD_DEPLOY_DIR` (선택, 기본값 `/opt/skuri/app`)
- `PROD_GHCR_USERNAME`
- `PROD_GHCR_READ_TOKEN`

참고:

- GitHub Actions는 `.env` 자체를 저장하지 않는다.
- CI/CD에서는 Secrets를 사용하고, 운영 서버는 자신의 `.env`를 유지한다.

## 7. OpenAPI 운영 정책

- `local`, `dev`: `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar` 사용 가능
- `prod`: 기본 비노출
- 운영에서 임시로 열어야 하면 `.env`의 `OPENAPI_ENABLED=true`로 조정할 수 있다.

## 8. Redis 운영 범위

이번 Phase의 Redis 범위는 아래까지만 포함한다.

- 로컬 `docker-compose.yml`에 Redis 컨테이너 추가
- 공통 설정 파일에 Redis host/port 환경변수 자리 확보
- 운영 설계 문서에 “후속 캐시 도입 후보”로 정리

이번 Phase에서 하지 않는 것:

- `@Cacheable` 적용
- 캐시 무효화 정책 설계
- ElastiCache 실제 연결

후속 후보:

- 파티 목록 캐시
- 공지 목록 캐시
- FCM 토큰 조회 캐시

## 9. 배포 전 체크리스트

- `./gradlew build` 성공
- Docker 이미지 빌드 성공
- 운영 `.env` 값 최신화 확인
- Firebase 자격증명 파일 존재 확인
- RDS 연결 정보 확인
- OpenAPI 노출 정책(`OPENAPI_ENABLED`) 확인
- GitHub `production` Environment 보호 규칙 확인

## 10. 배포 후 체크리스트

- `docker ps`에서 `skuri-backend` 정상 실행 확인
- `curl http://127.0.0.1:<APP_HOST_PORT>/actuator/health`
- 공개 API smoke check
  - `GET /v1/app-versions/{platform}`
- 인증 API smoke check
  - 토큰이 있으면 `GET /v1/members/me`
- 컨테이너 로그 확인
  - `docker logs --tail 200 skuri-backend`
- OpenAPI가 prod에서 의도대로 닫혀 있는지 확인

## 11. 롤백

롤백은 “이전 이미지 태그로 다시 기동” 방식으로 진행한다.

1. GHCR에서 이전 정상 이미지 태그를 확인한다.
2. 서버에서 해당 태그를 지정해 다시 배포한다.

예시:

```bash
cd /opt/skuri/app
IMAGE_URI=ghcr.io/<owner>/<repo>:<previous-sha> docker compose -f docker-compose.prod.yml up -d
```

롤백 후 반드시 다시 확인할 것:

- `/actuator/health`
- 공개 API 1개
- 인증 API 1개
- 최근 에러 로그

## 12. 향후 외부 AI 서비스 메모

향후 Python 기반 AI 서버를 붙일 가능성이 있다면 지금 단계에서 최소한 아래 환경변수 네이밍만 확보해두면 충분하다.

- `AI_SERVICE_BASE_URL`
- `AI_SERVICE_AUTH_TOKEN`
- `AI_SERVICE_TIMEOUT_MS`

권장 원칙:

- Spring과 Python은 HTTP API로 통신
- AI 장애가 공지 기본 조회를 깨지 않도록 fallback 유지
- 운영에서는 가능하면 같은 VPC/사설망에 배치
