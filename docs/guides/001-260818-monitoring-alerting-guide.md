# 001-260818-monitoring-alerting-guide.md

## 개요
- **목적**: SH Platform 모니터링 + 로깅 + 알림의 통합 가이드
- **대상**: 서버 운영자 (자원/서비스/로그 상태 확인, 알림 설정)
- **작성일**: 2026-08-18
- **관련 설치 문서**: [로깅 설치](logging-install.md), [로깅 개념](logging-concept.md), [Grafana 실습](grafana-practical-guide.md)

---

## 1. 시스템 구성도

```
                ┌─ Grafana (대시보드 + 알림) :3000
                │       │
    ┌───────────┼───────┼───────────────┐
    ▼           ▼       ▼               ▼
Prometheus   Loki   (알림 엔진)     Grafana Alerting
  :9090      :3100                    (알림 설정)
    │           │
    │           └─ Promtail ← Spring Boot 로그, nginx 로그, syslog
    │
    ├── Node Exporter   :9100  ← 서버 OS (CPU/RAM/Disk/Net)
    ├── Spring Boot     :8080~8083  ← JVM/API/HTTP 메트릭 (Actuator)
    └── mysqld_exporter :9104  ← MariaDB 메트릭
```

| 컴포넌트 | 포트 | 역할 | 수집/저장 |
|----------|------|------|-----------|
| Grafana | :3000 | 대시보드 시각화 + 알림 (통합 뷰) | 설정은 Grafana DB |
| Prometheus | :9090 | 메트릭 수집/저장 | 스크랩 주기 15s |
| Loki | :3100 | 로그 저장/검색 | Promtail이 push |
| Promtail | - | 로그 파일 → Loki 전송 | `/etc/promtail/promtail-config.yaml` |
| Node Exporter | :9100 | 서버 메트릭 | `/etc/prometheus/prometheus.yml` |
| mysqld_exporter | :9104 | MariaDB 메트릭 | 위 Prometheus 설정 |

## 2. 현재 구축 상태 요약

| 영역 | 상태 | 확인 방법 |
|------|------|-----------|
| 서버 자원 (CPU/MEM/Disk/Net) | ✅ | Grafana → node-exporter 대시보드 |
| Spring Boot (HTTP/5xx/JVM) | ✅ | Grafana → Spring 대시보드 / `/actuator/prometheus` |
| DB (커넥션/쿼리/slow query) | ✅ | Grafana → mysqld_exporter 대시보드 |
| 로그 (에러/접근) | ✅ | Grafana → Explore → Loki |
| 알림 (자원/서비스/로그) | ⚠️ 미설정 | Grafana → Alerting |

## 3. 접속 경로

| 도구 | URI |
|------|-----|
| Grafana | https://sunghoonyk.duckdns.org/grafana/ (admin/admin, 변경 권장) |
| Prometheus | https://sunghoonyk.duckdns.org/prometheus/ |

## 4. 서비스 관리

```bash
# 상태 확인
sudo systemctl status prometheus grafana-server prometheus-node-exporter mysqld-exporter loki promtail

# 재시작
sudo systemctl restart prometheus
sudo systemctl restart grafana-server
sudo systemctl restart prometheus-node-exporter
sudo systemctl restart mysqld-exporter
sudo systemctl restart loki
sudo systemctl restart promtail
```

## 5. 설정 파일 위치

| 파일 | 경로 |
|------|------|
| Prometheus 설정 | `/etc/prometheus/prometheus.yml` |
| Grafana 설정 | `/etc/grafana/grafana.ini` |
| Grafana Datasource provisioning | `/etc/grafana/provisioning/datasources/` |
| Loki 설정 | `/etc/loki/loki-config.yaml` |
| Promtail 설정 | `/etc/promtail/promtail-config.yaml` |
| nginx | `/etc/nginx/sites-available/sh-platform` |

---

## 6. Grafana에서 로그 조회 (Loki)

### 6.1 Explore에서 로그 검색

1. Grafana 접속 → 좌측 메뉴 → **Explore**
2. 상단에서 **Loki** 선택
3. 쿼리 입력 → **Run query**

### 6.2 기본 쿼리

```logql
# 모든 spring-boot 로그
{job="spring-boot"}

# 모든 nginx 로그
{job="nginx"}

# nginx access / error 로그만
{job="nginx", type="access"}
{job="nginx", type="error"}

# syslog
{job="syslog"}
```

### 6.3 필터링

```logql
# ERROR 로그만
{job="spring-boot"} |= "ERROR"

# DEBUG 제외
{job="spring-boot"} != "DEBUG"

# timeout 또는 error 포함
{job="spring-boot"} |~ "timeout|error"

# 대소문자 구분 없이
{job="spring-boot"} |~ "(?i)error"
```

### 6.4 LogQL 연산자

| 연산자 | 의미 | 예시 |
|--------|------|------|
| `|=` | 문자열 포함 | `|= "ERROR"` |
| `|~` | 정규식 매칭 | `|~ "timeout\|error"` |
| `!=` | 문자열 불포함 | `!= "DEBUG"` |
| `!~` | 정규식 불매칭 | `!~ "health"` |

파이프라인: `{job="spring-boot"} |= "ERROR" != "DEBUG"` / `| logfmt | level="error"` / `| json | status >= 500`

### 6.5 실용적 쿼리 모음

**Spring Boot**
```logql
# 모든 에러 / WARN 이상
{job="spring-boot"} |= "ERROR"
{job="spring-boot"} |~ "WARN|ERROR|FATAL"

# DB / JWT 인증 에러
{job="spring-boot"} |~ "SQLException|DataAccessException|timeout"
{job="spring-boot"} |~ "InvalidToken|TokenExpired|Authentication"

# 최근 5분간 에러 수
sum(rate({job="spring-boot"} |= "ERROR" [5m]))
```

**nginx (이상 접근/오류)**
```logql
# 5xx 에러
{job="nginx"} |= "500" or |= "502" or |= "503" or |= "504"

# 4xx 에러
{job="nginx"} |~ " [45]\\d{2} "

# 특정 경로 / 특정 IP 접근
{job="nginx"} |= "/api/v1/auth"
{job="nginx"} |= "192.168.1.100"
```

**시스템 (인증/변경)**
```logql
# 인증 실패
{job="auth"} |= "Failed password"

# systemd 서비스 상태 변경
{job="syslog"} |~ "Started|Stopped|Failed"
```

### 6.6 메트릭과 로그 연동

1. Explore → Loki → 로그 쿼리 실행
2. 상단 **Metric** 버튼 클릭 → 로그를 메트릭으로 변환
3. `sum(rate({job="spring-boot"} |= "ERROR" [5m])) by (job)`

---

## 7. 알림(Alert) 설정

### 7.1 SMTP 설정 (1회, 필수 전제)

`/etc/grafana/grafana.ini`에 추가:

```ini
[smtp]
enabled = true
host = smtp.gmail.com:587
user = {MAIL_USERNAME}            # .env의 MAIL_USERNAME과 동일
password = {MAIL_PASSWORD}
from_address = noreply@shplatform.com
```

```bash
sudo systemctl restart grafana-server
```

### 7.2 알림 채널 (Contact points)

1. Grafana → ⚙️ **Administration → Alerting → Contact points**
2. **Add contact point** → Name: `Email` / Type: `Email` / 수신 이메일 주소 입력
3. **Test** 버튼으로 발송 확인 (SMTP 설정 검증)

### 7.3 알림 규칙 생성 (Alert rules)

1. **Alert rules** 탭 → **New alert rule**
2. 데이터소스 선택 (Prometheus 또는 Loki)
3. 쿼리 + 조건 입력
4. **Evaluate**: Evaluate every `1m` / For: `표의 지속시간`
5. **Save rule and exit**

### 7.4 알림 기준표 (대중적 기준 + 서버 사양 반영)

> 서버: OCI 단일 코어, RAM 5.77GB, JVM 힙 768m×3 + 1024m×1

#### 7.4.1 서버 수준 (Prometheus — node-exporter)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 |
|---|--------|---------------|------|------|--------|
| 1 | Server Offline | `up{job="node-exporter"}` | `== 0` | 즉시 | 🔴 Critical |
| 2 | CPU 90% 초과 | `100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | `> 90` | 5분 | 🟡 Warning |
| 3 | Memory 90% 초과 | `(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100` | `> 90` | 5분 | 🟡 Warning |
| 4 | Disk 85% 초과 | `(1 - node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100` | `> 85` | 5분 | 🟡 Warning |
| 5 | Load 1분 초과 | `node_load1` | `> 4` (단일코어) | 10분 | 🟡 Warning |

#### 7.4.2 서비스 수준 (Prometheus — Spring Boot)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 |
|---|--------|---------------|------|------|--------|
| 6 | 서비스 다운 | `up{job="sh-platform-auth"}` (각 서비스) | `== 0` | 1분 | 🔴 Critical |
| 7 | 5xx 에러 증가 | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))` | `> 0.05` | 5분 | 🟡 Warning |
| 8 | 응답 지연 P95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | `> 2s` | 5분 | 🟡 Warning |

> `job` 라벨은 `prometheus.yml`의 실제 스크랩 설정과 일치해야 함.

#### 7.4.3 로그 수준 (Loki — LogQL)

| # | 알림명 | 쿼리 (LogQL) | 조건 | 지속 | 심각도 |
|---|--------|--------------|------|------|--------|
| 9 | 앱 에러 급증 | `sum(rate({job="spring-boot"} |= "ERROR" [5m]))` | `> 0.1` (초당) | 5분 | 🟡 Warning |
| 10 | 인증 실패 급증 | `sum(rate({job="auth"} |= "Failed" [5m]))` | `> 0.05` | 5분 | 🔴 Critical |
| 11 | nginx 5xx 증가 | `sum(rate({job="nginx"} |= "500" [5m]))` | `> 0.01` | 5분 | 🟡 Warning |
| 12 | 이상 접근 (403/경로) | `sum(rate({job="nginx"} |= "/api/v1/auth" [5m]))` | 사용자 정의 | 5분 | 🟡 Warning |

#### 7.4.4 DB 수준 (Prometheus — mysqld_exporter)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 |
|---|--------|---------------|------|------|--------|
| 13 | DB 커넥션 풀 소진 | `mysql_global_status_threads_connected` | `> 200` | 5분 | 🟡 Warning |
| 14 | Slow Query 증가 | `rate(mysql_global_status_slow_queries[5m])` | `> 0.1` | 10분 | 🟡 Warning |

### 7.5 Notification policies

- Alerting → **Notification policies** → 규칙 그룹을 `Email` 채널에 연결
- Default policy를 `Email`로 지정하면 모든 규칙이 이메일로 발송됨

### 7.6 적용 우선순위

| 단계 | 항목 | 이유 |
|------|------|------|
| 1 | SMTP 설정 + 이메일 테스트 | 모든 알림의 전제 |
| 2 | Server Offline | 서버 사망 감지 최우선 |
| 3 | 서비스 다운 (`up == 0`) | 재시작 루프 등 잡을 수 있음 |
| 4 | Disk 85% | DB/로그 디스크 풀림 예방 |
| 5 | Memory 90% | JVM 힙 상향 필요 감지 |
| 6 | 로그 기반 알림 (에러/인증실패) | 앱 이상 탐지 |

---

## 8. 문제 해결 (트러블슈팅)

### 8.1 Grafana "failed to load its application files"

**원인**: `/grafana/` 서브패스 프록시 시 설정 누락
**해결**: `/etc/grafana/grafana.ini`
```ini
[server]
domain = sunghoonyk.duckdns.org
root_url = https://sunghoonyk.duckdns.org/grafana/
serve_from_sub_path = true
```
```bash
sudo systemctl restart grafana-server
```

### 8.2 Grafana 무한 리다이렉트 (301 루프)

**원인**: nginx `proxy_pass` trailing slash로 경로 잘림
**해결**:
```nginx
# 수정 전 (문제)
location /grafana/ { proxy_pass http://127.0.0.1:3000/; }  # trailing slash → 경로 잘림
# 수정 후 (해결)
location /grafana/ { proxy_pass http://127.0.0.1:3000; }   # 경로 보존
```
```bash
sudo nginx -t && sudo systemctl reload nginx
```

### 8.3 Spring Boot Actuator 차단

**증상**: `/actuator/prometheus` 접근 시 302 리다이렉트
**해결**: SecurityConfig의 permitAll에 `/actuator/health`, `/actuator/prometheus`, `/actuator/info`, `/actuator/metrics` 추가

### 8.4 Prometheus 타겟 down

```bash
curl http://localhost:9100/metrics | head    # Node Exporter
curl http://localhost:8080/actuator/prometheus | head   # Spring Boot
curl http://localhost:9104/metrics | head    # mysqld_exporter
```

### 8.5 Grafana "No data"

1. Explore → Prometheus 선택 → 쿼리로 데이터 존재 확인
2. 타임피커 시간 범위 확인

### 8.6 Loki 관련

```bash
# 상태
sudo systemctl status loki promtail
curl -s http://localhost:3100/ready

# Promtail 위치 파일
cat /var/lib/promtail/positions.yaml

# 로그 파일 존재 확인
ls -la /home/ubuntu/sh-platform/logs/
ls -la /var/log/nginx/
```

### 8.7 CLI에서 로그 조회 (logcli)

```bash
logcli labels
logcli query {job="spring-boot"} |= "ERROR"
logcli query --since=1h {job="nginx"} |= "500"
```

---

## 9. 로그 보관 정책

| 로그 유형 | 보관 기간 | 설정 위치 |
|-----------|-----------|-----------|
| Spring Boot | 7일 | Loki compactor |
| nginx | 7일 | Loki compactor |
| syslog | 7일 | Loki compactor |

---

## 10. 관련 문서

- [로깅 설치 가이드](logging-install.md) — Loki/Promtail 설치
- [로깅 개념](logging-concept.md) — 중앙 로깅 개념
- [Grafana 실습 가이드](../grafana-practical-guide.md) — CPU/메모리/디스크 패널, 대시보드 임포트, 알림 실습
- [Grafana 사용 가이드](grafana-guide.md) — Grafana 기본 사용법

---
*작성일: 2026-08-18*