# 코드 스타일 & 컨벤션

## 아키텍처
- Controller는 요청 검증과 응답 변환만 담당한다.
- Service가 비즈니스 규칙, 상태 전이, 권한 판단을 담당한다.
- 부가효과(알림, SSE fan-out, push)는 핵심 트랜잭션과 분리한다.
- 상태 변경 후 알림 발행은 `AfterCommitApplicationEventPublisher`로 after-commit semantics를 보장한다.

## 응답/예외
- 모든 REST 응답은 `ApiResponse<T>`를 사용한다.
- 예외는 `BusinessException + ErrorCode`로 표현하고 `GlobalExceptionHandler`에서 일관 처리한다.
- Notification 전용 최소 ErrorCode는 `NOTIFICATION_NOT_FOUND`, `NOT_NOTIFICATION_OWNER`를 사용한다.

## Notification 구현 규칙
- canonical notification type 명칭은 `PARTY_*` 계열을 포함한 API 계약 기준 enum을 따른다.
- `allNotifications`는 마스터 토글이며, 문서화된 예외를 제외한 알림에 공통 적용한다.
- FCM 연동은 `PushSender` 인터페이스 뒤로 숨기고, Firebase credentials가 없으면 no-op fallback으로 기동 가능해야 한다.
- FCM token 등록은 unique 충돌이 나더라도 재조회/복구로 멱등하게 처리한다.
- inbox 저장, unread count 갱신, SSE fan-out, push 전송은 서로 결합하지 말고 실패 허용으로 다룬다.
- 댓글/복수 수신 조건은 recipient dedupe를 적용해 push/inbox를 1회만 생성한다.
- 공개 채팅 push는 inbox를 남기지 않는다.
- 학사 일정 날짜 계산은 scheduler cron zone과 동일한 `Asia/Seoul` 기준으로 맞춘다.

## OpenAPI/문서화
- Controller에 `@Tag`, `@Operation`, `@ApiResponses`를 작성한다.
- 모든 responseCode에 `content + examples`를 제공한다.
- Notification 예시는 `OpenApiNotificationExamples`로 분리한다.
- SSE 200 응답은 `stream_full`과 이벤트별 예시(`SNAPSHOT`, `NOTIFICATION`, `UNREAD_COUNT_CHANGED`, `HEARTBEAT`)를 함께 둔다.
- 문서 기준은 `/v3/api-docs`이며 `docs/api-specification.md`와 같은 PR에서 동기화한다.

## Git/리뷰
- 브랜치는 목적 단위로 유지한다.
- 커밋은 Conventional Commits, 타입은 영어, 메시지는 한국어로 작성한다.
- 런타임 코드/테스트/문서는 가능한 한 분리 커밋한다.
