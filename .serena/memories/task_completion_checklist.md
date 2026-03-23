# 작업 완료 시 체크리스트

## 머지 전 필수 검증
1. `./gradlew build` 성공
2. 변경된 기능 관련 Contract/Service/Event/Security 테스트 수행 (이미지 업로드 변경이면 MIME/용량뿐 아니라 해상도/총 픽셀 제한 케이스도 포함)
3. API 정상/예외 케이스 최소 1개 이상 확인
4. `/v1/images`처럼 업로드/정적 리소스가 추가되면 공개 업로드 경로(GET)와 도메인 재사용 플로우(예: Board 저장)를 함께 확인
5. `ApiResponse` 에러 포맷 일관성 확인
6. OpenAPI example과 실제 `errorCode/message` 일치 확인
7. OpenAPI/문서 동기화 확인 (`/v3/api-docs` 기준, `docs/api-specification.md`, lifecycle 정책 문서 포함)
7-1. TaxiParty/Chat 계약을 바꿨다면 join accept/member leave/close/reopen SYSTEM 메시지, ARRIVED/END 서버 메시지, ACCOUNT/settlement snapshot payload 예시가 OpenAPI와 런타임 응답에서 일치하는지 확인
7-2. 공개 채팅방 계약을 바꿨다면 공식 공개방 seed 생성, joined/not joined summary 필드, 미참여 공개방 detail 허용 + messages 차단, 학과 변경 시 학과방 membership 제거를 함께 확인
7-3. 회원/공개방 정책을 바꿨다면 active member 없이 create/join이 가능한지, department alias 정규화와 unsupported department 422가 맞는지, seed가 multi-instance에서도 중복 실패 없이 올라가는지 확인
7-4. 일반 Chat 읽음 계약을 바꿨다면 `PATCH /v1/chat-rooms/{id}/read`가 JS `new Date().toISOString()` UTC 문자열을 그대로 받고, markAsRead 후 summary/detail 재조회에서도 unread가 복원되지 않는지 확인한다. shared 문서를 수정했다면 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`를 같은 작업에서 즉시 동기화한다.
8. 회원 라이프사이클 변경이면 탈퇴 후 접근 차단, 동일 UID 재가입 차단, 연관 도메인 정합성 회귀 확인
9. Serena Memory 동기화 확인
10. Admin 공통 변경이면 대표 Admin API에 대해 `401` / `403 ADMIN_REQUIRED` / 관리자 성공 시나리오를 확인한다.
11. 상태 변경 Admin API를 건드렸다면 `admin_audit_logs` row 생성과 `actor/target/diff` snapshot을 확인하고, `target_id`가 raw 입력이 아닌 canonical 키로 저장되는지 함께 확인한다.
12. Support Admin 목록 규약을 바꿨다면 `page/size` validation, `PageResponse`, 고정 정렬 문서 동기화를 함께 확인한다.

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
