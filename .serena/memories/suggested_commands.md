# 개발에 필요한 명령어 모음

## 빌드
```bash
./gradlew build
./gradlew compileJava compileTestJava
env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build
env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew compileJava compileTestJava
```

## 테스트
```bash
./gradlew test
./gradlew test --tests "com.skuri.skuri_backend.domain.image.controller.ImageControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.image.service.ImageUploadServiceTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.image.integration.ImageUploadBoardFlowIntegrationTest"
env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest"
env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.controller.NotificationControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.controller.NotificationSseControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.service.NotificationServiceTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.notification.service.NotificationEventHandlerTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.chat.service.PartyMessageServiceTest" --tests "com.skuri.skuri_backend.domain.chat.service.ChatServiceTest" --tests "com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyServiceTest" --tests "com.skuri.skuri_backend.domain.taxiparty.controller.PartyControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.domain.chat.controller.ChatRoomControllerContractTest" --tests "com.skuri.skuri_backend.domain.chat.service.ChatServiceTest" --tests "com.skuri.skuri_backend.domain.chat.service.PublicChatRoomSeedMigrationTest" --tests "com.skuri.skuri_backend.domain.member.service.MemberServiceTest" --tests "com.skuri.skuri_backend.domain.member.controller.MemberControllerContractTest"
./gradlew test --tests "com.skuri.skuri_backend.common.event.AfterCommitApplicationEventPublisherTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.SecurityIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.SecurityInfraCorsIntegrationTest"
SPRING_PROFILES_ACTIVE=local ./gradlew test --tests "com.skuri.skuri_backend.domain.taxiparty.repository.PartyRepositoryDataJpaTest"
```

## 서버 실행
```bash
docker compose up -d mysql redis
MEDIA_STORAGE_BASE_DIR=$(pwd)/var/media MEDIA_STORAGE_PUBLIC_BASE_URL=http://localhost:8080/uploads SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
MEDIA_STORAGE_PROVIDER=FIREBASE MEDIA_STORAGE_FIREBASE_BUCKET=gs://sktaxi-acb4c.firebasestorage.app FIREBASE_PROJECT_ID=sktaxi-acb4c FIREBASE_CREDENTIALS_PATH=/Users/<user>/skuri-backend/serviceAccountKey.json SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
curl -X POST http://localhost:8080/v1/images -H "Authorization: Bearer <token>" -F "context=POST_IMAGE" -F "file=@/absolute/path/to/sample.jpg"
docker compose logs -f mysql
docker compose logs -f redis
docker compose down
curl http://localhost:8080/actuator/health
IMAGE_URI=ghcr.io/example/skuri:test FIREBASE_CREDENTIALS_FILE=/tmp/firebase-admin.json GOOGLE_APPLICATION_CREDENTIALS=/app/secrets/firebase-admin.json docker compose -f docker-compose.prod.yml config
docker compose -f docker-compose.prod.yml ps
ss -ltnp | grep 8080
ss -ltnp | grep 3307
curl -i -X OPTIONS 'http://127.0.0.1:8080/v1/app-versions/android' -H 'Origin: <CD_SMOKE_CORS_ORIGIN or first exact API_ALLOWED_ORIGIN_PATTERNS entry>' -H 'Access-Control-Request-Method: GET'
curl -o /dev/null -s -w '%{http_code}\n' http://127.0.0.1:8080/v3/api-docs
mysql -u <user> -p<password> -h <host> -P <port> <database> -e "ALTER TABLE members ADD COLUMN status VARCHAR(20) NULL AFTER is_admin; UPDATE members SET status = 'ACTIVE' WHERE status IS NULL; ALTER TABLE members MODIFY COLUMN status VARCHAR(20) NOT NULL;"
```

```bash
DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 DB_USERNAME=root DB_PASSWORD=1234 FIREBASE_PROJECT_ID=sktaxi-acb4c FIREBASE_CREDENTIALS_PATH=/Users/<user>/skuri-backend/serviceAccountKey.json SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 DB_USERNAME=root DB_PASSWORD=1234 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=sktaxi-acb4c FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```
- 평소 개발은 MySQL/Redis만 Docker로 올리고 앱은 호스트에서 실행하는 방식을 권장한다.
- Docker Compose 실행은 `.env`를 자동으로 읽지만, IDE 실행 설정은 `.env` 파일 경로를 직접 읽지 않으므로 필요한 값을 환경변수 칸에 `KEY=value` 형식으로 넣어야 한다.
- Docker 컨테이너에서 Firebase Auth Emulator를 사용할 때는 보통 `FIREBASE_AUTH_EMULATOR_HOST=host.docker.internal:9099`를 사용한다.
- 운영 MySQL Workbench 접속은 `127.0.0.1:3307` loopback 바인딩 + SSH 방식만 사용한다.
