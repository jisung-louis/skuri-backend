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

### App
- **Controller**: AppNoticeController, AppVersionController

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
