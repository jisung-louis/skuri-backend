# SKURI Backend - 프로젝트 개요

## 목적
성결대학교(Sungkyul University) 학생들을 위한 택시 합승 매칭 서비스 백엔드.
사용자는 택시 파티를 생성하거나 참여하고, 실시간 채팅과 정산 기능을 사용할 수 있다.

## 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.3
- **Build Tool**: Gradle (Groovy DSL)
- **Database**: MySQL 8.4 (production), H2 (test)
- **ORM**: Spring Data JPA + Hibernate
- **Authentication**: Firebase Admin SDK (ID Token 검증)
- **Real-time**: WebSocket (STOMP - 채팅), SSE (파티 이벤트)
- **API Documentation**: springdoc-openapi (Swagger UI + Scalar)
- **Testing**: JUnit 5, Spring Boot Test
- **CI/CD**: GitHub Actions

## 도메인 구조
```
domain/
├── member/       # 회원 (Firebase 인증, 프로필, 알림 설정, 은행 계좌)
├── taxiparty/    # 택시 파티 (핵심 도메인 - 생성/참여/정산/상태관리)
├── chat/         # 채팅 (WebSocket STOMP, 채팅방, 메시지)
├── board/        # 커뮤니티 게시판 (게시글/댓글/좋아요/북마크)
├── notice/       # 학교 공지 (RSS 수집, 상세 크롤링, 읽음/좋아요/댓글)
├── academic/     # 학사 정보 (강의 검색, 시간표, 학사 일정, 관리자 강의/학사일정 관리)
└── app/          # 앱 공지/버전 관리
```

## 인프라 구조
```
infra/
├── auth/         # Firebase 인증 + Spring Security
│   ├── config/   # SecurityConfig, FirebaseConfig
│   └── firebase/ # Token 검증, 인증 필터
└── openapi/      # OpenAPI 설정 및 도메인별 예시 상수
```

## 공통 모듈
```
common/
├── dto/          # ApiResponse<T>, PageResponse
├── config/       # JPA Auditing, ObjectMapper
├── entity/       # BaseTimeEntity
└── exception/    # ErrorCode, GlobalExceptionHandler, BusinessException
```

## 운영 정책
- NoticeScheduler: 평일 08:00~19:50 (Asia/Seoul), 10분 주기
- 성결대학교 공지 수집은 사이트 TLS 체인 이슈로 인해 Notice 전용 클라이언트에서만 인증서 검증을 비활성화
- Notice sync 응답은 `created/updated/skipped/failed`를 반환하고, 개별 공지 저장 실패가 나도 다음 항목을 계속 처리
- Notice 엔티티는 `rssPreview`(RSS 미리보기), `summary`(향후 AI 요약 예약), `bodyText`(정규화 plain text), `bodyHtml`(RN 렌더링용 원문 HTML), `attachments`로 구분한다.
- AppNotice 관리자 수정 API: `PATCH /v1/admin/app-notices/{appNoticeId}`
- AppNotice PATCH는 전달한 필드만 반영하고, 누락되거나 `null`인 필드는 유지
- 학사 일정 알림은 Phase 8 Notification 인프라에서 구현 예정이며, 기본 정책은 중요 일정(`isPrimary=true`) `startDate` 당일 오전 09:00 발송, 사용자 옵션은 전날 추가/모든 일정 확장이다.
- Phase 8 Notification 설계는 현행 RN + Firebase Cloud Functions 푸시 정책을 기본으로 이관하며, `allNotifications`/도메인 토글 반영이 불일치한 이벤트는 구현 시 정규화 여부를 명시한다.
- 댓글 알림 정책은 `commentNotifications`(Board/Notice 공통 댓글 알림)와 `bookmarkedPostCommentNotifications`(북마크 게시글 댓글 알림)로 분리된다.
- `COMMENT_CREATED`(게시글)은 게시글 작성자/부모 댓글 작성자/해당 게시글 북마크 사용자를 수신 대상으로 하며, 동일 사용자 중복 수신은 1회로 dedupe한다.
- Comment 도메인은 Board/Notice 공통 정책으로 운영하며, 무제한 depth 저장 + flat list 조회 + placeholder soft delete를 사용한다.
- Academic 도메인은 `Course`, `CourseSchedule`, `UserTimetable`, `UserTimetableCourse`, `AcademicSchedule` 엔티티로 구성된다.
- `Course`는 `semester + code + division` unique 제약을 가지며, 관리자 강의 bulk 등록 계약은 같은 키 기준 업서트 + 누락 강의 삭제 방식이다.
- 시간표는 `userId + semester` unique 규칙을 가지며, 같은 시간표 내 동일 강의 중복 추가와 시간 겹침(dayOfWeek/startPeriod/endPeriod overlap)을 서버에서 차단한다.
- `GET /v1/timetables/my`는 semester 미지정 시 현재 날짜 기준 `2~7월 -> yyyy-1`, `8~12월 -> yyyy-2`, `1월 -> 전년도 yyyy-2` 규칙으로 현재 학기를 해석한다.
- 실제 학교 학기 시작은 3월/9월이지만, 스쿠리는 수강신청/시간표 준비 수요를 반영해 한 달 앞선 2월/8월부터 새 학기 기준을 적용한다.
- 관리자 강의 bulk 등록 계약은 필드명 `credits`와 강의 단위 `location`으로 통일한다.
- 학사 일정 알림 구현 시 `NotificationSetting` 확장 후보는 `academicScheduleNotifications`, `academicScheduleDayBeforeEnabled`, `academicScheduleAllEventsEnabled`다.

## 이메일 도메인 제한
- `sungkyul.ac.kr` 도메인 이메일만 허용
