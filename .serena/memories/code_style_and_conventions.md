# 코드 스타일 & 컨벤션

## 아키텍처 패턴
- **Layered Architecture**: Controller → Service → Repository
- Controller: 요청 검증 + 응답 변환만 담당
- Service: 비즈니스 규칙 판단 (상태 전이, 권한, 정산)
- Entity: JPA 엔티티, 도메인 로직은 서버에서 최종 판단

## 응답 포맷
- 모든 API는 `ApiResponse<T>` 래퍼를 사용
  - 성공: `ApiResponse.success(data)` → `{success: true, data: ...}`
  - 실패: `ApiResponse.error(errorCode, message)` → `{success: false, errorCode: ..., message: ..., timestamp: ...}`

## 예외 처리
- `BusinessException` (커스텀 런타임 예외) + `ErrorCode` enum
- `GlobalExceptionHandler`에서 일관 처리
- 각 도메인별 커스텀 예외: `PartyNotFoundException`, `MemberNotFoundException` 등

## 네이밍 컨벤션
- 패키지: 도메인별 분리 (member, taxiparty, chat, board, app)
- DTO: `XxxRequest`, `XxxResponse` (도메인별 dto/request, dto/response 하위)
- Controller: `XxxController` (REST), `XxxStompController` (WebSocket)
- Service: `XxxService`
- Repository: `XxxRepository` (Spring Data JPA)
- Entity: 도메인 명사 (Party, Member, ChatRoom 등)

## Lombok 사용
- `@Getter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` 등 활용
- `compileOnly 'org.projectlombok:lombok'` + `annotationProcessor`

## OpenAPI 문서화
- Controller: `@Tag` + `@Operation` + `@ApiResponses`
- DTO: `@Schema(description, example, nullable)`
- 도메인별 예시 상수 클래스: `OpenApiXxxExamples`
- 모든 responseCode에 content + examples 필수
- REST 계약의 런타임 기준은 `/v3/api-docs`이며, `docs/api-specification.md`는 같은 PR에서 동기화

## WebSocket 보안 규칙
- CONNECT 인증 + SEND/SUBSCRIBE 목적지별 인가를 모두 적용
- 비공개 채팅방(PARTY 포함)은 멤버만 `/app/chat/{chatRoomId}`, `/topic/chat/{chatRoomId}` 접근 가능
- STOMP 에러는 `/user/queue/errors`로 `errorCode/message/timestamp` 형태를 우선 사용
- CORS는 `*`를 사용하지 않고 프로필/환경별 허용 Origin만 설정

## 커밋 메시지
- Conventional Commits: 타입은 영어, 본문은 한국어
- 예: `feat: 택시 파티 생성 API 구현`

## 브랜치 전략
- `main`: 항상 안정 상태
- `feat/<topic>`, `fix/<topic>`, `docs/<topic>`, `chore/<topic>`
- Squash merge 사용
