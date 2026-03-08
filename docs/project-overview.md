# SKURI Taxi — 프로젝트 종합 문서

> 운영 규칙: 본 문서의 제품/아키텍처 내용이 변경되면 Serena Memory `project_overview`, `codebase_structure`도 함께 갱신한다.

---

## 1. 프로젝트 소개

**SKURI Taxi**는 성결대학교(Sungkyul University) 학생을 위한 **택시 동승 + 캠퍼스 라이프 통합 모바일 앱**입니다. 앱 이름 "SKURI"는 Sungkyul University의 약자에서 왔습니다.

### 배경 및 목적

대학교 특성상 같은 방향으로 이동하는 학생이 많지만 택시를 혼자 타는 경우가 많습니다. SKURI Taxi는 동승자를 빠르게 모집하고 채팅과 정산 관리(상태 확인/확정)까지 앱 안에서 해결할 수 있게 합니다. 여기에 학교 공지, 커뮤니티 게시판, 시간표·학식 등의 생활 정보를 통합하여 성결대 학생의 캠퍼스 생활 플랫폼 역할을 합니다.

### 주요 기능 5가지


| #   | 기능           | 설명                            |
| --- | ------------ | ----------------------------- |
| 1   | **택시 동승**    | 파티 생성/참여, 실시간 채팅, 정산          |
| 2   | **학교 공지**    | 크롤링된 학교 공지 열람, 학과별 필터, 댓글     |
| 3   | **커뮤니티 게시판** | 익명/실명 게시글, 좋아요·북마크·댓글, 이미지    |
| 4   | **채팅**       | 학교/학과/게임 공개 채팅방, 마인크래프트 서버 연동 |
| 5   | **학교 생활 정보** | 시간표, 학식 메뉴, 학사 일정             |


### 대상 사용자

`@sungkyul.ac.kr` 이메일 계정을 보유한 성결대학교 재학생 (가입 시 학교 도메인 검증)

### 현재 버전

`v1.2.6` — iOS / Android 동시 지원

---

## 2. 기술 스택

### 프론트엔드


| 항목           | 버전/내용                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------ |
| React Native | 0.79.2                                                                                           |
| React        | 19.0.0                                                                                           |
| TypeScript   | 5.0.4                                                                                            |
| 네비게이션        | `@react-navigation/native` v7, `@react-navigation/bottom-tabs`, `@react-navigation/native-stack` |
| 지도           | `react-native-maps` v1.26 (iOS: MapKit, Android: Google Maps)                                    |
| 위치           | `@react-native-community/geolocation`                                                            |
| 애니메이션        | `react-native-reanimated` v3, `react-native-gesture-handler`                                     |
| Bottom Sheet | `@gorhom/bottom-sheet` v5                                                                        |
| 아이콘          | `react-native-vector-icons`                                                                      |
| HTML 렌더링     | `react-native-render-html`                                                                       |
| 이미지          | `react-native-image-picker`, `@bam.tech/react-native-image-resizer`                              |
| WebView      | `react-native-webview` (공지 상세, 마인크래프트 맵, 계정 안내)                                                  |
| 드래그 리스트      | `react-native-draggable-flatlist` (이미지 선택기 순서 변경)                                                |
| 기타 UI        | `react-native-modal`, `react-native-linear-gradient`, `react-native-svg`                         |


### 백엔드 (Firebase)


| Firebase 제품                  | 용도                                                              |
| ---------------------------- | --------------------------------------------------------------- |
| **Firebase Auth**            | Google OAuth (주요) + 이메일/비밀번호 (관리자 전용), `@sungkyul.ac.kr` 도메인 제한 |
| **Firestore**                | 실시간 데이터베이스 (파티, 게시판, 채팅 등 모든 데이터)                               |
| **Cloud Functions**          | 서버 트리거, FCM 알림 발송, 데이터 일관성                                      |
| **Firebase Messaging (FCM)** | 푸시 알림                                                           |
| **Firebase Storage**         | 게시판 이미지 파일 저장                                                   |
| **Firebase Analytics**       | 사용자 행동 분석                                                       |
| **Firebase Crashlytics**     | 런타임 오류 수집                                                       |
| **Realtime Database**        | 마인크래프트 채팅 연동 (별도 채널)                                            |


### 주요 유틸리티 라이브러리


| 라이브러리                                       | 용도                  |
| ------------------------------------------- | ------------------- |
| `axios`                                     | HTTP 요청 (공지 크롤링 등)  |
| `cheerio`, `htmlparser2`                    | HTML 파싱 (학교 공지 크롤링) |
| `rss-parser`                                | RSS 피드 파싱           |
| `date-fns`                                  | 날짜 처리               |
| `react-native-device-info`                  | 디바이스 정보             |
| `react-native-sound`                        | 알림음 재생              |
| `@react-native-google-signin/google-signin` | Google OAuth        |


---

## 3. 앱 아키텍처

### 3.1 네비게이션 구조

```
RootNavigator (Auth 상태 라우팅)
├── AuthNavigator (미로그인)
│   ├── LoginScreen
│   └── AccountGuideScreen
│
├── CompleteProfileScreen (프로필 미완성 시)
├── TermsOfUseForAuthScreen (약관 보기, CompleteProfile에서 진입)
├── PermissionOnboardingScreen (권한 온보딩 미완료 시)
│
└── MainNavigator (로그인 + 프로필 + 권한 완료)
    └── Bottom Tab Navigator
        ├── HomeTab
        │   ├── HomeScreen (메인)
        │   ├── NotificationScreen
        │   ├── ProfileScreen
        │   ├── SettingScreen
        │   ├── TimetableDetailScreen
        │   ├── CafeteriaDetailScreen
        │   ├── AcademicCalendarDetailScreen
        │   ├── MinecraftDetailScreen
        │   └── MinecraftMapDetailScreen
        │
        ├── TaxiTab
        │   ├── TaxiScreen (지도 + 파티 목록)
        │   ├── RecruitScreen (파티 생성)
        │   ├── AcceptancePendingScreen (동승 대기)
        │   ├── MapSearchScreen (위치 검색)
        │   └── ChatScreen (파티 채팅)
        │
        ├── NoticeTab
        │   ├── NoticeScreen (공지 목록)
        │   ├── NoticeDetailScreen
        │   └── NoticeDetailWebViewScreen
        │
        ├── BoardTab
        │   ├── BoardScreen (게시글 목록)
        │   ├── BoardDetailScreen
        │   ├── BoardWriteScreen
        │   └── BoardEditScreen
        │
        └── ChatTab
            ├── ChatListScreen (채팅방 목록)
            └── ChatDetailScreen (채팅방 내부)
```

> 탭 바는 각 탭의 최상위 화면에서만 표시되고, 하위 Stack 화면에서는 자동으로 숨겨집니다.

### 3.2 폴더 구조

```
src/
├── screens/          화면 컴포넌트 (탭별 폴더)
├── components/       재사용 UI 컴포넌트
│   ├── common/       버튼, 모달 등 공통
│   ├── home/         홈 탭 전용
│   ├── board/        게시판 전용
│   ├── academic/     학사 정보
│   ├── cafeteria/    학식 메뉴
│   ├── htmlRender/   HTML 렌더링 유틸
│   └── timetable/    시간표 전용
├── hooks/            Firestore 구독 훅 (~60개)
│   ├── auth/         인증 관련
│   ├── board/        게시판 데이터
│   ├── chat/         채팅 메시지
│   ├── common/       공용 훅 (권한, 위치 등)
│   ├── notice/       공지 데이터
│   ├── party/        택시 파티
│   ├── setting/      설정 관련
│   ├── storage/      Firebase Storage
│   ├── timetable/    시간표 데이터
│   └── user/         사용자 프로필
├── contexts/         전역 상태
│   ├── AuthContext.tsx
│   ├── JoinRequestContext.tsx
│   └── CourseSearchContext.tsx
├── lib/              외부 서비스 연동
│   ├── analytics.ts
│   ├── att.ts          ATT(앱 추적 투명성) 권한
│   ├── fcm.ts
│   ├── minecraftChat.ts  마인크래프트 채팅 브릿지
│   ├── moderation.ts   콘텐츠 검수
│   ├── notifications.ts
│   ├── noticeViews.ts  공지 조회수 처리
│   ├── versionCheck.ts
│   ├── minecraft/      MC 계정 관리 (UUID 조회, 등록)
│   └── sound/          알림음 파일 및 재생
├── utils/            유틸리티 함수 (날짜, 채팅, 정산)
├── config/           Firebase / Google Sign-In 초기화
├── constants/        디자인 토큰 (COLORS, TYPOGRAPHY)
└── types/            TypeScript 타입 정의

functions/            Firebase Cloud Functions (Node 22)
scripts/              Firestore 관리용 CLI 스크립트
docs/                 명세서, 가이드, 법적 문서
```

### 3.3 상태 관리 전략


| 레이어      | 도구                                                     | 용도                      |
| -------- | ------------------------------------------------------ | ----------------------- |
| 서버 상태    | Firestore 실시간 구독 훅 (`useParties`, `useChatMessages` 등) | 파티, 채팅, 게시글 등 모든 서버 데이터 |
| 전역 상태    | React Context (`AuthContext`, `JoinRequestContext`)    | 로그인 사용자 정보, 동승 요청 상태    |
| 로컬 UI 상태 | `useState` / `useReducer`                              | 폼 입력, 모달 표시 등 화면 내 상태   |


**Firestore가 단일 진실 공급원(Single Source of Truth)입니다.** 클라이언트는 Firestore 구독을 통해 실시간으로 데이터를 받고, 쓰기 작업도 Firestore를 통해 진행합니다. Cloud Functions는 트리거 방식으로 부가 처리(FCM 발송, 데이터 정리)를 담당합니다.

---

## 4. 화면 및 기능 상세

### 4.1 인증

**관련 파일:** `src/screens/auth/`

**가입/로그인 흐름:**

```
LoginScreen
  → "성결대 이메일로 로그인하기" 버튼 (Google OAuth)
  → Google 계정 선택 → Firebase Auth 검증
  → 이메일 도메인 @sungkyul.ac.kr 확인 (학교 계정만 허용)
  → 신규 가입 시 CompleteProfileScreen (닉네임, 학번, 학과 입력)
     └── 약관 보기 링크 → TermsOfUseForAuthScreen (선택적 열람)
  → PermissionOnboardingScreen (알림/ATT/위치 권한 온보딩)
  → MainNavigator 진입
```

- **일반 사용자**: Google OAuth 로그인만 사용 (`@react-native-google-signin/google-signin`)
- **관리자**: 화면 우하단 숨겨진 "관리자" 버튼 → 이메일/비밀번호 로그인 모달
- 성결대 이메일이 없는 사용자를 위한 안내 화면 (`AccountGuideScreen`) 연결
- 프로필 미완성 상태라면 MainNavigator 진입 후에도 완성 요구

**사용자 프로필 구성:**

- 기본 정보: 닉네임, 학번, 학과, 프로필 이미지
- 계좌 정보 (택시비 정산용): 은행명, 계좌번호, 예금주
- 마인크래프트 계정 연동 (선택)
- 알림 설정 (카테고리별 on/off)

---

### 4.2 홈 탭

**관련 파일:** `src/screens/HomeTab/HomeScreen.tsx`

홈은 **앱의 대시보드** 역할로, 모든 탭의 핵심 정보를 한눈에 보여줍니다.


| 섹션                      | 내용                                            |
| ----------------------- | --------------------------------------------- |
| TaxiSection             | 현재 모집 중인 파티를 가로 스크롤로 표시 → "모두 보기" 클릭 시 택시 탭으로 |
| NoticeSection           | 학교 전체 공지 / 내 학과 공지 전환 → 공지 탭으로 연결             |
| TimetableSection        | 오늘의 강의 목록                                     |
| AcademicCalendarSection | 이번 주/다음 학사 일정 미리보기                            |
| MinecraftSection        | 마인크래프트 서버 상태 및 접속자 정보                         |
| CafeteriaSection        | 오늘의 교내 식당 메뉴                                  |


- 헤더에 알림 아이콘 배지 (미읽음 수) → NotificationScreen으로 이동

홈에서 직접 세부 화면으로 이동 가능: `TimetableDetailScreen`, `CafeteriaDetailScreen`, `AcademicCalendarDetailScreen`, `MinecraftDetailScreen`, `MinecraftMapDetailScreen`

---

### 4.3 택시 동승 탭 (핵심 기능)

**관련 파일:** `src/screens/TaxiTab/`

SKURI의 핵심 기능으로, **파티 생성 → 동승 요청 → 채팅 → 정산**까지 완결된 UX를 제공합니다.

#### 파티 목록 (TaxiScreen)

- 하단 Bottom Sheet에 모집 중(`status: open`) 파티 카드 목록
- 지도(`react-native-maps`)에 출발지/도착지 핀 표시
- 카드 선택 시 지도 포커스 이동 + 상세 정보 패널 표시
- 우측 하단 `+` 버튼으로 파티 생성

#### 파티 생성 (RecruitScreen)


| 입력 항목 | 설명                                  |
| ----- | ----------------------------------- |
| 출발지   | 사전 정의 옵션 또는 지도 검색 (MapSearchScreen) |
| 도착지   | 동일                                  |
| 출발 시간 | 날짜·시간 선택                            |
| 최대 인원 | 2~7명                                |
| 태그    | 키워드 태그 (예: `#여성전용`, `#빠른출발`)        |
| 상세 내용 | 자유 텍스트                              |


#### 동승 요청 흐름

```
카드에서 "동승 요청" 클릭
  → AcceptancePendingScreen (수락 대기)
  → 파티 리더가 수락/거절
  → 수락 시: 파티 멤버 추가, 채팅방 자동 입장, FCM 알림
  → 정원 충족 시: 파티 status → 'closed'
```

#### 파티 채팅 (ChatScreen)

- 파티 멤버 간 전용 채팅 (`chats/{partyId}/messages`)
- 메시지 타입: `user`(일반), `system`(입퇴장), `account`(계좌 공유), `arrived`(도착 정산), `end`(파티 종료)
- **계좌 공유**: 프로필의 계좌 정보를 채팅에 직접 전송 가능

#### 정산 기능

- 도착 처리 시 택시비 입력 → 인원수로 자동 나눔
- `arrivalData` 메시지로 각 멤버의 정산 금액 + 계좌 정보 표시
- `parties/{partyId}.settlement` 필드에 정산 상태 저장
- 앱 내 결제/송금 기능은 제공하지 않으며, 향후에도 제공하지 않음
- 따라서 정산 상태 변경(정산 완료 확정)은 파티 리더만 수행

---

### 4.4 공지 탭

**관련 파일:** `src/screens/NoticeTab/`

성결대학교 공식 공지를 **크롤링하여 앱 안에서 열람**할 수 있게 합니다.

- 공지 목록: 카테고리(학사/장학/취업 등), 학과별 필터
- 상세 보기: HTML 콘텐츠 렌더링 (`react-native-render-html`) 또는 WebView
- 첨부파일 링크 제공
- 댓글/대댓글 기능 (`noticeComments` 컬렉션)
- 읽음 상태 표시 (`notices/{noticeId}/readBy/{uid}`)
- **운영 공지** (`appNotices`): 업데이트, 서비스 점검 등 앱 내부 공지 카드
- 백엔드는 원문 HTML(`bodyHtml`)을 유지해 RN 앱이 웹 구조(`h*`, `table`, `br`)를 최대한 살려 렌더링할 수 있게 한다.
- 동시에 정규화된 plain text(`bodyText`)를 함께 저장해 검색, AI 요약, 공지 기반 챗봇(RAG) 준비 데이터를 확보한다.

---

### 4.5 게시판 탭

**관련 파일:** `src/screens/BoardTab/`

학생 커뮤니티 게시판으로 **익명/실명 글쓰기**, 이미지 첨부, 상호작용이 가능합니다.

#### 게시글 기능


| 기능  | 설명                                             |
| --- | ---------------------------------------------- |
| 작성  | 제목, 내용, 카테고리 선택, 이미지 최대 10장 첨부                 |
| 익명  | 작성 시 익명 선택 가능 (anonId로 식별, 같은 글 내 댓글 간 일관성 유지) |
| 조회  | 뷰카운트 자동 증가                                     |
| 좋아요 | 1인 1회, `userBoardInteractions` 컬렉션으로 관리        |
| 북마크 | 동일 컬렉션에서 isBookmarked 토글                       |
| 신고  | `reports` 컬렉션에 신고 접수                           |


#### 댓글 기능

- 댓글/대댓글 (parentId 기반 2단계 구조)
- 댓글도 익명 선택 가능
- 게시글 작성자와 댓글 작성자가 같은 경우 시각적 표시

#### 이미지 처리

- `react-native-image-picker`로 선택
- `@bam.tech/react-native-image-resizer`로 리사이징 후 Firebase Storage 업로드
- 썸네일 URL과 원본 URL 분리 저장

---

### 4.6 채팅 탭

**관련 파일:** `src/screens/ChatTab/`

택시 파티 채팅과 별개로, **학교 공개 채팅방**을 제공합니다.

#### 채팅방 종류


| 타입           | 설명             |
| ------------ | -------------- |
| `university` | 학교 전체 채팅방      |
| `department` | 학과별 채팅방        |
| `game`       | 마인크래프트 등 게임 채팅 |
| `custom`     | 사용자 생성 커스텀 방   |


#### 읽음 상태 관리

- `users/{uid}/chatRoomStates/{chatRoomId}` — `lastReadAt` 기준으로 미읽음 수 계산
- 실시간 Firestore 구독으로 메시지 수신

#### 마인크래프트 채팅 연동

성결대 마인크래프트 서버와 **양방향 채팅 연동**을 지원하는 특수 기능입니다.

- 마인크래프트 서버 → Firebase Realtime Database → 앱: 게임 내 채팅이 앱에 표시
- 앱 → Realtime Database → 마인크래프트 서버: 앱 메시지가 게임 내에 표시
- 사용자 프로필에 마인크래프트 계정(닉네임, UUID, Java/Bedrock 에디션) 연동
- `src/lib/minecraftChat.ts`에서 메시지 전송 함수 제공
- `src/lib/minecraft/`에서 계정 UUID 조회 및 등록
- RTDB 구독은 `src/screens/ChatTab/ChatDetailScreen.tsx`에서 직접 처리

---

### 4.7 알림 & 개인 설정

**관련 파일:** `src/screens/HomeTab/NotificationScreen.tsx`, `SettingScreen.tsx`

#### 알림 설정 (SettingScreen 내 알림 설정 화면)


| 설정 항목      | 필드명                         | 설명                                                  |
| ---------- | --------------------------- | --------------------------------------------------- |
| 전체 알림      | `allNotifications`          | 마스터 토글 — 꺼지면 모든 알림 중단                               |
| 파티 알림      | `partyNotifications`        | 새 파티 생성, 동승 요청, 수락/거절, 상태 변경                        |
| 공지 알림      | `noticeNotifications`       | 새 학교 공지 (카테고리별 세분화 가능: `noticeNotificationsDetail`) |
| 학사 일정 알림  | `academicScheduleNotifications` | 기본: 중요 일정(`isPrimary=true`) `startDate` 당일 오전 09:00, 옵션: `academicScheduleDayBeforeEnabled`, `academicScheduleAllEventsEnabled` |
| 게시판 좋아요 알림 | `boardLikeNotifications`    | 내 글에 좋아요                                            |
| 댓글 알림         | `commentNotifications` | Board/Notice 공통 댓글 알림 마스터. 내 글/공지, 내 댓글에 새 댓글/답글 |
| 북마크 게시글 댓글 알림 | `bookmarkedPostCommentNotifications` | 내가 북마크한 게시글에 새 댓글이 달리면 발송 |
| 시스템 알림     | `systemNotifications`       | 앱 업데이트, 서비스 공지                                      |
| 마케팅 알림     | `marketingNotifications`    | 이벤트                                                 |


- 채팅방별 mute는 별도 설정: `users/{uid}/chatRoomNotifications/{chatRoomId}` 토글 (설정 화면이 아닌 채팅방 내부에서 설정)
- 알림 인박스: 레거시 클라이언트는 `userNotifications/{uid}/notifications/` 컬렉션을 구독했지만, Spring 런타임은 `user_notifications` 테이블 + `GET /v1/notifications` + `GET /v1/sse/notifications`로 대체한다.
- Spring FCM push payload는 특정 RN legacy payload에 맞추지 않고 canonical `NotificationType` + 리소스 식별자(`partyId`, `noticeId`, `chatRoomId` 등)를 사용한다.
- 서버는 route/screen 이름을 payload에 포함하지 않으며, 클라이언트가 `type + data`를 기준으로 화면 이동을 결정한다.
- 학사 일정 알림은 Phase 8에서 구현되었고, 기본 정책은 중요 일정 당일 오전 09:00(Asia/Seoul) 발송이다.
- 학사 일정 알림 사용자 옵션은 `academicScheduleNotifications`, `academicScheduleDayBeforeEnabled`, `academicScheduleAllEventsEnabled`를 사용한다.
- 현행 Cloud Functions 기준 핵심 푸시 정책 요약:
  - 새 택시 파티: 생성자 제외, 파티 알림 허용 사용자 대상
  - 동승 요청/승인/거절/상태 변경/정산 완료/강퇴/해체: 파티 이해관계자 대상이며 일부 이벤트는 현재 개별 설정을 강제 반영하지 않는다
  - 공개 채팅 메시지: 채팅방 mute + 전체 알림 기준, 인앱 인박스는 생성하지 않는다
  - 파티 채팅 메시지: 파티 멤버 대상, 인앱 인박스는 생성하지 않는다
- 게시글 댓글: 게시글 작성자, 부모 댓글 작성자, 게시글 북마크 사용자 대상이며 `commentNotifications` + `bookmarkedPostCommentNotifications`를 반영한다. 동일 사용자가 여러 조건에 동시에 해당하면 1회만 발송한다.
- 공지 댓글: 현재 Spring 런타임은 부모 댓글 작성자 대상 답글 알림만 지원한다. (`Notice.author`는 회원 ID가 아닌 문자열이기 때문)
  - 학교 공지: 전체 알림 + 공지 알림 + 카테고리 상세 토글 반영
  - 앱 공지: 일반 공지는 시스템 알림 토글 반영, `AppNoticePriority.HIGH`는 설정과 무관하게 강제 발송

#### 강제 업데이트

- `appVersion/{ios|android}` 컬렉션으로 `minimumVersion + forceUpdate + title/message/button*` 메타데이터를 관리
- 버전 메타데이터가 없으면 앱 시작 버전 체크는 기본값 `minimumVersion=1.0.0`, `forceUpdate=false`, `showButton=false`로 처리
- 앱 시작 시 `src/lib/versionCheck.ts`에서 현재 버전 비교 → `forceUpdate: true`이면 업데이트를 강제하고, `showButton=true`면 스토어 이동 버튼을 노출

---

## 5. Firestore 데이터 구조

> 전체 스키마의 단일 진실 공급원: `docs/firestore-data-structure.md`

### 주요 컬렉션 요약

#### 사용자


| 컬렉션                                              | 설명                                |
| ------------------------------------------------ | --------------------------------- |
| `users/{uid}`                                    | 프로필, FCM 토큰, 계좌, 동의 내역, 마인크래프트 계정 |
| `users/{uid}/chatRoomNotifications/{chatRoomId}` | 채팅방별 mute                         |
| `users/{uid}/chatRoomStates/{chatRoomId}`        | 읽음 상태                             |
| `userNotifications/{uid}/notifications/{id}`     | 알림 인박스                            |


#### 택시 파티


| 컬렉션                                    | 설명                                         |
| -------------------------------------- | ------------------------------------------ |
| `parties/{partyId}`                    | 파티 정보, 멤버, 상태, 정산                          |
| `joinRequests/{requestId}`             | 동승 요청 (pending/accepted/declined/canceled) |
| `chats/{partyId}/messages/{messageId}` | 파티 채팅 메시지                                  |


**파티 status 흐름:** `open` → `closed` (정원 충족) → `arrived` (도착) → `ended`

#### 공개 채팅


| 컬렉션                                           | 설명        |
| --------------------------------------------- | --------- |
| `chatRooms/{chatRoomId}`                      | 채팅방 메타데이터 |
| `chatRooms/{chatRoomId}/messages/{messageId}` | 채팅 메시지    |


#### 게시판 & 공지


| 컬렉션                                       | 설명           |
| ----------------------------------------- | ------------ |
| `boardPosts/{postId}`                     | 게시글          |
| `boardComments/{commentId}`               | 댓글/대댓글       |
| `userBoardInteractions/{userId}_{postId}` | 좋아요 + 북마크 통합 |
| `notices/{noticeId}`                      | 학교 공지 (크롤링)  |
| `noticeComments/{commentId}`              | 공지 댓글        |
| `appNotices/{noticeId}`                   | 앱 운영 공지      |


#### 학사·생활 정보


| 컬렉션                              | 설명         |
| -------------------------------- | ---------- |
| `courses/{courseId}`             | 강의 마스터 데이터 |
| `userTimetables/{docId}`         | 사용자별 시간표   |
| `academicSchedules/{scheduleId}` | 학사 일정      |
| `cafeteriaMenus/{weekId}`        | 주차별 학식 메뉴  |


#### 운영/관리


| 컬렉션                        | 설명                                             |
| -------------------------- | ---------------------------------------------- |
| `inquiries/{docId}`        | 문의 접수                                          |
| `reports/{reportId}`       | 신고 접수 (`POST`, `COMMENT`, `MEMBER`)            |
| `adminAuditLogs/{logId}`   | 관리자 활동 감사 로그                                   |
| `qr_logs/{logId}`          | QR 코드 스캔 로그 (os, source, userAgent, timestamp) |
| `appVersion/{ios|android}` | 앱 버전/업데이트 안내 메타데이터                           |


### 보안 규칙 핵심

- `appVersion`: 익명 읽기 허용 (로그인 전 버전 체크 필요)
- 그 외 모든 컬렉션: 인증된 사용자만 접근
- 파티 수정/종료: 리더(`leaderId == auth.uid`)만 가능
- 요청 조회: 요청자 또는 파티 리더만 가능

---

## 6. Firebase Cloud Functions & 알림

**관련 파일:** `functions/src/index.ts`

### 주요 Cloud Functions

> 모든 함수의 기본 리전: `asia-northeast3` (서울), 마인크래프트 동기화만 `asia-southeast1` (싱가포르)

#### 스케줄러


| 함수                  | 스케줄             | 처리 내용                                                           |
| ------------------- | --------------- | --------------------------------------------------------------- |
| `cleanupOldParties` | 4시간마다           | 생성 후 12시간 초과 파티 자동 종료 (status → `ended`, endReason → `timeout`) |
| `scheduledRSSFetch` | 평일 08:00~19:50, 10분마다 | 학교 공지 RSS 자동 크롤링 → Firestore `notices` 컬렉션에 신규/변경분 반영           |


#### 택시 파티 관련


| 함수                     | 트리거                 | 처리 내용                                               |
| ---------------------- | ------------------- | --------------------------------------------------- |
| `onPartyCreate`        | `parties` 생성        | 파티 알림 설정이 켜진 **모든 유저**에게 새 파티 FCM 알림                |
| `onJoinRequestCreate`  | `joinRequests` 생성   | 파티 리더에게 동승 요청 도착 알림                                 |
| `onJoinRequestUpdate`  | `joinRequests` 업데이트 | 승인/거절 시 요청자에게 결과 알림                                 |
| `onPartyStatusUpdate`  | `parties` 업데이트      | status가 `closed`(모집 마감) 또는 `arrived`(도착) 변경 시 멤버 알림 |
| `onSettlementComplete` | `parties` 업데이트      | 모든 멤버의 정산이 완료되면 전원에게 알림                             |
| `onPartyMemberKicked`  | `parties` 업데이트      | 멤버 강퇴 감지 → 강퇴 알림 + 해당 파티 관련 기존 알림 삭제                |
| `onPartyEnded`         | `parties` 업데이트      | 파티 해체(ended) 시 멤버 알림 + 해당 파티 관련 기존 알림 삭제            |


#### 채팅 관련


| 함수                         | 트리거                             | 처리 내용                                  |
| -------------------------- | ------------------------------- | -------------------------------------- |
| `onChatMessageCreated`     | `chats/{partyId}/messages` 생성   | 택시 파티 채팅 메시지 → 음소거/알림 설정 체크 후 FCM 발송   |
| `onChatRoomMessageCreated` | `chatRooms/{id}/messages` 생성    | 공개 채팅방 메시지 → 채팅방별/전체 알림 설정 체크 후 FCM 발송 |
| `syncMinecraftChatMessage` | RTDB `mc_chat/messages/{id}` 생성 | 마인크래프트 서버 → Firestore 채팅방으로 메시지 동기화    |


#### 게시판/공지 관련


| 함수                       | 트리거                        | 처리 내용                                        |
| ------------------------ | -------------------------- | -------------------------------------------- |
| `onNoticeCreated`        | `notices` 생성               | 새 학교 공지 → 카테고리별 알림 설정 체크 후 대상 유저에게 FCM 발송    |
| `onAppNoticeCreated`     | `appNotices` 생성            | 앱 운영 공지 → 레거시는 `urgent`, Spring 런타임은 `HIGH`면 전원, 아니면 시스템 알림 허용 유저에게 발송 |
| `onBoardCommentCreated`  | `boardComments` 생성         | 게시판 댓글/답글 → 게시글 작성자 또는 부모 댓글 작성자에게 알림        |
| `onNoticeCommentCreated` | `noticeComments` 생성        | 공지 댓글/답글 → 대상에게 알림                           |
| `onBoardLikeCreated`     | `userBoardInteractions` 생성 | 좋아요 시 게시글 작성자에게 알림                           |


### FCM 푸시 알림 처리

**토큰 관리:**

- 레거시 앱은 FCM 토큰을 `users/{uid}.fcmTokens[]` 배열에 저장
- Spring 런타임은 토큰을 `fcm_tokens` 테이블로 정규화해 저장
- 멀티 디바이스 지원 (배열로 복수 토큰 관리)
- 별도 정리 함수 없이, **각 알림 함수 내부에서** 전송 실패 토큰을 인라인으로 감지·제거

**알림 전송 전략:**

- 레거시 개별 알림: 수신자 uid → `fcmTokens[]` 조회 → `sendEachForMulticast`로 전송 (500개 배치)
- Spring 런타임도 `sendEachForMulticast` 500개 배치와 invalid token 정리 정책을 유지한다.
- Spring 런타임은 push presentation profile을 사용해 플랫폼별 sound/channel을 분리한다.
  - `PARTY_*`, `MEMBER_KICKED`, `SETTLEMENT_COMPLETED` → `party_channel`, `new_taxi_party(.wav)`
  - `CHAT_MESSAGE` → `chat_channel`, `new_chat_notification(.wav)`
  - `NOTICE`, `APP_NOTICE`, `ACADEMIC_SCHEDULE` → `notice_channel`, `new_notice(.wav)`
  - `POST_LIKED`, `COMMENT_CREATED` → Android는 별도 channel override 없이 기본 동작, iOS는 `default`
- 채팅방 알림: 채팅방별 mute + 전체 알림 설정 체크 후 발송
- 공지 알림: 카테고리별 세부 알림 설정까지 체크 후 발송
- 대부분의 Push 알림은 동시에 인앱 인박스를 생성한다. 레거시는 `userNotifications` 컬렉션, Spring 런타임은 `user_notifications` 테이블을 사용한다. 단, `PARTY_CREATED`, 공개 채팅/파티 채팅 메시지는 인박스를 생성하지 않는다.

### 마인크래프트 채팅 브릿지

- **게임 → 앱**: `syncMinecraftChatMessage` 함수가 RTDB `mc_chat/messages/` 리스닝 → Firestore `chatRooms/game-minecraft/messages/`로 동기화
- **앱 → 게임**: 클라이언트가 RTDB에 직접 메시지 작성 (`src/lib/minecraftChat.ts`)
- RTDB 구독(서버 상태, 접속자 수)은 `ChatDetailScreen`에서 직접 처리
- `src/lib/minecraft/`에는 계정 관리 유틸 (`lookupUuid.ts`, `registerAccount.ts`)

---

## 7. 개발 환경 & 운영 스크립트

### 7.1 사전 요건

- Node.js 18+
- Yarn (패키지 매니저)
- Android Studio / Xcode
- Firebase CLI (`npm install -g firebase-tools`) + `firebase login`

### 7.2 주요 커맨드

```bash
# 개발 서버
yarn start              # Metro Bundler 실행
yarn android            # Android 빌드 & 실행
yarn ios                # iOS 시뮬레이터 빌드 & 실행

# 코드 품질
yarn lint               # ESLint 검사 (PR 전 필수)
yarn test               # Jest 테스트

# Cloud Functions 배포
cd functions
npm run build
firebase deploy --only functions
```

### 7.3 Firebase 설정 파일


| 파일                                    | 플랫폼             |
| ------------------------------------- | --------------- |
| `android/app/google-services.json`    | Android         |
| `ios/SKTaxi/GoogleService-Info.plist` | iOS             |
| `firestore.rules`                     | Firestore 보안 규칙 |


### 7.4 운영 스크립트

```bash
# 앱 버전 관리 (iOS/Android 강제 업데이트 설정)
node scripts/manage-app-version.js

# 앱 운영 공지 CRUD (appNotices 컬렉션)
node scripts/manage-app-notices.js

# 학교 공지 동기화 (크롤링 후 Firestore 업로드)
node scripts/upload-notices.js
node scripts/add-dummy-notice.js   # 테스트용 더미 공지

# 오픈소스 라이선스 생성
node scripts/generate-licenses.js
```

### 7.5 개발 가이드라인

1. **Firestore 스키마 변경 전** `docs/firestore-data-structure.md` 먼저 업데이트
2. **수정 후** `yarn lint` 필수 실행 — 경고도 무시 금지
3. **모호한 요구사항**은 추천안과 함께 질문 목록 정리 후 구현

---

## 부록: 향후 계획

`docs/spring-migration/` 폴더에 Spring Boot 백엔드 마이그레이션 설계 문서가 작성되어 있습니다. Firebase 서버리스 구조에서 Spring Boot + PostgreSQL REST API 구조로의 전환을 검토 중입니다. 주요 검토 내용:

- 도메인 분석 (`domain-analysis.md`)
- ERD 설계 (`erd.md`)
- API 명세 (`api-specification.md`)
- 기술 전략 (`tech-strategy.md`)
- 역할 정의 (`role-definition.md`)
