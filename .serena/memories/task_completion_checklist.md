# 작업 완료 시 체크리스트

## 머지 전 필수 검증
1. `./gradlew build` 성공
2. 변경된 기능 관련 Contract/Service/Event/Security 테스트 수행 (이미지 업로드 변경이면 MIME/용량뿐 아니라 해상도/총 픽셀 제한 케이스도 포함)
3. API 정상/예외 케이스 최소 1개 이상 확인
4. `/v1/images`처럼 업로드/정적 리소스가 추가되면 공개 업로드 경로(GET)와 도메인 재사용 플로우(예: Board 저장)를 함께 확인
5. `ApiResponse` 에러 포맷 일관성 확인
6. OpenAPI example과 실제 `errorCode/message` 일치 확인
7. Scalar/Swagger의 success response `Show schema`에서 contract-critical `data` 필드가 concrete type으로 노출되는지 확인\n7-1. OpenAPI 문서화 전수 작업을 건드렸다면 `OpenApiSuccessSchemaCoverageIntegrationTest`와 `OpenApiUiAvailabilityIntegrationTest`를 함께 실행해 `/v3/api-docs` 기준 회귀를 확인\n8. OpenAPI/문서 동기화 확인 (`/v3/api-docs` 기준, `docs/api-specification.md`, lifecycle 정책 문서 포함)
8-1. shared 문서를 수정했다면 backend 문서와 함께 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/erd.md`를 같은 작업에서 즉시 동기화하고, `role-definition.md`를 건드렸다면 frontend 사본도 함께 동기화한다.
8-2. Campus 배너 계약을 바꿨다면 공개 노출 조건(`isActive`, `displayStartAt`, `displayEndAt`), `displayOrder` 연속값 유지, `IN_APP`/`EXTERNAL_URL` action 정합성, `CAMPUS_BANNER_IMAGE` 컨텍스트 문서화를 같이 확인한다.
8-3. TaxiParty/Chat 계약을 바꿨다면 join accept/member leave/close/reopen SYSTEM 메시지, ARRIVED/END 서버 메시지, ACCOUNT/settlement snapshot payload 예시가 OpenAPI와 런타임 응답에서 일치하는지 확인
8-4. 공개 채팅방 계약을 바꿨다면 공식 공개방 seed 생성, joined/not joined summary 필드, 미참여 공개방 detail 허용 + messages 차단, 학과 변경 시 학과방 membership 제거를 함께 확인
8-5. 회원/공개방 정책을 바꿨다면 active member 없이 create/join이 가능한지, department alias 정규화와 unsupported department 422가 맞는지, seed가 multi-instance에서도 중복 실패 없이 올라가는지 확인
8-6. 일반 Chat 읽음 계약을 바꿨다면 `PATCH /v1/chat-rooms/{id}/read`가 JS `new Date().toISOString()` UTC 문자열을 그대로 받고, markAsRead 후 summary/detail 재조회에서도 unread가 복원되지 않는지 확인한다. shared 문서를 수정했다면 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`를 같은 작업에서 즉시 동기화한다.
9. 회원 라이프사이클 변경이면 탈퇴 후 접근 차단, 동일 UID 재가입 차단, 연관 도메인 정합성 회귀 확인
10. SSE/Auth long-lived 경로를 건드렸다면 subscribe 메서드가 트랜잭션을 오래 유지하지 않는지, `spring.jpa.open-in-view=false`가 공통 설정에 유지되는지, Firebase auth async 재디스패치에서 member 조회가 재실행되지 않는지 확인
11. Serena Memory 동기화 확인
12. Admin 공통 변경이면 대표 Admin API에 대해 `401` / `403 ADMIN_REQUIRED` / 관리자 성공 시나리오를 확인한다.
13. 상태 변경 Admin API를 건드렸다면 `admin_audit_logs` row 생성과 `actor/target/diff` snapshot을 확인하고, `target_id`가 raw 입력이 아닌 canonical 키로 저장되는지 함께 확인한다.
14. Support Admin 목록 규약을 바꿨다면 `page/size` validation, `PageResponse`, 고정 정렬 문서 동기화를 함께 확인한다.
15. Inquiry 첨부 계약을 바꿨다면 `attachments` 생략/null -> 빈 배열 정규화, 최대 3개 제한, MIME 검증, `GET /v1/inquiries/my`와 Admin 문의 응답의 `attachments: []` 고정 규칙, `INQUIRY_IMAGE` context 문서화를 함께 확인한다.

## 운영/배포 변경 시 추가 검증
1. `./gradlew build` 성공
2. `docker compose` 설정 파일 문법/기동 절차 확인
2-1. 로컬 Docker 이미지 빌드 컨텍스트에 `application-local.yaml`, `application-local-emulator.yaml`이 포함되는지 확인
3. `/actuator/health` 응답 확인
4. `docker-compose.prod.yml` 렌더링과 MySQL/Redis/media 영속 볼륨 정책 확인
5. 운영 app host 바인딩이 `127.0.0.1:<APP_HOST_PORT>` loopback 으로만 열리는지 확인
6. `GET /v1/app-versions/android` 같은 공개 API smoke check 확인
7. `prod`에서 OpenAPI가 기본 비노출인지 확인
8. 브라우저 관리자 페이지가 있으면 허용 Origin의 REST CORS preflight와 WebSocket Origin 설정을 함께 확인
9. 로컬 프로필 변경 시 `local`은 실제 Firebase 자격증명 경로가 필요한지, `local-emulator`는 자격증명 경로 없이도 실행되는지 함께 확인
10. 배포 전/후 체크리스트와 rollback 문서 동기화 확인
11. 운영 MySQL 접근 정책을 바꿨다면 host 바인딩이 `127.0.0.1` loopback 으로만 열리는지 확인
12. Phase 10 이전 운영 DB 업그레이드면 앱 기동 전에 `members.status` 수동 마이그레이션 SQL을 먼저 적용했는지 확인
13. 관리자 회원 API를 건드렸다면 `GET /v1/admin/members`의 `PageResponse + joinedAt,DESC + query/status/isAdmin/department` 규약, `GET /v1/admin/members/{memberId}/activity`의 ACTIVE-only + current-data-only 정책, 삭제된 부모 게시글 댓글 제외 규칙, 비관리자 `403 ADMIN_REQUIRED`, 상세 응답의 `bankAccount`/`notificationSetting` 유지, 활동 요약의 count/recent list 정의, 권한 변경 성공/자기 자신 변경 `400 SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED`/탈퇴 회원 `409`, admin audit diff의 최소 snapshot(`id/email/nickname/isAdmin/status`)을 함께 확인한다.
14. 관리자 회원 정책은 activity summary에서 상태 변경/복원 로직을 추가하지 말고, 권한 변경도 self role change 금지만 기본으로 두며, 마지막 관리자 수 계산 같은 추가 제약은 docs/PR open question 없이 임의 확장하지 않는다.
15. 사용자 관리 placeholder를 닫는 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, 그리고 공유 계약 문서인 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md` 동기화를 확인한다.
