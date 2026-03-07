# 코드베이스 구조 상세

## 핵심 엔트리포인트
- `SkuriBackendApplication.java` - Spring Boot main class

## 도메인별 주요 클래스

### TaxiParty (핵심 도메인)
- **Entity**: Party, PartyMember, JoinRequest, MemberSettlement, Location, PartyTag
- **Enum**: PartyStatus, JoinRequestStatus, SettlementStatus, PartyEndReason
- **Service**: TaxiPartyService (생성/조회/수정/상태전이/정산 핵심 로직 담당)
- **Controller**: PartyController, JoinRequestController, PartySseController
- **기타 Service**: PartySseService, JoinRequestSseService, PartyTimeoutBatchService, PartyTimeoutCommandService
- **Scheduler**: PartyTimeoutScheduler, PartySseHeartbeatScheduler

### Member
- **Entity**: Member, LinkedAccount, BankAccount, NotificationSetting
- **Service**: MemberService
- **Controller**: MemberController

### Chat
- **Entity**: ChatRoom, ChatRoomMember, ChatMessage, ChatAccountData, ChatArrivalData
- **Enum**: ChatRoomType, ChatMessageType, ChatMessageDirection
- **Service**: ChatService, PartyMessageService
- **Controller**: ChatRoomController (REST), ChatStompController (WebSocket)
- **WebSocket**: ChatWebSocketConfig, FirebaseStompAuthChannelInterceptor

### Board
- **Entity**: Post, PostImage, Comment, PostInteraction
- **Enum**: PostCategory
- **Service**: BoardService
- **Controller**: PostController, CommentController, MemberBoardController
- **댓글 정책**: Board/Notice 공통으로 무제한 depth 댓글을 저장하고, 조회 API는 `parentId`와 `depth`를 포함한 flat list를 반환한다.

### Notice
- **Entity**: Notice, NoticeComment, NoticeReadStatus, NoticeLike
- **Service**: NoticeService, NoticeSyncService
- **Controller**: NoticeController, NoticeCommentController, NoticeAdminController
- **외부 연동**: SungkyulNoticeRssClient, SungkyulNoticeDetailCrawler, SungkyulNoticeTlsSupport
- **필드 의미**: Notice는 `rssPreview`를 목록/상세 API에 노출하고, `summary`는 향후 AI 요약용 예약 필드로 유지한다. `bodyHtml`은 RN 렌더링용 원문 HTML, `bodyText`는 검색/AI/RAG용 정규화 plain text다.
- **운영 메모**: 성결대학교 사이트 TLS 체인 이슈로 인해 Notice 크롤링 경로에서만 trust-all SSL socket factory 사용
- **동기화 정책**: `body_html`은 LONGTEXT로 저장하고, 개별 공지 저장 실패는 `failed` 집계 후 다음 공지를 계속 처리
- **Scheduler**: NoticeScheduler (평일 08:00~19:50, Asia/Seoul, 10분 주기)

### Academic
- **Entity**: Course, CourseSchedule, UserTimetable, UserTimetableCourse, AcademicSchedule
- **Service**: CourseService, TimetableService, AcademicScheduleService
- **Controller**: CourseController, TimetableController, AcademicScheduleController, AcademicScheduleAdminController, CourseAdminController
- **Repository**: CourseRepository, CourseScheduleRepository, UserTimetableRepository, UserTimetableCourseRepository, AcademicScheduleRepository
- **운영 정책**: `Course`는 `semester + code + division` unique, 시간표는 `user_id + semester` unique이며 동일 강의 중복 추가 및 시간 충돌을 차단한다.
- **응답 정책**: 시간표 응답은 `courses[] + slots[]` 구조를 반환하고, `GET /v1/timetables/my`는 semester 미지정 시 `2~7월 -> yyyy-1`, `8~12월 -> yyyy-2`, `1월 -> 전년도 yyyy-2` 규칙을 사용한다.
- **학기 기준 배경**: 실제 학교 학기 시작은 3월/9월이지만, 수강신청/시간표 준비 수요를 반영해 스쿠리는 한 달 앞선 2월/8월부터 새 학기 기준을 적용한다.

### App
- **Entity**: AppNotice
- **Service**: AppNoticeService
- **Controller**: AppNoticeController, AppNoticeAdminController, AppVersionController
- **관리 정책**: AppNotice 관리자 수정은 `PATCH /v1/admin/app-notices/{appNoticeId}` partial update 계약 사용

### Support
- **Entity**: Inquiry, Report, AppVersion, CafeteriaMenu
- **Service**: InquiryService, ReportService, AppVersionService, CafeteriaMenuService
- **Controller**: InquiryController, ReportController, CafeteriaMenuController, InquiryAdminController, ReportAdminController, AppVersionAdminController, CafeteriaMenuAdminController
- **운영 정책**: `GET /v1/app-versions/{platform}`는 공개 API이고, Support Admin API는 모두 `ROLE_ADMIN` + `403 ADMIN_REQUIRED` 정책을 따른다.
- **enum 기준**: Report는 `targetType=POST|COMMENT|MEMBER`, `status=PENDING|REVIEWING|ACTIONED|REJECTED`를 사용한다.

## 인증 흐름
1. Firebase ID Token → FirebaseAuthenticationFilter
2. Token 검증 → FirebaseTokenVerifier (prod) / DisabledFirebaseTokenVerifier (test)
3. 이메일 도메인 검증 → sungkyul.ac.kr만 허용
4. SecurityConfig → 인증 필요 경로 설정

## 테스트 구조
- test 프로필: H2 인메모리 DB (application-test.yaml)
- Contract 테스트: Controller 단위 (MockMvc)
- Service 테스트: 비즈니스 로직 단위
- Integration 테스트: ChatWebSocketIntegrationTest

## CI
- GitHub Actions: PR → main 시 `./gradlew clean build`
- MySQL 8.4 서비스 컨테이너 사용
