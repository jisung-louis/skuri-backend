# AGENTS.md

## 문서 목적
이 문서는 `skuri-backend` 저장소에서 작업하는 에이전트/기여자가 동일한 기준으로 구현, 검증, 리뷰, 문서화를 수행하기 위한 운영 규칙이다.

## 프로젝트 기준 문서
작업 전 아래 문서를 최신 기준으로 확인한다.
- `docs/project-overview.md`
- `docs/implementation-roadmap.md`
- `docs/api-specification.md`
- `docs/domain-analysis.md`
- `docs/erd.md`
- `docs/role-definition.md`

## 기술 기준
- Language: Java 21
- Framework: Spring Boot 4.0.3
- Build: Gradle (`./gradlew`)
- Database: MySQL (JPA)

## 작업 범위 규칙
1. 모든 작업은 현재 요청된 목적(기능 추가, 버그 수정, 리팩터링, 운영 작업) 범위 안에서 수행한다.
2. 요청 범위를 벗어나는 구조 변경이나 의존성 추가는 사전 합의 없이 반영하지 않는다.
3. 설계 문서와 구현 의도가 충돌하면, 현재 합의된 요구사항을 우선 적용하고 근거를 남긴다.
4. 범위 외 개선 아이디어는 "제안 사항"으로 분리해 전달하고, 본 변경과 분리한다.

## 코드 아키텍처 규칙
1. 공통 응답 포맷은 `ApiResponse`를 사용한다.
2. 예외 처리는 `GlobalExceptionHandler`에서 일관되게 처리한다.
3. 비즈니스 규칙은 Service 레이어에서 판단한다.
4. Controller는 요청 검증/응답 변환 중심으로 유지한다.
5. 도메인 규칙(상태 전이, 권한)은 클라이언트가 아닌 서버에서 최종 판단한다.

## 브랜치 규칙
1. `main`은 항상 안정 상태로 유지하고 직접 작업/커밋하지 않는다.
2. 기능 1개 또는 버그 1개당 브랜치 1개로 작업한다.
3. 한 브랜치에는 하나의 작업 목적만 담는다.
4. 브랜치명은 아래 형식을 사용한다.
   - 기능: `feat/<topic>`
   - 버그: `fix/<topic>`
   - 문서: `docs/<topic>`
   - 기타: `chore/<topic>`

## 커밋 규칙
1. Conventional Commits를 사용한다.
2. 타입은 영어, 본문(설명)은 한국어로 작성한다.
3. 권장 타입: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
4. 예시: `fix: 404 예외를 ApiResponse NOT_FOUND로 매핑`

## PR/머지 규칙
1. 모든 변경은 작업 브랜치에서 완료한 뒤 PR로 `main`에 머지한다.
2. PR 없이 `main`에 직접 머지하지 않는다.
3. 머지 방식은 `Squash merge`를 기본으로 사용한다.
4. 머지 후 작업 브랜치는 삭제한다.

## 작업 순서 (Git 기본 플로우)
1. `main` 최신화
   - `git checkout main`
   - `git pull origin main`
2. 작업 브랜치 생성
   - `git checkout -b feat/<topic>` (상황에 맞게 `fix/docs/chore`)
3. 작업 후 커밋
   - `git add .`
   - `git commit -m "feat: ..."`
4. 원격 푸시
   - `git push -u origin <branch-name>`
5. GitHub에서 PR 생성 후 머지
6. 로컬 브랜치 정리
   - `git checkout main`
   - `git pull origin main`
   - `git branch -d <branch-name>`

## GitHub 설정 권장
1. `main` 브랜치 보호 활성화
2. `Require a pull request before merging` 활성화
3. `Require status checks to pass` 활성화 (`./gradlew build`)
4. `Squash merge` 사용
5. 머지 후 브랜치 자동 삭제 활성화

## AI 에이전트 협업 규칙
1. 브랜치는 에이전트 기준이 아니라 기능/버그 기준으로 나눈다.
2. 같은 브랜치에서 여러 기능을 동시에 진행하지 않는다.
3. 작업 결과는 반드시 PR 단위로 검토 후 머지한다.

## 문서 동기화 규칙
아래 변경이 있으면 문서를 함께 수정한다.
1. API 계약 변경: `docs/api-specification.md`
2. 엔티티/관계/인덱스 변경: `docs/erd.md`
3. 도메인 책임/경계 변경: `docs/domain-analysis.md`, `docs/role-definition.md`
4. 구현 계획/운영 정책 변경: 관련 계획 문서(예: `docs/implementation-roadmap.md`)

## 검증 기준 (머지 전 최소)
1. `./gradlew build` 성공
2. 변경된 기능과 직접 관련된 테스트/검증 수행
3. API 동작 변경 시 정상/예외 케이스를 최소 1개 이상 확인
4. 공통 에러 포맷(`ApiResponse`)이 깨지지 않는지 확인

## 리뷰 체크리스트
1. 요청 범위를 벗어난 코드가 없는가
2. 상태 머신/권한/인증 규칙이 서버에서 강제되는가
3. 예외가 적절한 HTTP 상태와 `errorCode`로 매핑되는가
4. 응답 스키마가 문서와 일치하는가
5. 변경에 필요한 문서 업데이트가 포함됐는가

## 금지 사항
1. 승인 없는 파괴적 명령(`git reset --hard`, 대량 파일 삭제) 실행 금지
2. 근거 없는 대규모 리팩터링 금지
3. 문서/코드 불일치를 의도적으로 방치하는 커밋 금지

## 운영 메모
- 인증은 Firebase ID Token 검증 기반으로 확장한다.
- 실시간 통신은 채팅(WebSocket)과 이벤트(SSE)를 분리한다.
- 핵심 도메인 우선순위는 `TaxiParty`이며, 상태 전이/동시성/정산 규칙을 가장 엄격히 검증한다.

