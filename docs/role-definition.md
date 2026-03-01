

# 백엔드 역할 정의서 v1.0
(Firebase Serverless → Spring Migration)

## 1. 문서 목적

본 문서는 Firebase 기반 서버리스 백엔드를 사용 중인 현재 시스템을  
Spring 기반 백엔드로 점진적으로 마이그레이션하기에 앞서,  
Spring 백엔드의 역할과 책임 범위를 명확히 정의하는 것을 목적으로 한다.

이를 통해 다음을 달성한다.

- 프론트엔드(React Native), Spring 백엔드, Firebase 간의 역할 분리
- 마이그레이션 범위 및 원칙 명확화
- 향후 요구사항 명세 및 API 설계의 기준점 제공

---

## 2. 현재 시스템 개요 (AS-IS)

### 2.1 기술 스택

- Client: React Native
- Backend: Firebase Serverless
  - Firestore
  - Cloud Functions
  - Firebase Authentication
  - Firebase Cloud Messaging (FCM)

### 2.2 현재 역할 구조

| 구성 요소 | 역할 |
|---------|-----|
| React Native | 화면 렌더링, 사용자 입력 처리, Firestore 직접 CRUD |
| Firestore | 데이터 저장소 + 실시간 데이터 동기화 |
| Cloud Functions | 이벤트 기반 처리 (알림 발송, 자동 작업) |
| Firebase Auth | 사용자 인증 |
| FCM | 푸시 알림 전송 |

---

## 3. 목표 시스템 개요 (TO-BE)

### 3.1 마이그레이션 목표

- Firebase Serverless 구조를 Spring 기반 중앙 백엔드 구조로 전환
- FCM을 제외한 모든 비즈니스 로직을 Spring으로 이전
- 데이터 및 비즈니스 규칙의 단일 진실의 출처를 Spring 백엔드로 통합

### 3.2 기술 스택 (목표)

- Client: React Native
- Backend: Spring Boot
- Database: MySQL (RDB)
- Authentication: Firebase Authentication
- Notification: Firebase Cloud Messaging (FCM)
- Real-time Communication:
  - SSE (Server-Sent Events)
  - WebSocket

---

## 4. Spring 백엔드의 역할 정의

### 4.1 비즈니스 로직의 최종 판단자

Spring 백엔드는 모든 도메인 규칙의 최종 판단을 수행한다.

- 데이터 생성/수정/삭제에 대한 유효성 검증
- 도메인 상태 전이 규칙 관리

예:
- 파티 생성 가능 조건
- 파티 인원 제한
- 파티 상태 변경 규칙
- 사용자 권한 검증

### 4.2 데이터 접근의 단일 관문

- 클라이언트는 데이터베이스에 직접 접근하지 않는다.
- 모든 데이터 접근은 Spring API를 통해 수행된다.
- Spring 백엔드는 MySQL과의 상호작용을 책임진다.

### 4.3 이벤트 기반 후처리

Firebase Cloud Functions에서 수행하던 역할을  
Spring 내부의 이벤트 기반 구조로 대체한다.

- 비즈니스 핵심 로직과 부가 효과(알림, 로그 등)를 분리한다.
- 도메인 이벤트를 통해 후처리를 수행한다.

---

## 5. 클라이언트(React Native)의 역할

클라이언트는 다음 책임을 유지한다.

- 화면 렌더링
- 사용자 입력 처리
- 사용자 경험(UX) 개선 로직
  - 로딩 처리
  - optimistic UI
  - 버튼 비활성화 등

단, 데이터의 최종 상태 및 비즈니스 규칙 판단은 수행하지 않는다.

---

## 6. 비즈니스 규칙을 서버에서 관리한다는 의미

본 프로젝트에서 비즈니스 규칙이란  
서비스 도메인에서 “이 행위가 가능한지 / 불가능한지”를 판단하는 규칙을 의미한다.

- 클라이언트는 사용자의 의도를 요청으로 전달한다.
- 서버는 요청의 가능 여부를 판단한다.
- 서버의 판단 결과가 항상 최종 결과이다.

이는 “클라이언트에는 화면만 있다”는 의미는 아니다.

---

## 7. 인증(Authentication) 및 권한(Authorization) 구조

### 7.1 인증 방식

- 인증(Authentication)은 Firebase Authentication에 위임한다.
- 클라이언트는 Firebase Auth를 통해 로그인한다.
- 로그인 성공 시 Firebase에서 ID Token(JWT)을 발급받는다.

### 7.2 서버 인증 흐름

1. 클라이언트는 Firebase Auth로 발급받은 ID Token을  
   HTTP 요청의 Authorization 헤더에 포함하여 서버에 전달한다.
2. Spring 백엔드는 Firebase Admin SDK를 사용해 토큰을 검증한다.
3. 검증 성공 시 토큰에 포함된 uid를 사용자 식별자로 사용한다.

Spring 백엔드는 Access Token / Refresh Token을 직접 발급하거나 관리하지 않는다.

### 7.3 권한(Authorization)

- 인증된 사용자(uid)를 기준으로
- 도메인별 권한 검증을 Spring의 비즈니스 로직에서 수행한다.

---

## 8. 실시간 통신 설계 범위

### 8.1 실시간 제공 대상 기능

- 파티 목록
- 파티 상태
- 채팅
- 알림
- 게시물 목록
- 게시물 조회수

### 8.2 통신 방식 분리

| 기능 | 통신 방식 |
|----|----|
| 파티 목록 / 상태 | SSE |
| 알림 | SSE |
| 게시물 목록 / 조회수 | SSE |
| 채팅 | WebSocket |

### 8.3 실시간 통신 인증

- REST API: 매 요청마다 ID Token 전달
- SSE / WebSocket: 연결 시작 시 ID Token 전달
- 연결 인증 성공 후에는 추가 토큰 검증 없이 통신을 유지한다.

---

## 9. 마이그레이션 전략

- 기능(도메인) 단위 점진적 마이그레이션을 적용한다.
- Firebase와 Spring 백엔드는 일정 기간 공존할 수 있다.

---

## 10. 실패 허용 정책

- 비즈니스 핵심 로직은 실패 시 전체 요청을 실패로 처리한다.
- 알림, 로그 등 부가 효과는 실패하더라도 핵심 로직에는 영향을 주지 않는다.

---

## 11. Firebase에 잔존하는 역할

Spring 마이그레이션 이후에도 Firebase는 다음 역할을 유지한다.

- Firebase Authentication
- Firebase Cloud Messaging (FCM)

---

## 12. 정리

Spring 백엔드는 본 프로젝트에서 다음과 같은 역할을 수행한다.

- 비즈니스 규칙의 최종 판단자
- 데이터 접근의 단일 관문
- 실시간 데이터 제공자
- 인증 결과를 신뢰하고 권한을 판단하는 서버

---

## 13. 다음 단계

본 문서를 기준으로 다음 작업을 진행한다.

1. 요구사항 명세서 작성
2. API 명세 (REST / SSE / WebSocket)
3. 도메인 모델링 및 패키지 구조 설계