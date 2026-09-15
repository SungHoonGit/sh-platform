# Common Modules

## Overview
sh-platform-common provides reusable modules for scheduling and notification.

## Architecture
```
sh-platform-common/
├── scheduling/
│   ├── ScheduleConfig.java      # 스케줄 설정 엔티티
│   ├── ScheduleLog.java         # 실행 이력 엔티티
│   ├── ScheduleService.java     # 스케줄 관리 서비스
│   └── controller/
│       └── ScheduleController.java
└── notification/
    ├── NotificationConfig.java  # 알림 설정 엔티티
    ├── NotificationLog.java     # 전송 이력 엔티티
    ├── NotificationService.java # 알림 관리 서비스
    └── controller/
        └── NotificationController.java
```

## Usage

### 1. build.gradle.kts에 의존성 추가
```kotlin
dependencies {
    implementation(project(":sh-platform-common"))
}
```

### 2. Application에 스캔 범위 추가
```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.your.package",
    "com.shplatform.common"
})
@EntityScan(basePackages = {
    "com.your.package.model",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
@EnableJpaRepositories(basePackages = {
    "com.your.package.repository",
    "com.shplatform.common.scheduling",
    "com.shplatform.common.notification"
})
public class YourApplication { ... }
```

### 3. DB 테이블 생성
각 서비스의 DB에 공통 테이블 생성:
```sql
-- common_schedule_config
-- common_schedule_log
-- common_notification_config
-- common_notification_log
```

## API Endpoints

### Schedule API
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/schedule/configs` | 스케줄 설정 목록 |
| POST | `/schedule/configs` | 스케줄 설정 생성 |
| PUT | `/schedule/configs/{id}` | 스케줄 설정 수정 |
| DELETE | `/schedule/configs/{id}` | 스케줄 설정 삭제 |
| GET | `/schedule/configs/{id}/logs` | 실행 이력 조회 |

### Notification API
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/notification/configs` | 알림 설정 목록 |
| POST | `/notification/configs` | 알림 설정 생성 |
| PUT | `/notification/configs/{id}` | 알림 설정 수정 |
| DELETE | `/notification/configs/{id}` | 알림 설정 삭제 |
| POST | `/notification/send` | 알림 전송 |
| GET | `/notification/logs` | 전송 이력 조회 |

## Schedule Config Example
```json
{
  "moduleName": "scraper",
  "taskName": "daily_crawl",
  "cron": "0 9 * * *",
  "isEnabled": true
}
```

## Notification Config Example
```json
{
  "moduleName": "scraper",
  "eventType": "new_jobs_found",
  "notificationType": "EMAIL",
  "isEnabled": true,
  "recipientEmail": "user@example.com"
}
```

## MSA 대비
- 모듈은 라이브러리로 포함하여 사용
- 향후 서비스 분리 시 별도 API 서버로 전환 가능
- DB는 각 서비스가 독립 관리 (공통 테이블 포함)
