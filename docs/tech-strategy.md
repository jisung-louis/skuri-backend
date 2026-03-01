# Spring Boot 전환 기술 전략 가이드

> 최종 수정일: 2026-02-23
> 목적: Firebase → Spring Boot 전환 시 취업 포트폴리오 관점에서 투자 대비 효율이 높은 기술 적용 전략 정리

---

## 1. 핵심 도메인 구조 (RDB 설계 기준)

Firebase Firestore 컬렉션 구조를 RDB 테이블로 매핑한 도메인 목록입니다.

| 도메인 | 주요 테이블 | 특이사항 |
|--------|------------|---------|
| **User** | `users`, `user_fcm_tokens`, `user_notification_settings` | Firebase Auth UID를 PK로 사용 |
| **Party** | `parties`, `party_members`, `join_requests`, `settlements` | 상태 머신 + 동시성 핵심 도메인 |
| **Chat** | `party_messages`, `chat_rooms`, `chat_room_messages`, `chat_room_states` | 실시간 채널 필요 |
| **Board** | `board_posts`, `board_comments`, `user_board_interactions` | 좋아요·북마크 단일 테이블 통합 |
| **Notice** | `notices`, `notice_comments`, `app_notices`, `notice_reads` | RSS 크롤링 → 스케줄러 |
| **Notification** | `user_notifications` | Cloud Functions 역할 대체 |
| **Academic** | `courses`, `user_timetables`, `academic_schedules`, `cafeteria_menus` | 읽기 전용 마스터 데이터 |

> **Party 도메인이 가장 중요하다.** 상태 전이, 동시성 제어, 권한 분리, 이벤트 알림이 모두 여기에 집중되며, 면접에서 설명할 수 있는 소재가 가장 풍부하다.

---

## 2. 기술별 적용 시나리오

---

### 2-1. Spring Boot + RESTful API 설계

#### 취업 어필 이유
한국 백엔드 채용의 90% 이상이 Spring을 요구한다. 단, `CRUD를 만들었다`가 아니라 **"왜 이렇게 설계했는가"** 를 설명할 수 있어야 가산점이 된다.

#### 스쿠리 적용 시나리오

**Party 상태 머신 — 면접 단골 소재**

```
open → (리더 마감) → closed
closed → (도착 처리) → arrived
arrived → (정산 완료) → ended
```

**권한 분리 설계**

```http
PATCH /api/parties/{id}/status            ← 리더만
POST  /api/join-requests/{id}/accept      ← 리더만
POST  /api/parties/{id}/settlement/complete ← 리더만
```

**동승 요청 플로우**

```http
POST /api/parties/{partyId}/join-requests   → pending 생성
POST /api/join-requests/{id}/accept         → accepted + members 추가
POST /api/join-requests/{id}/decline        → declined
```

`Settlement` 플로우(정산 시작 → 개별 확인 → 완료)는 복잡한 비즈니스 로직이라 Service 계층 설계 어필에 최적이다.

| 항목 | 내용 |
|------|------|
| 구현 난이도 | Low |
| 신입 판단 | 필수. 핵심 기반 |

---

### 2-2. 동시성 처리

#### 취업 어필 이유
면접 단골 질문: *"동시에 여러 사용자가 같은 자원에 접근하면 어떻게 처리합니까?"* 실제 도메인 사례로 답할 수 있는 것이 핵심이다.

#### 스쿠리 적용 시나리오

**시나리오:** 4인 파티에 5명이 0.1초 간격으로 동시 동승 신청

**해결책 A — Optimistic Lock (추천)**

```java
@Entity
public class Party {
    @Version
    private Long version;  // 충돌 시 ObjectOptimisticLockingFailureException

    private int currentMembers;
    private int maxMembers;
}

@Transactional
public JoinRequest acceptJoinRequest(Long requestId) {
    Party party = partyRepository.findById(partyId).orElseThrow();

    if (party.getCurrentMembers() >= party.getMaxMembers()) {
        throw new PartyFullException();
    }
    // 수락 처리
}
```

**해결책 B — DB 레벨 쿼리**

```sql
UPDATE parties
SET current_members = current_members + 1
WHERE id = ? AND current_members < max_members
```

**비동기 FCM 발송 분리**

```java
// API 응답은 즉시 반환. 푸시는 백그라운드에서 전송
@Async
public void sendJoinRequestNotification(String fcmToken, ...) {
    fcmService.send(fcmToken, ...);
}
```

| 항목 | 내용 |
|------|------|
| 구현 난이도 | Medium |
| 신입 판단 | 적절. 설명 스토리가 명확함 |

---

### 2-3. Redis 캐시

#### 취업 어필 이유
"성능 최적화 경험"과 "캐시 전략 이해"를 보여줄 수 있다. **왜 캐시가 필요한지** 데이터와 함께 설명하면 더 강력하다.

#### 스쿠리 적용 시나리오

**캐시 적용 지점 3곳**

```java
// 1. 파티 목록 — 택시 탭 진입마다 조회되는 가장 빈번한 API
@Cacheable(value = "parties", key = "#status")
public List<PartyResponse> getParties(PartyStatus status) { ... }

// TTL: 30초 (파티는 자주 변하므로 짧게)

// 2. 공지 목록 — RSS 크롤링 결과를 Redis에 저장
// TTL: 10분 (크롤링 주기와 동기화)
@Cacheable(value = "notices", key = "#category + '-' + #page")
public List<NoticeResponse> getNotices(String category, int page) { ... }

// 3. FCM 토큰 — 채팅 메시지 생성 시마다 참여자 전원 토큰 조회
@Cacheable(value = "fcmTokens", key = "#userId")
public List<String> getFcmTokens(String userId) { ... }
```

| 항목 | 내용 |
|------|------|
| 구현 난이도 | Low~Medium |
| 신입 판단 | 적절. 구현과 설명 모두 쉬움 |

---

### 2-4. Kafka (이벤트 기반 처리)

#### 취업 어필 이유
대규모 시스템 설계 이해도를 보여준다. 단, 실제 구현보다 **설계 의사결정을 설명하는 것**이 더 중요하다.

#### 스쿠리 적용 시나리오

현재 Cloud Functions의 역할(`onChatMessageCreated` 등)을 이벤트 기반으로 대체한다.

**단계 1 — 신입 포트폴리오: Spring ApplicationEvent**

```java
// 메시지 저장 완료 후 이벤트 발행
applicationEventPublisher.publishEvent(
    new ChatMessageCreatedEvent(message, participants)
);

// 비동기 리스너에서 FCM 발송
@Async
@EventListener
public void handleChatMessage(ChatMessageCreatedEvent event) {
    fcmService.sendToMultiple(event.getParticipantTokens(), ...);
}
```

**단계 2 — 확장 설계 설명: Kafka**

```
채팅 메시지 저장
    → KafkaProducer.send("chat-events", event)
    → Notification Consumer
    → FCM 발송
```

**면접 답변 포인트**

> *"현재는 Spring 내부 이벤트 버스로 구현했지만, 트래픽 증가 시 Kafka로 교체 가능한 인터페이스 구조로 설계했습니다. 파티 채팅에 동시 접속자가 많아질 경우, 알림 전송이 API 응답에 영향을 주지 않도록 분리가 필요합니다."*

| 항목 | 내용 |
|------|------|
| 구현 난이도 | High (실구현) / Low (설계 논의) |
| 신입 판단 | 실구현은 과함. 설계 논의 수준으로 적절 |

---

### 2-5. Docker + AWS 배포

#### 취업 어필 이유
"로컬에서만 돌아가는 프로젝트"와 "실제로 서비스 중인 프로젝트"의 차이다. 운영 경험은 신입에게 큰 가산점이다.

#### 스쿠리 적용 시나리오

**로컬 개발 환경 (docker-compose)**

```yaml
services:
  app:
    build: .
    ports: ["8080:8080"]
    depends_on: [db, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/sktaxi

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: sktaxi
      POSTGRES_PASSWORD: password

  redis:
    image: redis:7-alpine
```

**AWS 운영 환경**

```
EC2 (Spring Boot)
  → RDS PostgreSQL
  → ElastiCache Redis
  → Firebase Admin SDK (FCM 발송 + ID Token 검증)
```

GitHub Actions로 `main` 브랜치 push 시 EC2 자동 배포 파이프라인을 추가하면 CI/CD 경험까지 어필할 수 있다.

| 항목 | 내용 |
|------|------|
| 구현 난이도 | Medium |
| 신입 판단 | 적절. EC2 + RDS 수준은 사실상 필수 |

---

### 2-6. MSA 전환 가능성

#### 취업 어필 이유
아키텍처 사고방식을 보여주는 기회다. 실제로 분리하지 않더라도 **경계가 명확한 모놀리식 구조**로 설계하면 충분히 어필된다.

#### 스쿠리 적용 시나리오

```
com.sktaxi
├── party/         ← 독립 서비스로 분리 가능
├── chat/          ← WebSocket 전담 서버로 분리 가능
├── notification/  ← 알림 전담 서비스로 분리 가능
├── notice/        ← 읽기 전용 서비스로 분리 가능
└── user/          ← Auth 서비스로 분리 가능
```

**면접 설명 포인트**

> *"현재는 모놀리식이지만, Notification 서비스는 파티/채팅 도메인에서 이벤트만 수신하는 구조로 설계했기 때문에, 별도 서비스로 분리가 용이합니다."*

| 항목 | 내용 |
|------|------|
| 구현 난이도 | High (실분리) / Low (설계 논의) |
| 신입 판단 | 실분리는 오버스펙. 패키지 구조로 경계만 명확히 |

---

### 2-7. Kubernetes

#### 취업 어필 이유
대규모 운영 이해를 보여주지만, 신입에게는 역효과가 날 수 있다.

#### 판단

대학교 앱에서 K8s가 실제로 필요한 규모가 아니다. 억지로 넣으면 면접관이 *"왜 굳이 K8s를?"* 이라고 물을 때 답하기 어렵다. Docker까지만으로 충분하다.

| 항목 | 내용 |
|------|------|
| 구현 난이도 | High |
| 신입 판단 | 과함. Docker까지만으로 충분 |

---

### 2-8. LLM 기반 기능

#### 취업 어필 이유
2025-2026년 채용 시장에서 **AI 활용 능력**은 독보적인 차별화 포인트다. 구현 난이도 대비 어필 효과가 매우 크다.

#### 스쿠리 적용 시나리오

**시나리오 A — 공지 3줄 요약 (구현 난이도 최저, 임팩트 최고)**

```java
// GET /api/notices/{id}?summary=true
public String summarizeNotice(String content) {
    String prompt = """
        다음 대학교 공지를 학생 입장에서 핵심만 3줄로 요약해줘:
        %s
        """.formatted(content);

    return llmClient.complete(prompt);  // Claude API or OpenAI API
}
```

**시나리오 B — 파티 추천 챗봇**

```http
POST /api/chat/find-party
{
  "message": "내일 오전 9시에 천안에서 학교 가는 파티 있어?"
}
```

자연어 입력 → LLM으로 조건 파싱 → Party 검색 → 결과 반환

| 항목 | 내용 |
|------|------|
| 구현 난이도 | Low~Medium |
| 신입 판단 | 적절. 차별화 포인트로 강력 추천 |

---

## 3. 신입 포트폴리오 투자 대비 효율 Top 5

| 순위 | 기술 | 구현 난이도 | 어필 효과 | 비고 |
|------|------|------------|----------|------|
| **1** | Spring Boot + RESTful API<br>(Party 상태머신 + 권한설계) | Low | ★★★★★ | 필수. 없으면 서류 통과 불가 |
| **2** | 동시성 처리<br>(Optimistic Lock + @Async) | Medium | ★★★★★ | 면접 킬러카드. 스토리 명확 |
| **3** | Redis 캐시<br>(파티목록 + FCM토큰 캐시) | Low | ★★★★☆ | 구현 쉽고 설명도 쉬움 |
| **4** | Docker + AWS 배포<br>(EC2 + RDS + GitHub Actions) | Medium | ★★★★☆ | 운영 경험 어필 |
| **5** | LLM 연동<br>(공지 요약 + 파티 챗봇) | Low | ★★★★☆ | 차별화. AI 트렌드 선도 |

**보류 기술:**

| 기술 | 이유 |
|------|------|
| Kafka | 설계 논의로만. 실구현은 신입 포트폴리오에서 오버스펙 |
| Kubernetes | Docker까지만으로 충분. 규모 대비 과함 |
| MSA | 패키지 구조로 경계만 명확히. 실분리는 복잡도만 증가 |

---

## 4. 구현 순서 권장안

```
Phase 1 — Spring Boot 기반 구축 (2~3주)
  - Party 도메인 REST API 설계 및 구현
  - Firebase ID Token 검증 필터
  - JPA + PostgreSQL 연동
  - Optimistic Lock으로 동시성 처리

Phase 2 — 성능 레이어 (1~2주)
  - Redis 캐시 (파티 목록 + 공지 목록)
  - @Async FCM 푸시 비동기 처리
  - @Scheduled (파티 cleanup + RSS 크롤링)
  - Spring ApplicationEvent 기반 알림 아키텍처

Phase 3 — 인프라 (1주)
  - Dockerfile + docker-compose
  - AWS EC2 / RDS 배포
  - GitHub Actions CI/CD 파이프라인

Phase 4 — 차별화 (1주)
  - LLM 공지 요약 API
  - Swagger(OpenAPI) 문서화
  - 포트폴리오 README 작성
```

---

## 5. 면접 준비 핵심 질문 목록

Party 도메인 하나만 잘 설계해도 아래 질문에 모두 답할 수 있다.

- "동시에 여러 사람이 같은 파티에 동승 신청하면 어떻게 처리했나요?"
- "캐시를 적용한 이유와 TTL을 어떻게 결정했나요?"
- "FCM 푸시 발송이 API 응답 속도에 영향을 주지 않도록 어떻게 설계했나요?"
- "서비스가 커지면 어떻게 확장할 생각인가요?"
- "MSA 전환을 고려했다면 어떤 서비스부터 분리하겠나요?"
- "공지 요약 기능에서 LLM을 왜 선택했고, 비용은 어떻게 관리했나요?"
