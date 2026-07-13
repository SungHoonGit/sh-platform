# Grafana 사용 가이드

## 1. 접속

```
URL:  https://sunghoonyk.duckdns.org/grafana/login
ID:   admin
PW:   admin (초기 비밀번호 → 로그인 후 변경 권장)
```

---

## 2. 대시보드란?

Prometheus에서 수집한 메트릭(CPU, 메모리 등)을 **그래프, 게이지, 테이블** 등으로 시각화하는 화면.

```
Prometheus (데이터 수집) → Grafana (시각화 대시보드)
```

---

## 3. 기본 개념

| 항목 | 설명 |
|------|------|
| **Dashboard** | 대시보드 (여러 패널을 모은 화면) |
| **Panel** | 개별 그래프/차트 (예: CPU 사용률) |
| **Datasource** | 데이터 소스 (여기서는 Prometheus) |
| **Query** | Prometheus 쿼리 (PromQL) |

---

## 4. 첫 번째 대시보드 만들기

### 4.1 Datasource 확인

1. 좌측 메뉴 → ⚙️ Connections → Data sources
2. `Prometheus` 가 등록되어 있는지 확인
3. URL: `http://localhost:9090` → Save & Test 클릭 → "Data source is working" 확인

### 4.2 새 대시보드 생성

1. 좌측 메뉴 → ➕ Create → Dashboard
2. **Add visualization** 클릭
3. Data source: `Prometheus` 선택
4. Query 창에 PromQL 입력

### 4.3 자주 쓰는 PromQL 쿼리

```promql
# CPU 사용률 (%)
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 메모리 사용률 (%)
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# 디스크 사용률 (%)
(node_filesystem_size_bytes - node_filesystem_avail_bytes) / node_filesystem_size_bytes * 100

# 디스크 사용량 (GB)
node_filesystem_size_bytes / 1024 / 1024 / 1024

# 네트워크 수신 (bytes/sec)
rate(node_network_receive_bytes_total[5m])

# 네트워크 송신 (bytes/sec)
rate(node_network_transmit_bytes_total[5m])

# 시스템 업타임 (초)
node_time_seconds - node_boot_time_seconds

# Spring Boot JVM 헙 사용률 (%)
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# Spring Boot HTTP 요청 수 (req/sec)
rate(http_server_requests_seconds_count[5m])

# HikariCP 커넥션 풀 사용량
hikaricp_connections_active
```

### 4.4 패널 설정

| 설정 | 권장값 |
|------|--------|
| **Title** | 패널 이름 (예: "CPU 사용률") |
| **Visualization** | Time series (시계열 그래프) |
| **Unit** | Percent (0-100) for CPU/RAM |
| **Min/Max** | 0 / 100 (퍼센트용) |
| **Legend** | `{{instance}}` 또는 `{{job}}` |

---

## 5. 기본 대시보드 구성 (추천)

### 5.1 인프라 대시보드

| 패널 | 쿼리 | Unit |
|------|------|------|
| CPU 사용률 | `100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | Percent |
| 메모리 사용률 | `(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100` | Percent |
| 디스크 사용률 | `(node_filesystem_size_bytes - node_filesystem_avail_bytes) / node_filesystem_size_bytes * 100` | Percent |
| 네트워크 수신 | `rate(node_network_receive_bytes_total[5m])` | Bps (bytes/sec) |
| 시스템 업타임 | `node_time_seconds - node_boot_time_seconds` | s (seconds) |

### 5.2 Spring Boot 대시보드

| 패널 | 쿼리 | Unit |
|------|------|------|
| JVM 헙 사용률 | `jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100` | Percent |
| HTTP 요청수 | `rate(http_server_requests_seconds_count[5m])` | reqps (req/sec) |
| HTTP 응답시간 | `rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])` | s (seconds) |
| HikariCP 커넥션 | `hikaricp_connections_active` | short |

---

## 6. 변수(Variable) 사용법

대시보드 상단에 드롭다운 필터를 만들 수 있음.

### 설정 방법

1. 대시보드 → ⚙️ Dashboard settings → Variables → New variable
2. 설정:
   - Name: `job`
   - Type: Query
   - Query: `label_values(node_cpu_seconds_total, job)`
3. 패널 쿼리에서 `$job` 으로 참조

```
# 예시: job별 CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle", job="$job"}[5m])) * 100)
```

---

## 7. 알림(Alert) 설정

### 7.1 알림 규칙 생성

1. 대시보드 → 패널 클릭 → Edit
2. 상단 탭: Alert
3. **Create alert rule** 클릭

### 7.2 알림 조건 예시

```
# CPU 80% 이상일 때 알림
Condition: query(A, 5m, now) > 80
Evaluate every: 1m
For: 5m (5분간 지속 시 알림)
```

### 7.3 알림 채널

1. ⚙️ Alerting → Contact points
2. 추가: Email, Slack, webhook 등

---

## 8. 유용한 기능

### 8.1 Explore (메트릭 탐색)

1. 좌측 메뉴 → 🔍 Explore
2. Prometheus 선택
3. PromQL을 직접 입력해서 메트릭 확인
4. **메트릭 이름 자동 완성** 지원

### 8.2 Notebook (쿼리 노트)

1. Explore → New notebook
2. 쿼리 결과를 메모장처럼 저장

### 8.3 대시보드 임포트 (공유)

1. ➕ Create → Import
2. Grafana.com 대시보드 ID 입력
3. 예시 ID:
   - `1860` — Node Exporter Full (서버 모니터링)
   - `12900` — Spring Boot Statistics

---

## 9. 자주 쓰는 단축키

| 단축키 | 기능 |
|--------|------|
| `d` | 대시보드 목록 |
| `e` | Explore |
| `n` | 새 대시보드 |
| `p` | 패널 편집 |
| `f` | 전체화면 |
| `⌘/ctrl + S` | 대시보드 저장 |
| `⭐` | 대시보드 즐겨찾기 |

---

## 10. 문제 해결

### Q: "No data" 표시
- Prometheus datasource가 제대로 연결되어 있는지 확인
- Explore에서 쿼리를 직접 실행하여 데이터 존재 여부 확인
- 시간 범위가 올바른지 확인 (우측 상단 타임피커)

### Q: 그래프가 안 나옴
- PromQL 문법 오류 확인
- 라벨 이름이 올바른지 확인 (`label_values()` 로 확인)

### Q: 권한 에러
- admin 계정으로 로그인 확인
- Organizations → Members에서 역할 확인

---

## 11. 관련 문서

- [Prometheus 공식 문서](https://prometheus.io/docs/)
- [Grafana 공식 문서](https://grafana.com/docs/)
- [PromQLcheatsheet](https://promlabs.com/promql-cheat-sheet/)
- [모니터링 설정 가이드](monitoring-guide.md)
