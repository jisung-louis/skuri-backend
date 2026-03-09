# 코드베이스 구조 상세

## 핵심 엔트리포인트
- 루트 인프라 파일: `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`, `.env.example`
- 프로필 파일: `src/main/resources/application.yaml`, `application-local.yaml`, `application-local-emulator.yaml`, `application-prod.yaml`, `src/test/resources/application-test.yaml`
- 배포 문서: `docs/deployment-guide.md`
- GitHub Actions: `.github/workflows/ci.yml`, `.github/workflows/cd.yml`
- `SkuriBackendApplication.java`

## 프로필 역할
- `application.yaml`: 모든 환경이 공유하는 기본 정책과 공통 datasource 인증정보
- `application-local.yaml`: `localhost:3306` 기반 로컬 통합 테스트 + 실제 Firebase ID Token 흐름 검증용
- `application-local-emulator.yaml`: `localhost:3306` 기반 Firebase Auth Emulator 백엔드 단독 테스트용
- `application-prod.yaml`: compose 내부 `mysql:3306`을 사용하는 OCI 운영 서버용
- `application-test.yaml`: 자동 테스트 전용
