# 009-260818-msa-log-files-design.md

## 개요
- **목적**: scraper/resume/portfolio 3개 MSA에 파일 로그(logback rolling)를 구축해, 서비스별 로그를 Loki로 중앙 수집한다
- **범위**: logback-spring.xml 추가, Promtail 설정, 배포 확인
- **작성일**: 2026-08-18
- **작성자**: AI Assistant / 사용자
- **관련 학습**: [journald 로그 학습](../learnings/003-260818-journald-logging-learning.md)

## 1. 배경 및 이유

### 1.1 현재 상태
- auth만 `logback-spring.xml` 보유 (daily rolling + 30일 + 500MB)
- scraper/resume/portfolio는 파일 로그 **없음** — stdout만 systemd journald로 수집
- journald 한계: 크기 제한으로 오래된 로그 자동 삭제, 회전 정책 불가, 서비스별 분리 어려움
- 결과적으로 **scraper/resume/portfolio 로그는 Loki에 수집되지 않거나**, journald 경유로만 존재

### 1.2 문제 인식
- 8번 알림 규칙(앱 에러 급증)이 scraper/resume/portfolio의 에러를 못 볼 수 있음
- 장애 원인 분석 시 해당 서비스 로그 이력이 없음
- 서비스별 로그 분리/회전/보관 정책 부재

## 2. 요구 사항

### 2.1 기능 요구 사항
- [ ] FR-001: scraper/resume/portfolio 각각 `logback-spring.xml` 추가
- [ ] FR-002: 각 서비스가 **자기 경로**에 rolling 로그 생성
- [ ] FR-003: Promtail이 각 서비스 경로를 읽어 Loki에 전송 (service 라벨 구분)
- [ ] FR-004: 로그 회전 정책 통일 (daily + 30일 + 500MB)

### 2.2 비기능 요구 사항
- **일관성**: auth와 동일한 로그 포맷/회전 정책
- **호환성**: Promtail 파일 추적과 충돌 없음
- **디스크**: 전체 로그 500MB 캡으로 디스크 안정성

## 3. 설계

### 3.1 디렉토리 구조

```
${LOG_PATH}/
├── auth-platform/     auth-platform.log + auth-platform-error.log
├── scraper-platform/  scraper-platform.log + scraper-platform-error.log
├── resume-platform/   resume-platform.log + resume-platform-error.log
└── portfolio-platform/ portfolio-platform.log + portfolio-platform-error.log
```

> 디렉토리와 파일명 모두 **`${APP_NAME}`(=`spring.application.name`)** 기반. 로그 파일명만 봐도 어떤 서비스인지 구분 가능하고, Promtail이 경로에서 `service` 라벨 추출하기 좋음. auth는 `spring.application.name: auth-platform` 명시 추가.

### 3.2 로그 경로 휴대성 (상대경로 기본 + 환경변수 오버라이드)

**표준 방식**: Spring Boot는 `logging.file.path`를 시스템 프로퍼티 `LOG_PATH`로 자동 전달하고, `logback-spring.xml`에서 `${LOG_PATH}`로 참조. 하드코딩 절대경로 대신 **기본값 상대경로 + 서버 환경변수**로 구성.

`application.yml` (4개 모듈 공통):
```yaml
logging:
  file:
    path: ${LOG_PATH:logs}   # 기본값 "logs" (상대), 서버는 LOG_PATH 환경변수로 오버라이드
```

`logback-spring.xml` (4개 모듈 공통):
```xml
<springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="logs"/>
<file>${LOG_PATH}/{모듈}/sh-platform.log</file>
```

**동작**:
| 환경 | LOG_PATH | 로그 경로 |
|------|----------|-----------|
| 로컬/개발 | 미설정 → 기본 `logs` | `./logs/{모듈}/sh-platform.log` (상대) |
| 서버 | systemd `Environment=LOG_PATH=/home/ubuntu/sh-platform/logs` | `/home/ubuntu/sh-platform/logs/{모듈}/sh-platform.log` |

> **이점**: 소스를 어디서 받아도(로컬/CI/다른 서버) 절대경로 수정 없이 로그가 남음. 운영 배포 시에만 systemd 환경변수로 경로 지정.

### 3.3 Logback 설정 (3개 모듈 동일, 파일명만 차이)

`modules/{모듈}/backend/src/main/resources/logback-spring.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>

    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="sh-platform"/>
    <springProperty scope="context" name="LOG_PATH" source="logging.file.path" defaultValue="logs"/>

    <!-- 콘솔 출력 (systemd journal) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 파일 로그 (일별 분리) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>500MB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- 에러 전용 로그 -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}/${APP_NAME}-error.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${APP_NAME}/${APP_NAME}-error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="ERROR_FILE"/>
    </root>

    <logger name="com.{패키지}" level="INFO"/>

</configuration>
```

> `{모듈}` = scraper / resume / portfolio, `{패키지}` = com.scraper.platform / com.resume.platform / com.portfolio.platform

### 3.4 서버 systemd 환경변수 설정

각 systemd 유닛(`/etc/systemd/system/sh-platform-{모듈}.service`)에 `LOG_PATH` 추가:
```ini
Environment=LOG_PATH=/home/ubuntu/sh-platform/logs
```
```bash
sudo systemctl daemon-reload
sudo systemctl restart sh-platform-{auth,scraper,resume,portfolio}
```

> **주의**: auth의 기존 로그가 `logs/sh-platform.log`(최상위)에 있었으나, 이번 설계에서 **`logs/auth/` 하위로 이동**. 기존 로그는 수동 이동 또는 방치.

### 3.5 Promtail 설정

`/etc/promtail/promtail-config.yaml`에서 `spring-boot` job의 `__path__`를 새 경로로 변경 + `service` 라벨 추출:

```yaml
scrape_configs:
  - job_name: spring-boot
    static_configs:
      - targets:
          - localhost
        labels:
          job: spring-boot
          __path__: /home/ubuntu/sh-platform/logs/*/*.log
    relabel_configs:
      - source_labels: [__path__]
        regex: '.*/logs/([^/]+)/.*\.log'
        target_label: service
```

> `service` 라벨 = 디렉토리명(`auth-platform`, `scraper-platform`, `resume-platform`, `portfolio-platform`). 기존 옛 경로 `logs/*.log`는 신규 구조로 대체되므로 제거 (옛 로그는 이미 수집됨). 변경 후 `sudo systemctl restart promtail`.

**Promtail 라벨 확인** (배포 후):
```bash
curl -s 'http://localhost:3100/loki/api/v1/label/values' --data-urlencode 'query={job="spring-boot"}'
```

## 4. 구현 계획

| 단계 | 내용 | 검증 |
|------|------|------|
| Phase 1 | 4개 모듈 logback-spring.xml + application.yml 수정 (LOG_PATH) | XML 파싱 검증, 로컬 빌드 |
| Phase 2 | git commit → push → GitHub Actions 배포 | 서버 JAR 재배포 |
| Phase 3 | 서버 systemd에 `LOG_PATH` 환경변수 추가 + 재시작 | `daemon-reload` 후 로그 파일 생성 확인 |
| Phase 4 | Promtail 설정에 경로 추가 + 재시작 | `sudo systemctl restart promtail` |
| Phase 5 | Loki에서 서비스별 로그 확인 | Explore → `{job="spring-boot"}` 로그 검색 |

## 5. 참고 자료
- [journald 로그 학습](../learnings/003-260818-journald-logging-learning.md)
- [모니터링/알림 가이드](../guides/001-260818-monitoring-alerting-guide.md)

---
*작성일: 2026-08-18*