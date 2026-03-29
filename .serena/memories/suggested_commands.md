# 개발에 필요한 명령어 모음

## 빌드
```bash
./gradlew build
./gradlew compileJava compileTestJava
```

## Inquiry Attachment 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.support.controller.InquiryControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.controller.InquiryAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.service.InquiryServiceTest" --tests "com.skuri.skuri_backend.domain.image.service.ImageUploadServiceTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest" --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest"
./gradlew build
```

## Legal Document 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.support.controller.LegalDocumentControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.controller.LegalDocumentAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.service.LegalDocumentServiceTest" --tests "com.skuri.skuri_backend.domain.support.service.LegalDocumentSeedMigrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest"
./gradlew build
```

## 자주 쓰는 테스트
```bash
./gradlew test
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest"
```

## 서버 실행
```bash
docker compose up -d mysql redis
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 DB_USERNAME=root DB_PASSWORD=1234 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=sktaxi-acb4c FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```

## Admin Member API 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.member.controller.MemberAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.member.service.MemberAdminServiceTest" --tests "com.skuri.skuri_backend.domain.member.repository.MemberRepositoryDataJpaTest" --tests "com.skuri.skuri_backend.domain.notification.controller.NotificationControllerContractTest" --tests "com.skuri.skuri_backend.domain.notification.service.FcmTokenServiceTest" --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin Member Activity 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18081 ./gradlew bootRun
curl http://127.0.0.1:18081/v1/admin/members/<MEMBER_ID>/activity -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl http://127.0.0.1:18081/v3/api-docs
curl http://127.0.0.1:18081/swagger-ui/index.html
curl http://127.0.0.1:18081/scalar
```

## local-emulator 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18081 ./gradlew bootRun
curl -X PATCH "http://127.0.0.1:18081/v1/admin/members/<ADMIN_MEMBER_ID>/admin-role" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' -d '{"isAdmin":false}'
curl http://127.0.0.1:18081/v3/api-docs
curl http://127.0.0.1:18081/swagger-ui/index.html
curl http://127.0.0.1:18081/scalar
```
