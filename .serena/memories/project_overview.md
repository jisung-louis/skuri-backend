# SKURI Backend - 프로젝트 개요

## 목적
성결대학교(Sungkyul University) 학생들을 위한 택시 합승 매칭 서비스 백엔드.
사용자는 택시 파티를 생성하거나 참여하고, 실시간 채팅과 정산 기능을 사용할 수 있다.

## 기술 스택
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.3
- **Build Tool**: Gradle (Groovy DSL)
- **Database**: MySQL 8.4 (production), H2 (test)
- **ORM**: Spring Data JPA + Hibernate
- **Authentication**: Firebase Admin SDK (ID Token 검증)
- **Real-time**: WebSocket (STOMP - 채팅), SSE (파티 이벤트)
- **API Documentation**: springdoc-openapi (Swagger UI + Scalar)
- **Testing**: JUnit 5, Spring Boot Test
- **CI/CD**: GitHub Actions

## 도메인 구조
```
domain/
├── member/       # 회원 (Firebase 인증, 프로필, 알림 설정, 은행 계좌)
├── taxiparty/    # 택시 파티 (핵심 도메인 - 생성/참여/정산/상태관리)
├── chat/         # 채팅 (WebSocket STOMP, 채팅방, 메시지)
├── board/        # 커뮤니티 게시판 (게시글/댓글/좋아요/북마크)
└── app/          # 앱 공지/버전 관리
```

## 인프라 구조
```
infra/
├── auth/         # Firebase 인증 + Spring Security
│   ├── config/   # SecurityConfig, FirebaseConfig
│   └── firebase/ # Token 검증, 인증 필터
└── openapi/      # OpenAPI 설정 및 도메인별 예시 상수
```

## 공통 모듈
```
common/
├── dto/          # ApiResponse<T>, PageResponse
├── config/       # JPA Auditing, ObjectMapper
├── entity/       # BaseTimeEntity
└── exception/    # ErrorCode, GlobalExceptionHandler, BusinessException
```

## 이메일 도메인 제한
- `sungkyul.ac.kr` 도메인 이메일만 허용
