# SKURI Backend - 프로젝트 개요

## 목적
성결대학교 학생 대상 택시 합승/커뮤니티/공지/학사 서비스 백엔드다. 핵심 도메인은 TaxiParty이며, 알림과 실시간 기능은 핵심 트랜잭션의 부가효과로 분리한다.

## 기술 스택
- Java 21, Spring Boot 4.0.3, Gradle
- MySQL 8.4 (prod), H2 (test)
- Spring Data JPA + Hibernate
- Firebase Admin SDK (ID Token 검증, FCM sender)
- 실시간: WebSocket(STOMP), SSE
- OpenAPI: springdoc Swagger UI + Scalar

## 도메인 구조
- member: 회원, 프로필, 알림 설정, FCM 토큰, 탈퇴/재가입 정책
- taxiparty: 파티 생성/참여/정산/상태 전이, SSE, 택시 history/summary
- chat: 공개 채팅방/파티 채팅방, SYSTEM/ARRIVED/END 서버 메시지, 공개방 seed
- board: 게시글/댓글/좋아요/북마크, 이미지 썸네일 규칙
- notice: 학교 공지/댓글/읽음/북마크/앱 공지
- academic: 강의/시간표/학사 일정
- campus: 캠퍼스 배너 공개/관리자 API
- support: 문의/신고/앱 버전/법적 문서/학식
- notification: 인앱 인박스, FCM, SSE, 이벤트 기반 알림 처리

## Legal Document 메모
- `LegalDocument`는 `termsOfUse`, `privacyPolicy` 두 고정 키를 `legal_documents` 테이블에서 관리한다.
- 공개 API는 `GET /v1/legal-documents/{documentKey}`이며 `isActive=true` 문서만 조회 가능하다. 비활성 또는 미존재 문서는 `404 LEGAL_DOCUMENT_NOT_FOUND`를 반환한다.
- 관리자 API는 `GET /v1/admin/legal-documents`, `GET /v1/admin/legal-documents/{documentKey}`, `PUT /v1/admin/legal-documents/{documentKey}`, `DELETE /v1/admin/legal-documents/{documentKey}`다.
- 초기 이용약관/개인정보 처리방침 2건은 `seed_migrations` 기반 1회성 seed migration으로 적재한다. 이후에는 관리자 CRUD로만 수정한다.
- 문서 본문 구조는 프론트 계약과 맞춘 `banner`, `sections`, `footerLines`를 JSON 컬럼으로 저장한다.

## 인프라/공통
- 공통 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`, `ErrorCode` 중심으로 처리한다.
- 로컬 프로필은 `local`, `local-emulator`, 운영은 `prod`, 자동 테스트는 `test`를 사용한다.
- OpenAPI는 로컬에서 노출하고 `prod`에서는 기본 비노출이다.
- 상태 변경 이후 알림/외부 후처리는 after-commit semantics를 따른다.
- Admin API는 `@AdminApiAccess`, `ADMIN_REQUIRED`, `admin_audit_logs` 기반 정책을 사용한다.
