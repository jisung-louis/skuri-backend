# SKURI Backend - 프로젝트 개요

## 목적
성결대학교 학생 대상 택시 합승/커뮤니티/공지/학사 서비스 백엔드다. 핵심 도메인은 TaxiParty이며, 알림은 핵심 트랜잭션의 부가효과로 분리한다.

## 기술 스택
- Java 21, Spring Boot 4.0.3, Gradle
- MySQL 8.4 (prod), H2 (test)
- Spring Data JPA + Hibernate
- Firebase Admin SDK (ID Token 검증, FCM sender 구현)
- 실시간: WebSocket(STOMP, 채팅), SSE(파티/알림)
- OpenAPI: springdoc Swagger UI + Scalar

## 도메인 구조
- member: 회원, 프로필, 알림 설정, FCM 토큰 API
- taxiparty: 생성/참여/정산/상태 전이, SSE
- chat: 채팅방/메시지/WebSocket
- board: 게시글/댓글/북마크
- notice: 학교 공지 수집/상세/댓글/읽음
- academic: 강의/시간표/학사 일정
- app: 앱 공지
- support: 문의/신고/버전/학식
- notification: 인앱 인박스, FCM 토큰, SSE, 리마인더 스케줄링, 이벤트 기반 알림 처리

## 인프라/공통
- Phase 9 기준 실행/배포는 `application`, `local`, `local-emulator`, `prod`, `test` 5개 정책 파일 + `.env` 중심으로 정리되었다. 로컬 기본 실행은 `local` 프로필 + `docker compose`(`app + MySQL + Redis`), 운영은 `prod` 프로필 + `OCI 단일 인스턴스에서 docker-compose.prod.yml(app + MySQL + Redis)` 구조를 사용한다.
- OpenAPI는 `local/local-emulator`에서 노출하고 `prod`에서는 기본 비노출로 운영한다.
- GitHub Actions CD는 `production` 환경 승인 기반 반자동 배포 초안을 사용하며, `linux/amd64`와 `linux/arm64` 멀티플랫폼 이미지를 빌드한다.
- `common.event.AfterCommitApplicationEventPublisher`: 성공한 상태 변경 이후에만 `ApplicationEvent`를 발행한다.
- `infra.auth.config.FirebaseConfig`를 재사용하고 Firebase Admin 중복 초기화는 금지한다.
- `infra.notification`에는 `PushSender` 추상화, Firebase 기반 sender, credentials 부재 시 `NoOpPushSender`가 있다.
- 공통 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`, ErrorCode 중심으로 처리한다.

## 운영 정책
- 알림은 after-commit 이후 처리하고, 실패해도 핵심 트랜잭션을 롤백하지 않는다.
- Notification 저장소 모델은 Firestore가 아니라 RDB(`user_notifications`, `fcm_tokens`)를 사용한다.
- Notification canonical enum은 API 계약 기준으로 `PARTY_JOIN_REQUEST`, `PARTY_JOIN_ACCEPTED`, `PARTY_JOIN_DECLINED`를 사용한다.
- `allNotifications`는 마스터 토글이며, 문서화된 예외(`AppNoticePriority.HIGH`, 파티 채팅)를 제외한 알림에 공통 적용한다.
- Notification SSE는 `/v1/sse/notifications`에서 `SNAPSHOT`, `NOTIFICATION`, `UNREAD_COUNT_CHANGED`, `HEARTBEAT` 이벤트를 발행한다.
- 학사 일정 리마인더는 `Asia/Seoul` 기준 오전 09:00, 기본 대상 `isPrimary=true`, 멀티데이는 `startDate` 기준이다.
- 기본 알림 설정은 `academicScheduleNotifications=true`, `academicScheduleDayBeforeEnabled=true`, `academicScheduleAllEventsEnabled=false`다.
- 기존 회원의 학사 일정 알림 nullable 필드는 런타임 기본값으로 해석하고, 애플리케이션 시작 시 backfill 한다. 단, `test` 프로필에서는 비활성화한다.
- 댓글 알림은 게시글 작성자/부모 댓글 작성자/북마크 사용자를 dedupe해서 1회만 생성한다.
- `PARTY_CREATED`, `PARTY_CLOSED`는 inbox를 남기지 않는 parity를 유지한다.
- 파티 채팅 푸시는 mute parity를 우선 반영하고, 전역 토글은 현재 런타임에서 적용하지 않는다.
- 공지 댓글 알림은 `Notice.author`가 문자열이라 원글 작성자 알림은 미지원이며 부모 댓글 작성자 reply 알림만 지원한다.
- AppNotice 강제 발송은 문서의 legacy `urgent` 대신 런타임 `AppNoticePriority.HIGH`를 기준으로 본다.
- FCM 토큰 등록은 재시도/동시 등록 상황에서도 멱등적으로 처리한다.
- FCM raw push payload는 특정 RN legacy type에 종속되지 않고 canonical `NotificationType` + 리소스 식별자 + `contractVersion`을 사용한다.
- 플랫폼별 알림 표현은 `PushPresentationProfile`(`PARTY`, `CHAT`, `NOTICE`, `DEFAULT`)로 분리하며, Android는 channel/sound override, iOS는 `aps.sound`를 사용한다.

## 이메일/인증
- Firebase ID Token 기반 인증
- `sungkyul.ac.kr` 도메인 이메일만 허용
