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
