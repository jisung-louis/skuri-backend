# OpenAPI Show Schema 전수 보강 계획

> 최종 수정일: 2026-03-24
> 작업 브랜치: `docs/openapi-show-schema-rollout`
> 런타임 계약 기준: `/v3/api-docs`

---

## 1. 배경

- Board/Notice 계약 gap 보강 과정에서 일부 핵심 API는 `Scalar Show schema`에 `data` 내부 구조가 드러나도록 개선했다.
- 그러나 프로젝트 전체 기준으로는 여전히 많은 성공 응답이 `@Schema(implementation = ApiResponse.class)`에 머물러 있어, `Show schema`에서 `data`가 비어 보이거나 generic object처럼 보인다.
- 이 상태는 사람이 문서를 읽을 때도 불편하고, 프론트 코드 생성/계약 검토/회귀 확인에도 불리하다.

---

## 2. 목표

### 2.1 목표

- 모든 `2xx + application/json` 성공 응답에서 contract-critical `data` 필드가 `Scalar Show schema`에 concrete type으로 드러나게 한다.
- 기존 `Example Response`는 유지하고, `Show schema` 품질만 끌어올린다.
- `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar`가 모두 계속 동작하도록 유지한다.
- 전수 회귀 테스트를 추가해 누락된 API 없이 커버한다.

### 2.2 비목표

- 런타임 응답 포맷 `ApiResponse<T>` 자체를 변경하지 않는다.
- 비즈니스 로직/권한/상태 전이 규칙은 손대지 않는다.
- `204 No Content`, `text/event-stream`(SSE), `ApiResponse<Void>` 중심 응답까지 동일 규칙으로 강제하지 않는다.

---

## 3. 현재 관찰 결과

### 3.1 남아 있는 raw `ApiResponse.class` 성공 응답 규모

- `taxiparty`: 90
- `support`: 70
- `chat`: 45
- `academic`: 44
- `board`: 43
- `member`: 32
- `notice`: 30
- `notification`: 21
- `app`: 16
- `image`: 8

### 3.2 도메인별 대표 성공 DTO 수

- `member`: 4
- `notification`: 4
- `academic`: 5
- `chat`: 6
- `taxiparty`: 9
- `support`: 10

즉, annotation 교체 건수는 많지만 실제 wrapper로 관리할 성공 DTO 종류는 생각보다 제한적이다.

### 3.3 방법론 검증 결과

- 현재 프로젝트는 `springdoc-openapi-starter-webmvc-ui/scalar 3.0.2`를 사용한다.
- `useReturnTypeSchema = true`를 샘플 API에 적용해도 `Scalar Show schema`에 `data` 내부 필드가 충분히 드러나지 않았다.
- 따라서 이번 전수 작업은 `도메인별 OpenAPI wrapper schema` 방식으로 진행한다.

---

## 4. 적용 기준

### 4.1 반드시 보강할 대상

- `2xx` 성공 응답
- `application/json` 응답
- `ApiResponse<T>`에서 `T`가 아래 중 하나인 경우
  - 단일 DTO
  - `List<DTO>`
  - `PageResponse<DTO>`
  - 요약/통계/상태 DTO

### 4.2 제외/예외 대상

- `ApiResponse<Void>` 기반 삭제/등록 완료 응답
- `204 No Content`
- `text/event-stream` 기반 SSE 엔드포인트
- 에러 응답(`401/403/404/409/422/500`)

### 4.3 문서 품질 기준

- `Example Response`는 기존 값 유지
- `Show schema`에서 `data`가 description-only 상태로 남지 않아야 함
- `data`는 최소한 아래 중 하나를 가져야 함
  - `properties`
  - `items`
  - `$ref`
  - `oneOf`
  - `anyOf`
  - `allOf`

---

## 5. 구현 방식

### 5.1 기본 원칙

- 런타임 메서드 반환형은 그대로 `ResponseEntity<ApiResponse<T>>` 유지
- OpenAPI 문서화 전용 schema record/class를 도메인별 파일로 추가
- controller의 `2xx` 성공 응답만 해당 wrapper schema를 사용
- `401/403/404/...` 공통 에러 응답 예시는 기존 방식 유지

### 5.2 파일 구조

- `infra/openapi/OpenApiMemberSchemas.java`
- `infra/openapi/OpenApiNotificationSchemas.java`
- `infra/openapi/OpenApiAcademicSchemas.java`
- `infra/openapi/OpenApiSupportSchemas.java`
- `infra/openapi/OpenApiAppSchemas.java`
- `infra/openapi/OpenApiImageSchemas.java`
- `infra/openapi/OpenApiChatSchemas.java`
- `infra/openapi/OpenApiTaxiPartySchemas.java`

기존 [OpenApiBoardSchemas](../src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiBoardSchemas.java)와 [OpenApiNoticeSchemas](../src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiNoticeSchemas.java)는 기준 구현으로 재사용한다.

### 5.3 컨트롤러 반영 규칙

- 성공 응답 annotation의 `schema = @Schema(implementation = ApiResponse.class)`를 wrapper schema로 교체
- 같은 DTO를 쓰는 여러 API는 동일 wrapper를 재사용
- admin/public/member 전용 controller가 같은 DTO를 쓰더라도 중복 wrapper를 만들지 않는다

---

## 6. 구현 로드맵

### Phase A. 스캐폴딩

- 도메인별 wrapper schema 파일 생성
- wrapper naming 규칙 고정
  - 예: `MemberMeApiResponse`, `NotificationUnreadCountApiResponse`, `PartyDetailApiResponse`
- Board/Notice와 동일한 envelope 필드(`success`, `data`, `message`, `errorCode`, `timestamp`) 유지

### Phase B. 소규모/핵심 도메인 먼저 반영

- `app`
- `image`
- `member`
- `notification`

이 구간은 DTO 수가 적고 프론트 소비 빈도가 높아 초반 회귀 확인에 적합하다.

### Phase C. 생활 정보/운영 도메인 반영

- `academic`
- `support`

목록, 페이지 응답, admin 응답이 섞여 있으므로 wrapper 재사용 구조를 먼저 안정화한다.

### Phase D. 실시간/핵심 비즈니스 도메인 반영

- `chat`
- `taxiparty`

DTO가 크고 path 수가 많으므로 마지막에 몰아서 적용한다.  
단, SSE는 제외하고 REST 성공 응답만 다룬다.

### Phase E. 잔여 보정

- 이미 개선된 `board`/`notice`에서 남아 있는 admin/void 예외 처리 확인
- wrapper naming/description 일관성 정리
- 중복 schema 제거

---

## 7. 테스트 및 검증 방법

### 7.1 자동 검증

1. 기존 OpenAPI 예시 규약 테스트 유지
- `OpenApiResponseExamplesConventionTest`

2. 기존 smoke 테스트 유지
- `/v3/api-docs`
- `/swagger-ui/index.html`
- `/scalar`

3. 신규 전수 회귀 테스트 추가
- 목적: `2xx + application/json` 성공 응답 중 대상 API가 generic `data`로 남아 있지 않은지 검사
- 방식:
  - `/v3/api-docs`를 직접 파싱
  - path/method/responseCode별 success schema 확인
  - 예외 허용 대상(`Void`, `204`, SSE`)은 allowlist로 분리

4. 도메인별 대표 API 샘플 테스트 유지/추가
- Board/Notice처럼 핵심 path는 개별 integration test로 고정

### 7.2 수동 검증

- 대표 API를 도메인별로 최소 1개씩 선택
- `Scalar Show schema`에서 `data` 내부 구조 확인
- `Example Response`가 기존과 동일한지 확인
- `/swagger-ui/index.html`에서도 schema가 깨지지 않는지 확인

### 7.3 최종 명령

```bash
./gradlew test --tests 'com.skuri.skuri_backend.infra.openapi.*'
./gradlew build
```

필요 시 보안 smoke 테스트와 domain contract 테스트를 추가로 묶어 실행한다.

---

## 8. 커밋/PR 전략

### 8.1 브랜치

- `docs/openapi-show-schema-rollout`

### 8.2 커밋 분리

1. 도메인별 runtime/OpenAPI 문서화
- 예: `docs: Member Notification 성공 응답 스키마를 구체화한다`

2. 테스트
- 예: `test: OpenAPI success schema 전수 회귀 테스트를 추가한다`

3. Serena memory / 보조 문서
- 예: `chore: Serena Memory에 OpenAPI schema 전수 기준을 반영한다`

### 8.3 PR 범위

- PR 하나의 목적은 “성공 응답 Show schema 전수 보강”
- 런타임 계약 변경이나 비즈니스 로직 수정은 포함하지 않는다

---

## 9. 리스크와 대응

### 9.1 리스크

- controller annotation 변경량이 많아 누락 가능성 존재
- 동일 DTO에 대한 wrapper naming 중복 가능성
- `ApiResponse<Void>`/SSE까지 잘못 건드리면 문서 회귀 가능
- domain별 sample test만으로는 누락이 숨어들 수 있음

### 9.2 대응

- 전수 회귀 테스트를 먼저/같이 추가
- `Void`, `SSE`, `204`는 명시적으로 제외
- domain별 wrapper 파일로 구조를 단순화
- 기존 Board/Notice 구현을 기준 패턴으로 고정

---

## 10. 완료 기준

- 대상 모든 `2xx + application/json` 성공 응답이 `Show schema`에서 concrete `data`를 노출한다
- `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar`가 정상 노출된다
- `OpenApiResponseExamplesConventionTest`와 신규 전수 회귀 테스트가 모두 통과한다
- `./gradlew build` 성공
- shared roadmap와 대응 프론트 문서가 동기화된다

---

## 11. 작업 시작 준비 상태

- `main` 최신화 완료
- 별도 작업 브랜치 생성 완료: `docs/openapi-show-schema-rollout`
- 방식 검증 완료: `useReturnTypeSchema`는 채택하지 않음
- inventory 완료: 적용 대상/제외 대상/도메인별 대표 DTO 목록 정리 완료
- 다음 착수 순서:
  1. wrapper schema 파일 생성
  2. `app/image/member/notification` 반영
  3. 전수 회귀 테스트 추가
  4. `academic/support/chat/taxiparty` 순차 확장

---

## 11. 작업 시작 준비 상태

- `main` 최신화 완료
- 별도 작업 브랜치 생성 완료: `docs/openapi-show-schema-rollout`
- 방식 검증 완료: `useReturnTypeSchema`는 채택하지 않음
- 다음 착수 순서:
  1. wrapper schema 파일 생성
  2. `app/image/member/notification` 반영
  3. 전수 회귀 테스트 추가
  4. 나머지 도메인 확장
