# 개발에 필요한 명령어 모음

## 빌드
```bash
./gradlew build                    # 전체 빌드 (컴파일 + 테스트)
./gradlew clean build              # 클린 빌드
./gradlew compileJava              # 컴파일만
```

## 테스트
```bash
./gradlew test                                      # 전체 테스트 (H2 인메모리 DB)
SPRING_PROFILES_ACTIVE=local ./gradlew test          # local 프로필로 테스트
./gradlew test --tests "com.skuri...ClassName"       # 특정 클래스만 테스트
./gradlew cleanTest test                             # 캐시 무시 재테스트
```

## 서버 실행
```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun                 # 로컬 서버 실행
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun        # Firebase Auth Emulator 기반 실행
CHAT_WS_ALLOWED_ORIGIN_PATTERNS="http://localhost:3000,http://localhost:8081" SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```
- 사전 조건: 로컬 MySQL 서버 기동 + `skuri` DB 생성 필요
- 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 필요 (또는 application-local.yaml 사용)
- WebSocket CORS 허용 Origin은 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS`로 오버라이드 가능

## 테스트 결과 확인
```bash
# 테스트 결과 XML 파일 위치
ls build/test-results/test/

# 테스트 리포트 HTML
open build/reports/tests/test/index.html
```

## Git 워크플로우
```bash
git checkout main && git pull origin main            # main 최신화
git checkout -b feat/<topic>                         # 기능 브랜치 생성
git push -u origin <branch-name>                     # 원격 푸시
gh pr create --title "..." --body "..."              # PR 생성
```

## API 문서 확인 (서버 실행 후)
- Swagger UI: `/swagger-ui/index.html`
- Scalar: `/scalar`
- OpenAPI JSON: `/v3/api-docs`

## 시스템 (macOS / Darwin)
```bash
git status / git diff / git log --oneline -20
ls -la / find . -name "*.java" / grep -r "keyword" src/
```

## 린트/포맷
- 현재 저장소에는 별도 린트/포맷 명령이 정의되어 있지 않으므로, 최소 검증 기준은 `./gradlew build`를 사용한다.
