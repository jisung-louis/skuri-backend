# 작업 완료 시 체크리스트

## 머지 전 필수 검증
1. `./gradlew build` 성공
2. 변경된 기능 관련 Contract/Service/Event/Security 테스트 수행
3. API 정상/예외 케이스 최소 1개 이상 확인
4. `ApiResponse` 에러 포맷 일관성 확인
5. OpenAPI example과 실제 `errorCode/message` 일치 확인
6. OpenAPI/문서 동기화 확인 (`/v3/api-docs` 기준, `docs/api-specification.md`, lifecycle 정책 문서 포함)
7. 회원 라이프사이클 변경이면 탈퇴 후 접근 차단, 동일 UID 재가입 차단, 연관 도메인 정합성 회귀 확인
8. Serena Memory 동기화 확인

## 운영/배포 변경 시 추가 검증
1. `./gradlew build` 성공
2. `docker compose` 설정 파일 문법/기동 절차 확인
2-1. 로컬 Docker 이미지 빌드 컨텍스트에 `application-local.yaml`, `application-local-emulator.yaml`이 포함되는지 확인
3. `/actuator/health` 응답 확인
4. `docker-compose.prod.yml` 렌더링과 MySQL/Redis 영속 볼륨 정책 확인
5. 운영 app host 바인딩이 `127.0.0.1:<APP_HOST_PORT>` loopback 으로만 열리는지 확인
6. `GET /v1/app-versions/android` 같은 공개 API smoke check 확인
7. `prod`에서 OpenAPI가 기본 비노출인지 확인
8. 브라우저 관리자 페이지가 있으면 허용 Origin의 REST CORS preflight와 WebSocket Origin 설정을 함께 확인
7. 로컬 프로필 변경 시 `local`은 실제 Firebase 자격증명 경로가 필요한지, `local-emulator`는 자격증명 경로 없이도 실행되는지 함께 확인
8. 배포 전/후 체크리스트와 rollback 문서 동기화 확인
9. 운영 MySQL 접근 정책을 바꿨다면 host 바인딩이 `127.0.0.1` loopback 으로만 열리는지 확인
