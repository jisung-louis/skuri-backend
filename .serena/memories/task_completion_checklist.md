# 작업 완료 시 체크리스트

## 머지 전 필수 검증
1. `./gradlew build` 성공
2. 변경된 기능 관련 Contract/Service/Event/Security 테스트 수행
3. API 정상/예외 케이스 최소 1개 이상 확인
4. `ApiResponse` 에러 포맷 일관성 확인
5. OpenAPI example과 실제 `errorCode/message` 일치 확인
6. Serena Memory 동기화 확인

## Notification 변경 시 추가 검증
1. `/v1/notifications` pagination/query/response 계약 확인
2. `/v1/notifications/unread-count`, 단건 읽음, 전체 읽음, 삭제 ownership 확인
3. `/v1/members/me/fcm-tokens` 등록/해제 계약과 인증 필수 확인
4. `/v1/sse/notifications` 인증 필수 및 이벤트 예시 확인
5. recipient dedupe, unread count 갱신, invalid token cleanup 정책 테스트 확인
6. 학사 일정 리마인더 기본값과 09:00 Asia/Seoul 스케줄 확인
7. Firebase credentials가 없어도 앱 기동/테스트가 되는지 no-op fallback 확인

## 운영/배포 변경 시 추가 검증
1. `./gradlew build` 성공
2. `docker compose` 설정 파일 문법/기동 절차 확인
3. `/actuator/health` 응답 확인
4. `docker-compose.prod.yml` 렌더링과 MySQL/Redis 영속 볼륨 정책 확인
5. `prod`에서 OpenAPI가 기본 비노출인지 확인
6. 브라우저 관리자 페이지가 있으면 허용 Origin의 REST CORS preflight와 WebSocket Origin 설정을 함께 확인
7. 배포 전/후 체크리스트와 rollback 문서 동기화 확인

## 문서 동기화
- API 계약 변경 -> `docs/api-specification.md`
- 엔티티/관계/인덱스 변경 -> `docs/erd.md`
- 도메인 책임/경계 변경 -> `docs/domain-analysis.md`, `docs/role-definition.md`
- 구현 계획 변경 -> `docs/implementation-roadmap.md`
- 운영 정책 변경 -> `docs/project-overview.md`
- ErrorCode/예외 규칙 변경 -> OpenAPI example 상수와 controller response 동기화

## OpenAPI
- 새 도메인 API 추가 시 `GroupedOpenApi` 그룹 설정 갱신
- SSE는 `stream_full` + 이벤트별 예시 제공
- Scalar에서 200/4xx example이 분리 노출되는지 확인
