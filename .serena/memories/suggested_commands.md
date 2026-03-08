# 개발에 필요한 명령어 모음

## 빌드
```bash
./gradlew build
SPRING_PROFILES_ACTIVE=local ./gradlew build
./gradlew compileJava compileTestJava
```

## 테스트
```bash
./gradlew test
SPRING_PROFILES_ACTIVE=local ./gradlew test
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
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
CHAT_WS_ALLOWED_ORIGIN_PATTERNS="http://localhost:3000,http://localhost:8081" SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```
- 로컬 MySQL 및 `skuri` DB 준비 필요
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 또는 `application-local.yaml` 필요

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
