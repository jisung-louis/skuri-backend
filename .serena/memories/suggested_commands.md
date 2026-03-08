# 개발에 필요한 명령어 모음

## 빌드
```bash
./gradlew build
./gradlew compileJava compileTestJava
```

## 테스트
```bash
./gradlew test
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.controller.NotificationControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.controller.NotificationSseControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.service.NotificationServiceTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.service.NotificationEventHandlerTest"
./gradlew test --tests "com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisherTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.SecurityIntegrationTest"
SPRING_PROFILES_ACTIVE=local ./gradlew test --tests "com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepositoryDataJpaTest"
```

## 서버 실행
```bash
docker compose up -d --build
docker compose logs -f app
docker compose down
curl http://localhost:8080/actuator/health
IMAGE_URI=ghcr.io/example/skuri:test FIREBASE_CREDENTIALS_FILE=/tmp/firebase-admin.json GOOGLE_APPLICATION_CREDENTIALS=/app/secrets/firebase-admin.json docker compose -f docker-compose.prod.yml config
```

```bash
set -a
source .env
set +a
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

set -a
source .env
set +a
export FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```
- 호스트 실행 시 `.env`를 먼저 로드하거나 IDE 환경변수로 주입
- Docker Compose 실행은 `.env`를 자동으로 읽음
- Firebase Auth Emulator를 Docker 컨테이너에서 사용할 때는 보통 `FIREBASE_AUTH_EMULATOR_HOST=host.docker.internal:9099`를 사용

## 문서/계약 확인
```bash
open build/reports/tests/test/index.html
open http://localhost:8080/swagger-ui/index.html
open http://localhost:8080/scalar
curl http://localhost:8080/v3/api-docs
```

## Git 워크플로우
```bash
git checkout main
git pull origin main
git checkout -b codex/feat/<topic>
git status --short --branch
git add <files>
git commit -m "feat: ..."
git push -u origin <branch>
gh pr create --title "..." --body-file /tmp/pr.md
```
