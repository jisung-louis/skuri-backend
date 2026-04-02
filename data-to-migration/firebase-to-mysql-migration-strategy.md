# Firebase -> MySQL 마이그레이션 전략

> 작성일: 2026-04-02  
> 기준 저장소: `/Users/jisung/skuri-backend`  
> 입력 원본 폴더: `/Users/jisung/skuri-backend/data-to-migration`

## 1. 목적

이 문서는 현재 export 완료된 Firebase 원본 JSON을 기준으로, `skuri-backend`의 MySQL 스키마로 안전하게 이관하기 위한 전략을 정리한다.

이번 전략은 아래 두 단계로 나눈다.

1. 사전 이관
- `notices-export.json`만 지금 바로 이관

2. 컷오버 당일 이관
- `users-export.json`
- `courses-export.json`
- `userTimetables-export.json`
- `minecraft_accounts.json`

컷오버 당일 대상은 앱스토어/플레이스토어 업데이트 승인 시점에 최신 데이터를 다시 export한 뒤 이관한다.

## 2. 입력 데이터와 범위

### 2.1 현재 확보된 입력 파일

- `/Users/jisung/skuri-backend/data-to-migration/notices-export.json`
- `/Users/jisung/skuri-backend/data-to-migration/users-export.json`
- `/Users/jisung/skuri-backend/data-to-migration/courses-export.json`
- `/Users/jisung/skuri-backend/data-to-migration/userTimetables-export.json`
- `/Users/jisung/skuri-backend/data-to-migration/minecraft_accounts.json`

### 2.2 현재 확인된 데이터 규모

- 공지: 7,937건
- 사용자: 690건
- 강의 마스터: 2,276건
- 사용자 시간표: 150건
- RTDB 마인크래프트 계정: Java 101건 + Bedrock 89건

### 2.3 이번 이관 범위

#### 지금 바로 이관

- `notices` 테이블

#### 컷오버 당일 이관

- `members`
- `linked_accounts`
- `fcm_tokens`
- `user_timetables`
- `user_timetable_courses`
- `minecraft_accounts`

### 2.4 이번 이관 제외 범위

- `notice_read_status`
- `user_notifications`
- `chat_room_notifications`
- `chat_room_states`
- `user_timetable_manual_courses`
- `notice_comments`
- `notice_likes`
- `notice_bookmarks`
- `app_notices`

제외 이유는 아래와 같다.

- 공지 읽음 상태(`readBy`)는 이번 범위에서 제외한다.
- 유저 알림함은 서버 이관 시 초기화한다.
- 채팅방 mute/읽음 상태는 새 서버에서 “아직 채팅방에 들어오지 않은 상태”로 초기화한다.
- 시간표 직접 입력 강의는 현재 export source에 없다.
- `courses`와 `course_schedules`는 이미 Spring MySQL에 존재하므로 신규 import 대상이 아니다.

## 3. 구현 원칙

### 3.1 원본 JSON은 절대 수정하지 않는다

- `data-to-migration/*.json`은 원본 보관용으로 유지한다.
- 가공은 별도 migration runner 내부에서만 한다.

### 3.2 서비스 레이어가 아니라 전용 migration runner를 사용한다

권장 구현 방식:

- `ApplicationRunner` 또는 전용 CLI entrypoint
- `JdbcTemplate` 또는 순수 SQL 기반 batch insert/upsert

이유:

- 서비스 레이어를 재사용하면 도메인 이벤트 발행, 스케줄러, 알림, 유효성 검사, 외부 호출이 섞여 이관 재현성이 떨어진다.
- 마이그레이션은 “원본을 어떻게 DB 상태로 재구성할지”가 목표이므로, 서비스 API 호출보다 테이블 단위 적재가 더 안전하다.

### 3.3 Dry-run과 실제 적재를 분리한다

권장 옵션:

- `--dry-run`
- `--apply`
- `--file=/abs/path/file.json`
- `--domain=notices|members|courses|timetables|minecraft`

### 3.4 재실행 가능하도록 만든다

- 동일 파일을 여러 번 돌려도 결과가 같아야 한다.
- `INSERT ... ON DUPLICATE KEY UPDATE` 또는 “기존 대상 row 삭제 후 재삽입” 방식으로 idempotent 하게 구성한다.

### 3.5 Firestore Timestamp는 Asia/Seoul 기준 `LocalDateTime`으로 변환한다

변환 규칙:

- `{"_seconds": ..., "_nanoseconds": ...}` -> `Instant`
- `Instant` -> `ZoneId.of("Asia/Seoul")` 기준 `LocalDateTime`

이 저장소는 `LocalDateTime` 기반 엔티티를 쓰므로, import 시점에도 서버 런타임과 같은 시간대 기준을 유지한다.

### 3.6 실행 방식은 "스프링 내부 1회성 배치"로 고정한다

- migration 로직은 `skuri-backend` 코드베이스 안에 구현한다.
- 평소 운영 API 서버처럼 상시 구동하지 않고, `migration.enabled=true`일 때만 1회 실행 후 종료한다.
- 운영 서버에서는 기존 app 컨테이너를 migration 모드로 바꾸지 않고, 같은 이미지를 사용한 별도 임시 컨테이너를 `run --rm` 형태로 한 번만 실행한다.
- 실행 시에는 아래 옵션을 함께 사용해 웹서버와 스케줄러를 끈다.
  - `--spring.main.web-application-type=none`
  - `--spring.task.scheduling.enabled=false`

예시:

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --args='--migration.enabled=true --migration.plan=notices --migration.mode=dry-run --migration.notice-file=/Users/jisung/skuri-backend/data-to-migration/notices-export.json --migration.report-dir=/Users/jisung/skuri-backend/data-to-migration/reports --spring.main.web-application-type=none --spring.task.scheduling.enabled=false'
```

운영 서버 예시:

```bash
docker compose -f docker-compose.prod.yml run --rm \
  -v /opt/skuri/migration-input:/migration-input \
  app \
  java -jar /app/app.jar \
  --spring.profiles.active=prod \
  --spring.main.web-application-type=none \
  --spring.task.scheduling.enabled=false \
  --migration.enabled=true \
  --migration.plan=notices \
  --migration.mode=apply \
  --migration.notice-file=/migration-input/notices-export.json \
  --migration.report-dir=/migration-input/reports
```

## 4. 대상 스키마 요약

주요 대상 엔티티:

- 공지: [`Notice.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notice/entity/Notice.java)
- 회원: [`Member.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/member/entity/Member.java)
- 연결 계정: [`LinkedAccount.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/member/entity/LinkedAccount.java)
- 알림 설정: [`NotificationSetting.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/member/entity/NotificationSetting.java)
- 은행 계좌: [`BankAccount.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/member/entity/BankAccount.java)
- FCM 토큰: [`FcmToken.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notification/entity/FcmToken.java)
- 강의 마스터: [`Course.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/academic/entity/Course.java)
- 강의 시간: [`CourseSchedule.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/academic/entity/CourseSchedule.java)
- 사용자 시간표: [`UserTimetable.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/academic/entity/UserTimetable.java)
- 시간표-강의 매핑: [`UserTimetableCourse.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/academic/entity/UserTimetableCourse.java)
- 마인크래프트 계정: [`MinecraftAccount.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/minecraft/entity/MinecraftAccount.java)

## 5. 도메인별 전략

## 5.1 공지사항 이관 전략

### 5.1.1 지금 바로 이관 가능한 이유

- 공지는 사용자/실시간 상태와 달리 변경 빈도가 상대적으로 낮다.
- 이번 범위에서는 `readBy`를 버리므로, 지금 적재해도 컷오버 당일 재동기화 부담이 작다.

### 5.1.2 대상 테이블

- `notices`

주의:

- 현재 최신 공지 약 200건은 이미 MySQL `notices`에 존재할 수 있다.
- 이 row들은 Firestore export와 `id`는 같고, Spring/Firebase 해시 계산 방식만 다를 가능성이 높다.
- 따라서 공지 이관은 반드시 `id` 기준 upsert로 처리한다.

### 5.1.3 source -> target 매핑

| Firebase export field | MySQL column | 비고 |
| --- | --- | --- |
| `id` | `id` | 그대로 보존 |
| `title` | `title` | 그대로 |
| `content` | `rss_preview` | RSS 미리보기 텍스트 |
| `link` | `link` | 그대로 |
| `postedAt` | `posted_at` | Timestamp 변환 |
| `category` | `category` | 그대로 |
| `department` | `department` | 그대로 |
| `author` | `author` | 없으면 `NULL` 허용 |
| `source` | `source` | 그대로 |
| `contentDetail` | `body_html` | HTML 원문 |
| `contentDetail` | `body_text` | `NoticeBodyTextExtractor.extract()`로 추출 |
| `contentAttachments[]` | `attachments` | JSON 배열 저장 |
| `viewCount` | `view_count` | 신규 insert일 때만 사용, 기존 row면 보존 |
| `likeCount` | `like_count` | 신규 insert일 때만 사용, 기존 row면 보존 |
| 없음 | `comment_count` | 0 고정 |
| 없음 | `bookmark_count` | 0 고정 |
| `createdAt` | `created_at` | 가능하면 원본 보존 |
| `updatedAt` | `updated_at` | 가능하면 원본 보존 |

### 5.1.4 해시 컬럼 전략

주의:

- export JSON의 `contentHash`는 Firebase 레거시 기준 해시다.
- 현재 Spring의 `notices.content_hash` 계산 규칙과 동일하지 않다.

따라서 아래처럼 재계산한다.

1. `rss_fingerprint`
- 레거시 export의 `contentHash`를 그대로 사용
- 이유: 레거시 공지 수집에서는 `title|fullLink|rawDate` 기반 SHA-1을 `contentHash`로 썼고, 현재 Spring의 `rss_fingerprint` 의미와 가장 가깝다.

2. `detail_hash`
- `contentDetail` + `contentAttachments` 기준으로 [`NoticeHashUtils.detailHash(...)`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notice/service/NoticeHashUtils.java) 규칙으로 재계산

3. `content_hash`
- `title`, `rss_preview`, `category`, `posted_at`, `author`, `detail_hash` 기준으로 [`NoticeHashUtils.contentHash(...)`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notice/service/NoticeHashUtils.java) 규칙으로 재계산

4. `detail_checked_at`
- import 시각으로 채운다

이렇게 해야 이후 [`NoticeSyncService.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notice/service/NoticeSyncService.java) 가 돌 때 전체 row를 잘못 “변경됨”으로 판단하지 않는다.

### 5.1.5 제외 대상

- `readBy`는 이번 이관에서 무시
- 공지 댓글/좋아요/북마크도 이번 범위에서 무시

### 5.1.6 기존 MySQL 공지와 겹치는 경우 처리

동일 `id` 공지가 이미 MySQL에 있으면 아래처럼 처리한다.

- update 대상
  - `title`
  - `rss_preview`
  - `link`
  - `posted_at`
  - `category`
  - `department`
  - `author`
  - `source`
  - `rss_fingerprint`
  - `detail_hash`
  - `content_hash`
  - `detail_checked_at`
  - `body_text`
  - `body_html`
  - `attachments`

- 보존 대상
  - `view_count`
  - `like_count`
  - `comment_count`
  - `bookmark_count`

즉, 현재 MySQL에 이미 적재된 공지 200건이 있더라도 `id` 기준 upsert + 카운터 보존 원칙이면 문제 없다.

### 5.1.7 실행 시 주의

- [`NoticeScheduler.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notice/service/NoticeScheduler.java) 가 평일 08:00~19:50 10분 주기로 sync를 돈다.
- 공지 import는 이 시간대를 피하거나, import 동안 스케줄러를 비활성화하고 수행한다.

### 5.1.8 권장 적재 방식

- 배치 크기 200~500
- `id` 기준 upsert
- 재실행 시 같은 `id`는 update

## 5.2 사용자 이관 전략

이 영역은 컷오버 당일 최신 export 기준으로 수행한다.

### 5.2.1 대상 테이블

- `members`
- `linked_accounts`
- `fcm_tokens`

### 5.2.2 제외 대상

- `user_notifications`: 이관하지 않음
- `chatRoomNotifications`: 이관하지 않음
- `chatRoomStates`: 이관하지 않음

채팅방 상태는 새 서버에서 “미입장/미읽음 초기 상태”로 시작한다.

### 5.2.3 `members` 매핑

| Firebase export field | MySQL column | 규칙 |
| --- | --- | --- |
| `id` / `uid` | `members.id` | Firebase uid 보존 |
| `email` | `email` | 필수 |
| `displayName` | `nickname` | 그대로 |
| `studentId` | `student_id` | 그대로 |
| `department` | `department` | 그대로 |
| `photoURL` | `photo_url` | 그대로 |
| `realname` | `realname` | 그대로 |
| `isAdmin` | `is_admin` | 없으면 `false` |
| 없음 | `status` | 모두 `ACTIVE` |
| `createdAt` / `joinedAt` | `joined_at` | `createdAt` 우선, 없으면 `joinedAt` |
| `lastLogin` | `last_login` | 없으면 `joined_at` fallback |
| `account` / `accountInfo` | `bank_*` | `account` 우선, 없으면 `accountInfo` |

은행 계좌 규칙:

- `account`와 `accountInfo`가 둘 다 있으면 `account` 우선
- `account`가 없고 `accountInfo`가 있으면 fallback

### 5.2.4 알림 설정 매핑

source `notificationSettings`는 일부 사용자에게만 존재한다.

현재 MySQL target 필드:

- `allNotifications`
- `partyNotifications`
- `noticeNotifications`
- `boardLikeNotifications`
- `commentNotifications`
- `bookmarkedPostCommentNotifications`
- `systemNotifications`
- `academicScheduleNotifications`
- `academicScheduleDayBeforeEnabled`
- `academicScheduleAllEventsEnabled`
- `noticeNotificationsDetail`

매핑 규칙:

| Firebase key | MySQL field | 비고 |
| --- | --- | --- |
| `allNotifications` | `all_notifications` | 그대로 |
| `partyNotifications` | `party_notifications` | 그대로 |
| `noticeNotifications` | `notice_notifications` | 그대로 |
| `boardLikeNotifications` | `board_like_notifications` | 그대로 |
| `boardCommentNotifications` | `comment_notifications` | 이름 변경 |
| `systemNotifications` | `system_notifications` | 그대로 |
| `noticeNotificationsDetail` | `notice_notifications_detail` | 있으면 그대로 |
| `marketingNotifications` | 없음 | 버림 |

보강 규칙:

- source에 없는 `bookmarkedPostCommentNotifications`는 현재 기본값 `true` 사용
- source에 없는 학사 일정 3종은 현재 기본값 사용
  - `academicScheduleNotifications = true`
  - `academicScheduleDayBeforeEnabled = true`
  - `academicScheduleAllEventsEnabled = false`
- `notificationSettings` 전체가 없으면 [`NotificationSetting.defaultSetting()`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/member/entity/NotificationSetting.java) 기준으로 삽입

### 5.2.5 `linked_accounts` 매핑

source는 `linkedAccounts[]` 배열이다.

매핑 규칙:

| Firebase field | MySQL column | 규칙 |
| --- | --- | --- |
| user id | `member_id` | 부모 회원 FK |
| `provider` | `provider` | `google` -> `GOOGLE` |
| `providerId` | `provider_id` | 그대로 |
| `email` | `email` | 그대로 |
| `displayName` | `provider_display_name` | 그대로 |
| `photoURL` | `photo_url` | 그대로 |

현재 dump에서는 대부분 `google` 1개씩이라, `(member_id, provider)` unique constraint 기준 upsert로 충분하다.

### 5.2.6 `fcm_tokens` 매핑

source는 `fcmTokens[]` + `lastLoginOS` + `currentVersion`.

매핑 규칙:

| Firebase source | MySQL column | 규칙 |
| --- | --- | --- |
| user id | `user_id` | 회원 uid |
| token string | `token` | 그대로 |
| `lastLoginOS` | `platform` | 소문자 유지 (`ios`, `android`) |
| `currentVersion` | `app_version` | 그대로 |
| `lastLogin` 또는 import 시각 | `last_used_at` | `lastLogin` 우선 |

중요:

- `token` unique 이므로 재실행 시 token 기준 upsert
- user 재귀 export에선 사용자당 토큰 배열 1개인 경우가 대부분이지만, 로직은 여러 개를 허용해야 한다

## 5.3 강의/시간표 이관 전략

이 영역은 컷오버 당일 최신 export 기준으로 수행한다.

### 5.3.1 대상 테이블

- `user_timetables`
- `user_timetable_courses`

참고 source:

- `courses-export.json`은 Firestore course ID 해석용 reference source로 사용
- 실제 `courses`, `course_schedules`는 현재 MySQL 데이터를 그대로 유지

이번 source에는 직접 입력 강의가 없으므로 `user_timetable_manual_courses`는 비워둔다.

### 5.3.2 선행 조건

- `courses-export.json`을 반드시 함께 사용한다.
- migration runner가 실행 시점의 MySQL `courses` 테이블을 직접 조회할 수 있어야 한다.
- 현재 확인 결과, 시간표가 참조하는 Firestore course ID는 전부 `courses-export.json` 안에 존재한다.

### 5.3.3 핵심 전제: Firestore course ID와 MySQL course ID는 다르다

샘플 확인 결과:

- Firestore course id 예: `tq26BUtf3H0joBuAAxho`
- MySQL `courses.id` 예: `3ac0dc26-ea5b-4eba-b4cf-6cefda46cd42`

즉, 시간표 이관 시 Firestore `courses[]`를 그대로 `user_timetable_courses.course_id`에 넣으면 안 된다.

### 5.3.4 Firestore course -> MySQL course 매핑 전략

기본 매핑 키:

- `(semester, code, division)`

이유:

- 현재 MySQL `courses`는 UUID PK를 사용한다.
- [`Course.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/academic/entity/Course.java) 는 `(semester, code, division)` unique constraint를 가진다.

하지만 이것만으로 충분하다고 가정하면 안 된다.

추가 확인 결과:

- Firestore `courses-export.json` 자체에는 `(semester, code, division)` 중복이 실제로 존재한다.
- 현재 확인된 중복 key는 2건이다.
  - `('2025-2', '00046', '001')`
  - `('2026-1', '00046', '001')`
- 특히 `2026-1 / 00046` 케이스는 Firestore에서 아래처럼 존재한다.
  - `oAjlK2sleeU6LiHijpjt`: `division=001`, `department=파이데이아학부`, 수요일 2교시
  - `y7b3FV2w35AR9OLdvQNq`: `division=001`, `department=파이데이아학부(2부)`, 수요일 13교시
- 반면 현재 MySQL에는 운영 중 정규화된 값이 들어 있다.
  - `파이데이아학부` 일반반: `division=001`
  - `파이데이아학부(2부)`: `division=005`
- 즉, MySQL은 일부 강의에서 Firestore 원본 `division`을 그대로 유지하지 않고 재부여한 상태다.

따라서 실제 migration runner는 “정확히 같은 `(semester, code, division)`”을 1차 시도로 쓰되, 실패 시 fallback 매칭을 수행해야 한다.

구현 순서:

1. `courses-export.json`에서 `firestoreCourseId -> course source row` 맵 구성
2. 현재 MySQL `courses` + `course_schedules`를 조회해 아래 lookup 맵 구성
   - exact key: `(semester, code, division)`
   - fallback candidate key: `(semester, code, name)`
3. 각 Firestore course ID마다 아래 순서로 MySQL course를 결정
   - 1차: exact key `(semester, code, division)` 일치
   - 2차: `(semester, code, name)` 후보군 조회
   - 3차: `department` 일치로 후보 축소
   - 4차: `schedule` 일치로 후보 축소
   - 5차: 필요 시 `location` 일치로 마지막 축소
4. 후보가 정확히 1건이면 그 MySQL `courses.id`를 사용
5. 0건 또는 2건 이상이면 reject 목록으로 분리
6. 최종적으로 `user_timetable_courses.course_id`에는 MySQL UUID를 넣음

컷오버 전 실제 MySQL DB에서 반드시 확인할 쿼리:

```sql
SELECT semester, code, division, COUNT(*) AS cnt
FROM courses
GROUP BY semester, code, division
HAVING COUNT(*) > 1;
```

기대 결과:

- 0 rows

현재 사용자 확인 기준으로는 실제 MySQL에서 중복이 나오지 않았다.

즉:

- 현재 운영 MySQL은 `(semester, code, division)` unique 상태를 만족한다.
- 다만 일부 강의는 Firestore와 division 값이 달라졌을 수 있으므로, 위 fallback 매칭은 여전히 필요하다.

### 5.3.5 `courses-export.json`의 역할

현재 dump에는 `semester = null`인 course가 1건 있다.

정책:

- `courses-export.json`은 reference source로만 사용한다.
- `semester`, `code`, `division` 중 하나라도 비어 있으면 해당 course는 매핑에 사용하지 않고 reject 목록에 기록한다.
- 이 row가 시간표에서 참조되지 않는지 사전 검증 후 import 진행한다.
- 동일 `(semester, code, division)`을 가진 Firestore course가 여러 개인 경우:
  - exact key로는 절대 바로 결정하지 않는다
  - `department`, `schedule`, 필요 시 `location`까지 써서 1건으로 줄인 뒤에만 진행한다
  - 위 조건으로도 1건으로 줄지 않으면 자동 이관하지 말고 reject 목록으로 보낸다

현재 확인된 예시:

- `2026-1 / 00046 / 001`은 Firestore에서 2건이지만, 현재 시간표에서는 `oAjlK2sleeU6LiHijpjt`만 참조한다.
- `oAjlK2sleeU6LiHijpjt`는 현재 MySQL `division=001`로 매핑 가능하다.
- `y7b3FV2w35AR9OLdvQNq`는 현재 시간표에서 참조되지 않지만, 참조된다면 MySQL `division=005`로 매핑해야 한다.
- `2026-1 / 00046 / 002`, `2026-1 / 00046 / 003`은 현재 시간표에서도 참조되고, MySQL exact key로 바로 매핑 가능하다.

현재 확인 기준으로 시간표에서 참조되지 않는 course ID는 없다.

### 5.3.6 `user_timetables` 매핑

핵심 원칙:

- Firestore timetable 문서 ID를 MySQL `user_timetables.id`에 그대로 보존한다.

이유:

- 이후 `user_timetable_courses` 매핑 시 traceability가 좋아진다.
- `user_timetables.id`도 길이 36 문자열 컬럼이므로 Firestore ID 저장이 가능하다.

매핑 규칙:

| Firebase export field | MySQL column |
| --- | --- |
| `id` | `user_timetables.id` |
| `userId` | `user_id` |
| `semester` | `semester` |
| `createdAt` | `created_at` |
| `updatedAt` | `updated_at` |

### 5.3.7 `user_timetable_courses` 매핑

source는 `userTimetables[].data.courses[]`이지만, 이 값은 Firestore course ID다.

매핑 규칙:

| Source | Target |
| --- | --- |
| timetable document id | `timetable_id` |
| Firestore course id -> MySQL course id 매핑 결과 | `course_id` |

적재 방식:

- `user_timetables` parent upsert
- 해당 `timetable_id`의 기존 `user_timetable_courses` 삭제
- source `courses[]`를 MySQL course UUID로 변환한 뒤 다시 삽입

빈 시간표 문서 1건은 parent row만 생성하고 mapping row는 0건으로 둔다.

## 5.4 마인크래프트 계정 이관 전략

이 영역은 컷오버 당일 최신 export 기준으로 수행한다.

### 5.4.1 대상 테이블

- `minecraft_accounts`

### 5.4.2 source 우선순위

마인크래프트 계정은 source를 두 개 함께 쓴다.

1. 1차 source
- `users-export.json` 안의 `minecraftAccount.accounts[]`

2. 2차 source
- `minecraft_accounts.json` (RTDB `players`, `BEPlayers`)

이유:

- RTDB dump는 whitelist 상태와 `lastSeenAt` 보강에는 유용하다.
- 하지만 `parent_account_id`와 “어느 계정이 본인/self인지”는 `users[].minecraftAccount.accounts[]`가 더 잘 표현한다.

### 5.4.3 현재 매칭 현황

현재 dump 기준:

- user embedded account: 189건
- RTDB account: 190건
- 교집합: 188건
- user only: 1건
- RTDB only: 2건
- 교집합 owner mismatch: 0건

즉, 컷오버 시에는 `users`를 primary source로 써도 큰 문제는 없고, RTDB는 보강/검증용으로 충분하다.

### 5.4.4 source -> target 매핑

| Source | MySQL column | 규칙 |
| --- | --- | --- |
| 없음 | `id` | deterministic UUID 생성 |
| user id / `addedBy` | `owner_member_id` | 회원 uid |
| `whoseFriend` 유무 | `account_role` | 없으면 `SELF`, 있으면 `FRIEND` |
| self 계정의 deterministic id | `parent_account_id` | friend 계정만 채움 |
| `edition` | `edition` | `JE` -> `JAVA`, `BE` -> `BEDROCK` |
| `nickname` | `game_name` | 그대로 |
| `storedName` | `stored_name` | Bedrock만 사용, Java는 `NULL` |
| edition + uuid/storedName | `normalized_key` | 현재 백엔드 규칙으로 정규화 |
| normalized key | `avatar_uuid` | Java는 normalized uuid, Bedrock은 기본 avatar uuid |
| RTDB `lastSeenAt` | `last_seen_at` | 있으면 사용 |
| `linkedAt` | `created_at` | 원본 보존 |
| `lastSeenAt` 또는 `linkedAt` | `updated_at` | 보강 |

### 5.4.5 deterministic account id 규칙

`minecraft_accounts.id`는 source에 직접 존재하지 않으므로 deterministic 하게 만든다.

권장 규칙:

- `UUID.nameUUIDFromBytes(("minecraft:" + ownerUid + ":" + normalizedKey).getBytes(UTF_8))`

이렇게 하면:

- rerun 시 같은 계정이 항상 같은 PK를 가짐
- friend 계정의 `parent_account_id` 연결이 안정적

### 5.4.6 edition / normalized key 규칙

현재 백엔드 기준:

- Java: UUID에서 `-` 제거 후 lowercase
- Bedrock: `be:{storedName}`

참고 구현:

- [`MinecraftIdentityService.java`](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/minecraft/service/MinecraftIdentityService.java)

### 5.4.7 parent-child 연결 규칙

동일 owner 기준으로:

- `whoseFriend`가 없으면 self 계정
- `whoseFriend`가 있으면 friend 계정
- friend 계정의 `whoseFriend` 값은 owner의 self 계정 `nickname` 또는 `storedName`과 매칭해서 `parent_account_id`를 채운다

주의:

- self 계정이 없는 friend 계정은 자동 적재하지 말고 reject 목록으로 보낸다

### 5.4.8 RTDB-only / users-only 불일치 처리

컷오버 당일에도 아래 상황은 자동 삽입보다 검토 대상으로 두는 편이 안전하다.

- users에만 있고 RTDB에 없는 계정
- RTDB에만 있고 users에 없는 계정

현재 dump 기준 예시:

- users only: `be:Dohyun060221`
- RTDB only: `be:nunYUNA`, `be:Dohyun065284`

권장 정책:

- users primary import 완료
- RTDB와 diff 생성
- diff 항목은 수동 검토 후 보조 import 여부 결정

### 5.4.9 RTDB top-level `enabled` 필드 처리

- `minecraft_accounts.json.enabled`는 현재 MySQL 스키마 직접 대응이 없다
- 이번 이관에서는 사용하지 않는다

## 6. 컷오버 순서

## 6.1 지금 바로 수행

1. `notices-export.json` dry-run
2. `notices` import
3. import 결과 검증

## 6.2 컷오버 당일 수행

1. 앱 점검 모드 또는 서버 차단
2. 최신 export 재실행
   - `users-export.json`
   - `courses-export.json`
   - `userTimetables-export.json`
   - `minecraft_accounts.json`
3. 사전 검증
   - users 중복 email 확인
   - timetable의 Firestore course ID가 `courses-export.json`에 모두 존재하는지 확인
   - `courses-export.json`의 business key가 현재 MySQL `courses`에 모두 매핑되는지 확인
   - minecraft account diff 확인
4. import 순서
   - `members`
   - `linked_accounts`
   - `fcm_tokens`
   - `user_timetables`
   - `user_timetable_courses`
   - `minecraft_accounts`
5. 검증
6. 서버 오픈

## 7. 검증 체크리스트

## 7.1 공지

- source row 수 == `notices` row 수
- `attachments` JSON 파싱 오류 없음
- `body_html` 비어 있지 않은 공지 수가 source와 거의 일치
- RSS sync 1회 수동 실행 시 대량 업데이트가 발생하지 않음

## 7.2 사용자

- source 사용자 수 == `members` row 수
- `email` unique 충돌 없음
- `linked_accounts` 수가 source 배열 합계와 일치
- `fcm_tokens` 수가 source 토큰 합계와 일치

## 7.3 강의/시간표

- source timetable 수 == `user_timetables` row 수
- source timetable course 총합 == `user_timetable_courses` row 수
- source timetable referenced Firestore course ID missing count == 0
- `courses-export.json` business key -> MySQL `courses.id` 매핑 실패 count == 0
- 최종 `user_timetable_courses.course_id`는 모두 현재 MySQL `courses.id`를 참조

## 7.4 마인크래프트

- users primary source 계정 수 == `minecraft_accounts` row 수 - reject 수
- owner별 self 1개 / friend 최대 3개 규칙 위반 없음
- `normalized_key` unique 충돌 없음
- friend 계정의 `parent_account_id` 누락 없음

## 8. reject / 수동 검토 목록 운영

아래는 자동 삽입하지 말고 별도 reject 파일로 남긴다.

- `semester`가 null 인 course
- Firestore course business key를 현재 MySQL `courses`에 매핑하지 못한 timetable course
- owner 회원이 존재하지 않는 minecraft account
- self parent를 찾지 못한 friend minecraft account
- 예상치 못한 provider 값을 가진 linked account
- 필수값이 비어 있는 member

권장 산출물:

- `/Users/jisung/skuri-backend/data-to-migration/reports/notices-rejects.json`
- `/Users/jisung/skuri-backend/data-to-migration/reports/members-rejects.json`
- `/Users/jisung/skuri-backend/data-to-migration/reports/courses-rejects.json`
- `/Users/jisung/skuri-backend/data-to-migration/reports/minecraft-rejects.json`

## 9. 구현 권장안

권장 구현 파일 구조 예시:

- `src/main/java/com/skuri/skuri_backend/migration/FirebaseMigrationApplicationRunner.java`
- `src/main/java/com/skuri/skuri_backend/migration/notice/NoticeMigrationRunner.java`
- `src/main/java/com/skuri/skuri_backend/migration/member/MemberMigrationRunner.java`
- `src/main/java/com/skuri/skuri_backend/migration/academic/AcademicMigrationRunner.java`
- `src/main/java/com/skuri/skuri_backend/migration/minecraft/MinecraftMigrationRunner.java`

공통 유틸:

- Firestore timestamp -> `LocalDateTime` 변환 유틸
- deterministic UUID 생성 유틸
- JSON attachment 직렬화 유틸
- dry-run 통계/리포트 유틸

## 10. 최종 결정 요약

1. 공지는 지금 바로 이관한다.
2. 공지 `readBy`는 버린다.
3. 사용자 알림함은 이관하지 않는다.
4. 채팅방 mute/읽음 상태도 이관하지 않는다.
5. 사용자 시간표 이관은 `courses-export.json`을 함께 사용한다.
6. `user_timetables.id`는 Firestore 문서 ID를 그대로 보존한다.
7. `courses.id`는 현재 MySQL UUID를 유지하고, 시간표는 `(semester, code, division)` business key로 매핑한다.
8. 마인크래프트는 `users[].minecraftAccount.accounts[]`를 primary source로 사용하고 RTDB는 보강용으로 쓴다.
9. 모든 이관은 전용 migration runner로 수행하고, dry-run과 apply를 분리한다.
