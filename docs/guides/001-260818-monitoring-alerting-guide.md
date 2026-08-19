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
| 알림 (자원/서비스) | ✅ 전 규칙 등록 | Grafana → Alerting |
| 알림 (로그/DB) | ✅ 전 규칙 등록 | Grafana → Alerting |

> **등록 현황 (2026-08-18 완료)**: 1~13번 전 규칙 Grafana UI에 등록 완료. 전 규칙 **No data 상태 = Normal(OK)** 처리 완료 → 평소 조용, 이상 발생 시에만 메일. Server Offline ❌ 제거 (단일 서버에서 무의미). 메일 발송 확인됨 (DatasourceNoData 알림 수신으로 SMTP 동작 검증).

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

> **nginx 로그 형식 (2026-08-18 확인)**: JSON이 아니라 **combined 포맷** (`IP - - [날짜] "METHOD 경로 PROTO" 상태코드 ...`). 라벨은 `job=nginx`, `type=access|error`, `service_name=nginx`, `filename` 만 존재 — `status` 라벨이 없어 `sum by (status)`가 바로 안 됨. 상태코드별 집계는 `| pattern` 파서로 추출:
>
> ```logql
> # 상태코드별 분포 (패턴 파서 사용)
> sum by (status) (count_over_time({job="nginx", type="access"} | pattern `<_ip> - - [<_time>] "<_method> <_path> <_proto>" <status> <_size> "<_referer>" "<_ua>"` [5m]))
>
> # 상태코드별 시계열
> sum by (status) (rate({job="nginx", type="access"} | pattern `<_ip> - - [<_time>] "<_method> <_path> <_proto>" <status> <_size> "<_referer>" "<_ua>"` [5m]))
> ```
>
> `<이름>` 자리에 로그 값이 매칭되고 그 이름이 라벨이 됨 → 그 뒤로 `sum by (status)` 사용 가능.

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

### 6.7 Spring Boot Log 대시보드 (Loki)

> **구성 (2026-08-19)**: 서비스 선택 변수 + 패널 4개. 각 패널은 **쿼리 1개**만 넣는다 (여러 쿼리 섞으면 빈 화면). 최종 구성: 실시간 로그 목록(Logs), 로그 레벨 분포(Bar chart), ERROR 로그 수(Stat), 로그 수 추이(Time series).

#### 6.7.1 서비스 변수 (드롭다운)

1. 대시보드 → **Settings → Variables → + New variable**
2. 설정 (Label values 방식):
   - Name: `service`
   - Type: **Query** → Query type: **Label values**
   - Data source: **Loki**
   - **Label**: `service` (⚠ `job` 아님)
   - **Stream selector**: `{job="spring-boot"}` (⚠ `label_values(...)` 함수를 넣지 말 것 — Label values 모드에서는 스트림만 입력)
3. Refresh: `On dashboard load` → 저장 → 대시보드 상단에 드롭다운 생성

> **실패 사례**: `Label values` 모드에서 Label=`job`, Stream selector=`label_values({job="spring-boot"}, service)`로 넣으면 값이 0개. **Label=`service`**, **Stream selector=`{job="spring-boot"}`** 가 정답.

#### 6.7.2 패널 1 — 실시간 로그 목록 (Logs)

- 쿼리: `{job="spring-boot", service=~"$service"}`
- Visualization: **Logs**
- 기본 필터로 사용 (서비스 선택에 따라 해당 서비스 로그만 표시)

#### 6.7.3 패널 2 — 서비스별 로그 수 (Bar chart)

- 쿼리:
```logql
sum by (service) (count_over_time({job="spring-boot"} [5m]))
```
- Visualization: **Bar chart**

#### 6.7.4 패널 3 — ERROR 로그 수 (Stat)

- 쿼리:
```logql
sum by (service) (count_over_time({job="spring-boot"} |= "ERROR" [5m]))
```
- Visualization: **Stat**

#### 6.7.6 패널 2 대체 — 로그 레벨 분포 (Bar chart)

> 서비스별 로그 수 대신 **레벨(info/warn/error) 분포**를 보는 게 WAS 모니터링에 더 유용. Loki가 로그에서 `detected_level` 라벨을 자동 추출하므로 바로 사용 가능.

- 쿼리:
```logql
sum by (detected_level) (count_over_time({job="spring-boot", service=~"$service"} [5m]))
```
- Visualization: **Bar chart**
- 0건 레벨도 항상 표시하려면 (레벨 3개 고정):
```logql
sum by (detected_level) (count_over_time({job="spring-boot", service=~"$service"} [5m]))
or on() label_replace(vector(0), "detected_level", "info", "", "")
or on() label_replace(vector(0), "detected_level", "warn", "", "")
or on() label_replace(vector(0), "detected_level", "error", "", "")
```
- **색상 레벨별 고정**: 패널 Edit → 우측 하단 **Add field override** → Match(name)=`error` → Add override property → **Standard options → Color → Fixed color** → 빨강 (`warn`=주황, `info`=파랑도 동일 반복)
  - ⚠ 숫자 Thresholds(90/10 등)는 **건수(값) 기준** 색칠이라 레벨 이름별 색상에는 부적합. **Field overrides**로 해야 함.
  - Grafana 11에서 Overrides는 우측 패널 맨 아래 `Add field override`.

#### 6.7.5 패널 4 — 로그 수 추이 (Time series)

- 쿼리:
```logql
sum(rate({job="spring-boot"}[5m]))
```
- Visualization: **Time series**

> **시간 범위**: 대시보드 우측 상단 타임피커 하나로 전체 패널이 따라감. 패널이 빈 화면이면 우측 상단 시간을 **Last 15 minutes**로 먼저 확인.

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
4. **Set evaluation behavior**: Evaluate every `1m` / For: `표의 지속시간`
5. **Configure no data and error handling** (접혀 있음 → 펼침):
   - `Alert state if no data` → **`Normal`** (구버전은 `OK`)
   - `Alert state if execution error` → **`Normal`** (구버전은 `OK`)
   - **Missing series evaluations to resolve**: 기본 `2` 유지
6. **Save rule and exit**

> **중요 (DatasourceNoData 알림 함정)**: No data 상태를 `Normal`로 설정하지 않으면, **데이터가 없는 게 정상인 규칙**(로그 기반 규칙 8~11, 5xx 6번, slow query 13번)이 평소에 계속 발화해서 **"DatasourceNoData" 알림 메일이 반복 수신**됩니다. 이것은 실제 장애가 아니라 "쿼리에 데이터가 없다"는 알림이므로, 로그 규칙 등 평소 No data가 정상인 규칙은 반드시 `Normal`로 설정해야 합니다. Grafana 11 이상은 `OK`라는 명칭이 `Normal`로 바뀌었습니다.

### 7.4 알림 기준표 (대중적 기준 + 서버 사양 반영)

> 서버: OCI 단일 코어, RAM 5.77GB, JVM 힙 768m×3 + 1024m×1

#### 7.4.1 서버 수준 (Prometheus — node-exporter)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 | Summary |
|---|--------|---------------|------|------|--------|---------|
| 1 | CPU 90% 초과 | `100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | `> 90` | 5분 | 🟡 Warning | `서버 CPU가 90%를 5분 이상 초과했습니다` |
| 2 | Memory 90% 초과 | `(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100` | `> 90` | 5분 | 🟡 Warning | `서버 메모리가 90%를 5분 이상 초과했습니다` |
| 3 | Disk 85% 초과 | `(1 - node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100` | `> 85` | 5분 | 🟡 Warning | `루트 디스크 사용률이 85%를 초과했습니다` |
| 4 | Load 1분 초과 | `node_load1` | `> 4` (단일코어) | 10분 | 🟡 Warning | `서버 부하(load1)가 4를 10분 이상 초과했습니다` |

> **Server Offline 알림은 미사용 (제거됨)**: 단일 서버 구성에서는 서버가 꺼지면 Grafana도 함께 죽어 메일 발송이 불가능. 실질적으로 node-exporter만 죽은 희귀 케이스만 잡음. 진짜 서버 사망 감지는 외부 서비스(UptimeRobot 등)로 대체 예정.

#### 7.4.2 서비스 수준 (Prometheus — Spring Boot)

> 실제 Prometheus job 구조: `job="spring-boot"`, 서비스는 `instance` 라벨로 구분 (localhost:8080~8083)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 | Summary |
|---|--------|---------------|------|------|--------|---------|
| 5 | 서비스 다운 (통합) | `up{job="spring-boot"}` | `== 0` | 1분 | 🔴 Critical | `Spring Boot 서비스 다운 ({{ $labels.instance }})` |
| 6 | 5xx 에러 증가 | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))` | `> 0.05` | 5분 | 🟡 Warning | `HTTP 5xx 에러율이 5%를 초과했습니다` |
| 7 | 응답 지연 P95 | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | `> 2` | 5분 | 🟡 Warning | `응답 시간 P95가 2초를 초과했습니다` |

> **P95 규칙 `is above` 값은 초(seconds) 단위** — `2` = P95 응답시간 2초 초과 시 알림. 요구사항에 따라 `1`(엄격)~`5`(여유)로 조정. 이 규칙은 히스토그램 활성화 필요 (`management.metrics.distribution.percentiles-histogram.http.server.requests: true`)

> 서비스 다운 규칙은 `up{job="spring-boot"}` 하나로 4개 서비스(instance 별)를 모두 감지. 죽은 인스턴스만 발화됨.

#### 7.4.3 로그 수준 (Loki — LogQL)

| # | 알림명 | 쿼리 (LogQL) | 조건 | 지속 | 심각도 | Summary |
|---|--------|--------------|------|------|--------|---------|
| 8 | 앱 에러 급증 | `sum(rate({job="spring-boot"} |= "ERROR" [5m]))` | `> 0.1` (초당) | 5분 | 🟡 Warning | `Spring Boot 에러 로그가 급증했습니다` |
| 9 | 인증 실패 급증 | `sum(rate({job="auth"} |= "Failed" [5m]))` | `> 0.05` | 5분 | 🔴 Critical | `인증 실패가 급증했습니다 (무차별 대입 의심)` |
| 10 | nginx 5xx 증가 | `sum(rate({job="nginx"} |= "500" [5m]))` | `> 0.01` | 5분 | 🟡 Warning | `nginx 5xx 응답이 증가했습니다` |
| 11 | 이상 접근 (403/경로) | `sum(rate({job="nginx"} |= "/api/v1/auth" [5m]))` | 사용자 정의 | 5분 | 🟡 Warning | `이상 접근 패턴이 감지되었습니다` |

#### 7.4.4 DB 수준 (Prometheus — mysqld_exporter)

> **데이터소스는 반드시 Prometheus** — `mysql_global_status_threads_connected` 같은 메트릭은 mysqld_exporter(:9104)가 MariaDB에서 뽑아 Prometheus로 노출하기 때문. Grafana의 **MySQL 데이터소스**(직접 DB 연결용)를 선택하면 "failed to connect to server" 오류가 남.
>
> **의미**: WAS(HikariCP) 기준이 아니라 **DB(MariaDB) 입장의 연결 수**. WAS들이 동시에 재시작하며 커넥션을 몰고 가면 DB 커넥션 수가 치솟으므로, 이 규칙이 DB 과부하를 조기에 잡는 역할.

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 | Summary |
|---|--------|---------------|------|------|--------|---------|
| 12 | DB 커넥션 풀 소진 | `mysql_global_status_threads_connected` | `> 200` | 5분 | 🟡 Warning | `DB 커넥션 수가 200을 초과했습니다` |
| 13 | Slow Query 증가 | `rate(mysql_global_status_slow_queries[5m])` | `> 0.1` | 10분 | 🟡 Warning | `Slow Query 발생률이 증가했습니다` |

### 7.5 Notification policies

- Alerting → **Notification policies** → 규칙 그룹을 `Email` 채널에 연결
- Default policy를 `Email`로 지정하면 모든 규칙이 이메일로 발송됨

### 7.6 적용 우선순위

| 단계 | 항목 | 이유 |
|------|------|------|
| 1 | SMTP 설정 + 이메일 테스트 | 모든 알림의 전제 |
| 2 | 서비스 다운 (`up == 0`) | 재시작 루프 등 잡을 수 있음 |
| 3 | Disk 85% | DB/로그 디스크 풀림 예방 |
| 4 | Memory 90% | JVM 힙 상향 필요 감지 |
| 5 | 로그 기반 알림 (에러/인증실패) | 앱 이상 탐지 |

> **서버 사망 감지(Server Offline)는 알림으로 대체 불가** — 서버가 꺼지면 Grafana도 죽어 메일 발송 불가. 외부 uptime 서비스(UptimeRobot 등) 도입 시 감지 가능.

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

### 8.6.1 Promtail `service` 라벨이 `*`로 들어감

**증상**: `curl -s http://localhost:3100/loki/api/v1/label/service/values` 결과가 `["*"]` → Grafana 변수 드롭다운이 `*`만 표시.

**원인**: glob 경로에 relabel 정규식을 적용하면 **파일명이 아니라 glob 패턴(`logs/*/*.log`)에서 `*`를 캡처**함:
```yaml
relabel_configs:
  - source_labels: [__path__]
    regex: '.*/logs/([^/]+)/.*\.log'   # ⚠ glob 패턴이면 "logs/*/*.log"의 * 를 캡처 → service="*"
    target_label: service
```

**해결**: 정규식 파싱 대신 **서비스별 static_configs 분리 + 정적 라벨** 사용:
```yaml
  - job_name: spring-boot-auth
    static_configs:
      - targets: [localhost]
        labels:
          job: spring-boot
          service: auth-platform
          __path__: /home/ubuntu/sh-platform/logs/auth-platform/*.log
  # ... scraper-platform / resume-platform / portfolio-platform 동일 패턴
```

**검증**:
```bash
sudo systemctl restart promtail
sudo systemctl restart sh-platform-auth   # 새 로그 발생시켜 새 스트림 생성
curl -s 'http://localhost:3100/loki/api/v1/label/service/values' | python3 -m json.tool
# ["*", "auth-platform", "scraper-platform"] → "*"는 옛 스트림(과거 데이터), 새 스트림 정상
```

> `*`는 설정 변경 전 옛 스트림에 남은 데이터라 새로 쌓이지 않음. 그래프/변수에서 제외하려면 `service=~".+"` 필터 사용. Loki 보관 기간이 지나면 자연히 사라짐.

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

### 9.1 보관 기간 설정 방법 (Loki retention)

Loki는 로그 보관 주기를 **compactor**가 관리하며 `limits_config.retention_period`로 정합니다.

```yaml
# /etc/loki/loki-config.yaml
limits_config:
  retention_period: 168h   # 7일 (720h = 30일)
  # retention_enabled는 v2.9+ 에서 기본 true

compactor:
  retention_enabled: true
  # 작업 주기: 기본 10분마다 보관 기간 초과 청크 삭제
```

```bash
# 현재 설정 확인
grep -A5 "compactor\|retention" /etc/loki/loki-config.yaml

# 변경 후 적용
sudo systemctl restart loki
```

**고려사항**:
- 7일이면 에러 로그 트러블슈팅에 충분 (대부분 당일~3일 이내 해결)
- 보관 기간을 늘리면 디스크 사용량 증가 → `df -h`로 여유 확인 후 결정
- 현재 서버는 Loki retention 기본값으로 두고, 디스크 85% 알림(3번)이 울리면 조정하면 됨

### 9.2 로그 기반 알림의 "No data"는 정상

로그 알림 규칙(8~11번)은 **평소에 No data일 수 있습니다** — 해당 로그(ERROR, 인증 실패, `/api/v1/auth` 접근 등)가 없으면 당연히 데이터가 없습니다. 실제 이상이 발생하는 순간에만 알림이 발화하므로, No data를 "문제"로 보지 않습니다.

No data가 실제 쿼리 문제인지 확인하는 법:
```bash
curl -s -G 'http://localhost:3100/loki/api/v1/query_range' \
  --data-urlencode 'query={job="nginx"} |= "/api/v1/auth"' \
  --data-urlencode 'limit=3' | python3 -m json.tool
```
로그가 쌓이는데 No data면 쿼리 문제, 로그 자체가 없으면 정상입니다.

---

## 10. 관련 문서

- [로깅 설치 가이드](logging-install.md) — Loki/Promtail 설치
- [로깅 개념](logging-concept.md) — 중앙 로깅 개념
- [Grafana 실습 가이드](../grafana-practical-guide.md) — CPU/메모리/디스크 패널, 대시보드 임포트, 알림 실습
- [Grafana 사용 가이드](grafana-guide.md) — Grafana 기본 사용법

---
*작성일: 2026-08-18*