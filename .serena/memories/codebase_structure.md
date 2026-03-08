# 코드베이스 구조 상세

## 핵심 엔트리포인트
- `SkuriBackendApplication.java`

## 공통/인프라
- `common/dto`: `ApiResponse`, `PageResponse`
- `common/exception`: `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`
- `common/config`: JPA Auditing, ObjectMapper
- `common/event`: `AfterCommitApplicationEventPublisher`
- `infra/auth`: Firebase 인증/보안 설정
- `infra/openapi`: 도메인별 예시 상수, `OpenApiConfig` 그룹 설정
- `infra/notification`: `PushSender`, `FirebasePushSender`, `NoOpPushSender`

## 도메인별 주요 클래스
- member
  - Entity: `Member`, `NotificationSetting`, `LinkedAccount`, `BankAccount`
  - Service: `MemberService`, `NotificationSettingBackfillService`
  - Controller: `MemberController`
  - 학사 일정 알림 3개 필드는 nullable legacy row를 기본값으로 해석하고 startup backfill 한다 (`test` 프로필 제외)
- taxiparty
  - Entity: `Party`, `JoinRequest`, `MemberSettlement`, `PartyMember`, `PartyTag`
  - Service: `TaxiPartyService`, `PartyTimeoutCommandService`, `PartyTimeoutBatchService`, `PartySseService`, `JoinRequestSseService`
  - Controller: `PartyController`, `JoinRequestController`, `PartySseController`
  - 상태 변경 성공 지점에서 notification event publish 추가
- notification
  - Entity: `UserNotification`, `FcmToken`
  - Enum/Model: `NotificationType`, notification payload/data DTO
  - Repository: `UserNotificationRepository`, `FcmTokenRepository`
  - Service: `NotificationService`, `FcmTokenService`, `NotificationEventHandler`, `NotificationSseService`, `AcademicScheduleReminderScheduler`
  - Controller: `NotificationController`, `FcmTokenController`, `NotificationSseController`
  - Event: 도메인 이벤트를 수신해 inbox 저장, SSE fan-out, push delegation 수행
  - `NotificationEventHandler`는 broad `findAll()` 대신 repository-level recipient query로 파티/공지/시스템/학사 일정 수신 대상을 1차 축소한다.
- `NotificationService`는 after-commit 콜백에서 다시 count query를 치지 않고, 트랜잭션 안에서 unread count를 미리 계산해 SSE에 전달한다.
- `FcmTokenService.register()`는 unique token 충돌 시 재조회로 복구해 멱등 등록을 보장한다.
- academic
  - Entity: `AcademicSchedule`, `Course`, `CourseSchedule`, `UserTimetable`, `UserTimetableCourse`
  - Scheduler/Notification과 연계되어 학사 일정 리마인더를 발행
- board / notice / app / chat / support
  - 기존 구조 유지, 필요한 알림 이벤트만 최소 침습적으로 연결

## 테스트 구조
- Contract 테스트: controller별 MockMvc
- Service 테스트: 권한/상태전이/알림 dedupe/unread count/recipient resolution
- Event 테스트: event publish -> listener -> inbox 저장 / push delegation
- Security 회귀: notifications, fcm-tokens, notification SSE 인증 필수 검증
- 플래키 DataJpa 테스트는 auditing listener 경로를 피하기 위해 조회 전용 fixture를 native insert로 시드한다.

## 주요 저장 모델
- `user_notifications`: 인앱 인박스 + 읽음/삭제 관리
- `fcm_tokens`: 사용자별 멀티 디바이스 토큰 저장, invalid token cleanup 대상
- `members.notification_setting`: 알림 토글 집합
