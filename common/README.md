# Common Module

모듈 간 공유 라이브러리

## 개요

auth, scraper, resume, portfolio 모듈이 공통으로 사용하는 라이브러리.

## 구성

```
common/src/main/java/com/shplatform/common/
├── dto/                    # 공통 DTO (ApiResponse, PageResponse 등)
├── exception/              # 예외 처리 (BusinessException, ErrorCode)
├── file/                   # 파일 뷰어 (마크다운, Excel, PDF)
├── notification/           # 알림 (이메일, 웹 푸쉬)
├── scheduling/             # 스케줄링 (Quartz)
└── security/               # 공통 보안 (JWT, SecurityUtils)
```

## 주요 컴포넌트

### 1. 알림 (`notification/`)

| 클래스 | 설명 |
|--------|------|
| `NotificationService` | 이메일 발송 (Gmail SMTP) |
| `WebPushService` | 브라우저 푸쉬 발송 (VAPID) |
| `PushController` | 푸쉬 구독/해제 API |
| `PushSubscription` | 푸쉬 구독 엔티티 |

### 2. 보안 (`security/`)

| 클래스 | 설명 |
|--------|------|
| `SecurityUtils` | 현재 사용자 ID 조회 |
| `JwtAuthenticationFilter` | JWT 토큰 검증 |

### 3. 예외 (`exception/`)

| 클래스 | 설명 |
|--------|------|
| `BusinessException` | 비즈니스 예외 |
| `ErrorCode` | 에러 코드 열거형 |
| `GlobalExceptionHandler` | 예외 처리 핸들러 |

### 4. 파일 뷰어 (`file/`)

| 클래스 | 설명 |
|--------|------|
| `FileViewerService` | 마크다운/Excel/PDF 렌더링 |

## 사용법

```gradle
// build.gradle.kts
dependencies {
    implementation(project(":common"))
}
```

## 주의사항

- common 변경 시 의존하는 모든 모듈重新 빌드 필요
- 공통 컴포넌트만 추가할 것 (모듈별 로직 금지)
