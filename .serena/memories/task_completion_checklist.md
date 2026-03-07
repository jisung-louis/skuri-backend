# 작업 완료 시 체크리스트

## 머지 전 필수 검증
1. `./gradlew build` 성공 확인
2. 변경된 기능 관련 테스트 수행 확인
3. API 변경 시 정상/예외 케이스 최소 1개 확인
4. `ApiResponse` 포맷 일관성 확인
5. OpenAPI 예시가 실제 응답과 일치하는지 확인
6. Serena Memory(`.serena/memories/*.md`) 동기화 확인
7. WebSocket 변경 시 CONNECT 인증 + SEND/SUBSCRIBE 인가 + `/user/queue/errors` 에러 포맷 동작 확인

## 테스트 작성 규칙
- 신규/수정 API: Contract 테스트 (정상 1개 + 인증/권한 1개 + 예외 1개)
- 상태 전이/권한/정산 변경: Service 테스트 (성공 1개 + 실패 1개)
- Academic 변경 시 추가 확인: 강의 필터 조합, 시간표 동일 강의 중복 차단, 시간 충돌 차단, 관리자 Academic API 403, 학기 강의 bulk 업서트/삭제 검증
- Board 변경 시 추가 확인: 댓글 depth 1 제한, 부모 placeholder soft delete, 익명 순번, 좋아요/북마크 카운트 동기화
- 응답 스키마 변경: 필드 존재/미노출 검증 추가

## 문서 동기화
변경 사항에 따라 함께 수정해야 하는 문서:
- API 계약 변경 → `docs/api-specification.md`
- 엔티티/관계/인덱스 변경 → `docs/erd.md`
- 도메인 책임/경계 변경 → `docs/domain-analysis.md`, `docs/role-definition.md`
- 구현 계획 변경 → `docs/implementation-roadmap.md`
- ErrorCode 변경 → OpenAPI 예시 상수 + Controller @ApiResponses

## OpenAPI 동기화
- 새 도메인 API 추가 시 `GroupedOpenApi` 그룹 설정 갱신
- `BusinessException` 커스텀 메시지와 OpenAPI 예시 메시지 일치시키기
- Scalar에서 최소 1개 API의 200/4xx 탭 예시 확인
