# 003-260818-journald-logging-learning.md

## 개요
- **주제**: systemd journald와 애플리케이션 파일 로그의 차이, MSA에서 로그 관리 표준 관행
- **학습일**: 2026-08-18
- **수준**: 초급

## 1. 개념 설명

### 1.1 정의
**journald**는 systemd에 내장된 **중앙 로그 수집 데몬**입니다. systemd가 관리하는 모든 프로세스의 **표준 출력(stdout)/표준 에러(stderr)** 를 자동으로 수집해 저장합니다.

```
Spring Boot 프로세스 ──stdout/stderr──> systemd ──> journald ──> journalctl로 조회
```

Spring Boot에서 파일 로그 설정 없이 콘솔로만 출력하면, 그 출력이 그대로 journald로 들어갑니다.

### 1.2 왜 필요한가
- systemd 서비스는 stdout/stderr를 그냥 버리면 사라짐 → journald가 자동 수집
- `journalctl -u sh-platform-auth` 처럼 서비스 단위로 즉시 로그 확인 가능
- **별도 설정 없이** 로그를 남길 수 있는 가장 간단한 방법

### 1.3 관련 개념
- **journalctl**: journald 로그 조회 명령
- **logrotate**: 파일 로그 회전/삭제 도구
- **rolling (Logback)**: 앱 레벨에서 파일을 일/크기 단위로 분할
- **Promtail**: 파일/journald를 읽어 Loki로 전송

## 2. 사용법

### 2.1 기본 사용
```bash
# 특정 서비스 로그 실시간
sudo journalctl -u sh-platform-auth -f

# 최근 5분
sudo journalctl -u sh-platform-auth --since "5 min ago"

# 전체 서비스
sudo journalctl -u sh-platform-*
```

### 2.2 보관 설정 (journald)
```bash
# /etc/systemd/journald.conf
SystemMaxUse=1G          # journald 최대 사용량
SystemMaxFileSize=100M   # 개별 파일 크기
```
```bash
sudo systemctl restart systemd-journald
```

## 3. 주의사항 — journald의 한계

| 한계 | 설명 | 영향 |
|------|------|------|
| **크기 제한** | 기본 SystemMaxUse(보통 4GB 또는 디스크 10%) 초과 시 **오래된 로그 자동 삭제** | 장애 로그가 지워질 수 있음 |
| **회전 정책 제어 불가** | 서비스별 보관 기간/크기를 지정할 수 없음 | 과거 이력 관리 불가 |
| **서비스 구분 불편** | 모든 서비스가 systemd로 수집, 필터 필요 | 조회 시 유닛 지정해야 함 |
| **영구성 불확실** | Storage=persistent 설정 전까지 재부팅 후 유실 가능 | 메모리 휘발성 |

> **결론**: journald는 "임시 로그"에 적합. **보관/회전/분리 관리가 필요한 로그는 파일 로그가 표준.**

## 4. 실전 적용

### 4.1 MSA 로그 관리 3단계 표준

```
1. 각 서비스가 자기 경로에 파일 로그 작성 (rolling)
2. Promtail이 파일을 읽어 Loki로 전송 (중앙 집중)
3. journald는 콘솔 출력의 부산물 (보조)
```

```
/home/ubuntu/sh-platform/logs/
├── auth/
│   └── sh-platform.log
├── scraper/
│   └── sh-platform.log
├── resume/
│   └── sh-platform.log
└── portfolio/
    └── sh-platform.log
```

### 4.2 이 프로젝트에서의 적용

| 모듈 | 파일 로그 | 상태 |
|------|-----------|------|
| auth | ✅ logback-spring.xml (daily + 30일 + 500MB) | 기존 |
| scraper | ⬜ 없음 (journald만) | 추가 필요 |
| resume | ⬜ 없음 (journald만) | 추가 필요 |
| portfolio | ⬜ 없음 (journald만) | 추가 필요 |

### 4.3 관련 코드
- 파일: `modules/auth/backend/src/main/resources/logback-spring.xml`

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
    <fileNamePattern>/home/ubuntu/sh-platform/logs/sh-platform.%d{yyyy-MM-dd}.log</fileNamePattern>
    <maxHistory>30</maxHistory>
    <totalSizeCap>500MB</totalSizeCap>
</rollingPolicy>
```

## 5. 참고 자료
- [systemd journald 공식 문서](https://www.freedesktop.org/software/systemd/man/latest/systemd-journald.service.html)
- [journald.conf 매뉴얼](https://www.freedesktop.org/software/systemd/man/latest/journald.conf.html)

---
*작성일: 2026-08-18*