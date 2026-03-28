# 코드베이스 구조 상세

## 핵심 엔트리포인트
- `SkuriBackendApplication.java`
- 프로필 파일: `src/main/resources/application.yaml`, `application-local.yaml`, `application-local-emulator.yaml`, `application-prod.yaml`, `src/test/resources/application-test.yaml`
- 배포/운영: `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`, `.github/workflows/ci.yml`, `.github/workflows/cd.yml`
- 주요 문서: `docs/project-overview.md`, `docs/implementation-roadmap.md`, `docs/api-specification.md`, `docs/domain-analysis.md`, `docs/erd.md`, `docs/role-definition.md`

## 주요 패키지 메모
- `domain/taxiparty`: 파티 상태 전이, 정산 snapshot, taxi history/summary
- `domain/chat`: 공개/파티 채팅, SYSTEM 메시지, 읽음 처리, 공개방 seed
- `domain/board`: 게시글/댓글/좋아요/북마크, 이미지 연결
- `domain/notice`: 학교 공지, 공지 댓글, 북마크, 앱 공지
- `domain/academic`: 강의, 시간표, 학사 일정
- `domain/campus`: 캠퍼스 배너 공개/관리자 API
- `domain/support`: 문의, 신고, 앱 버전, 법적 문서, 학식
- `domain/notification`: 인앱 알림, unread count, SSE snapshot
- `infra/auth`: Firebase 인증 필터, Admin guard, Security 설정
- `infra/admin`: admin audit, admin list 공통 인프라
- `infra/openapi`: OpenAPI 그룹/예시/wrapper schema

## Inquiry Attachment 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/InquiryAttachment.java`: 문의 첨부 이미지 메타데이터 value object
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/converter/InquiryAttachmentListJsonConverter.java`: inquiry attachments JSON 컬럼 converter
- `src/main/java/com/skuri/skuri_backend/domain/support/dto/request/CreateInquiryAttachmentRequest.java`: 문의 생성용 첨부 메타데이터 요청 DTO
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/Inquiry.java`: `attachments` JSON 컬럼 추가
- `src/main/java/com/skuri/skuri_backend/domain/support/service/InquiryService.java`: attachments null->[] 정규화, 최대 3개/MIME 검증, 사용자/관리자 응답 변환
- `src/main/java/com/skuri/skuri_backend/domain/image/dto/request/ImageUploadContext.java`: `INQUIRY_IMAGE` context 추가
- `src/test/java/com/skuri/skuri_backend/domain/support/controller/InquiryControllerContractTest.java`, `InquiryAdminControllerContractTest.java`: attachments 요청/응답 계약 검증
- `src/test/java/com/skuri/skuri_backend/domain/support/service/InquiryServiceTest.java`: attachments 정규화/검증 서비스 테스트
- `src/test/java/com/skuri/skuri_backend/domain/image/service/ImageUploadServiceTest.java`: `INQUIRY_IMAGE` 저장 경로 검증

## Legal Document 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/LegalDocument.java`: 문서 본문, 배너, 푸터, 활성 여부를 저장하는 엔티티
- `src/main/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentController.java`: 공개 조회 API
- `src/main/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentAdminController.java`: 관리자 목록/상세/저장/삭제 API
- `src/main/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentService.java`: documentKey 정규화, 공개/관리자 응답 변환, upsert/delete 비즈니스 규칙
- `src/main/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentSeedMigration.java`: 초기 이용약관/개인정보 처리방침 1회성 seed 로직
- `src/main/java/com/skuri/skuri_backend/domain/support/repository/LegalDocumentRepository.java`: active 공개 조회와 전체 관리자 목록 조회
- `src/main/java/com/skuri/skuri_backend/common/seed/entity/SeedMigration.java`, `.../repository/SeedMigrationRepository.java`: seed 적용 이력 기록용 marker 저장소
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiLegalSchemas.java`, `OpenApiLegalExamples.java`: legal document API용 OpenAPI schema/example 정의

## 테스트 포인트
- `src/test/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentControllerContractTest.java`
- `src/test/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentAdminControllerContractTest.java`
- `src/test/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentServiceTest.java`
- `src/test/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentSeedMigrationTest.java`
- `src/test/java/com/skuri/skuri_backend/infra/openapi/OpenApiSuccessSchemaCoverageIntegrationTest.java`
