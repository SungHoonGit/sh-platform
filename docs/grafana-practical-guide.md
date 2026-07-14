# Grafana 모니터링 실습 가이드

> 이 문서는 Grafana에서 실제로 대시보드를 만들고 모니터링을 설정하는 과정을
> 한 단계씩 설명합니다.

---

## 1. Grafana 접속

### 1.1 브라우저로 접속

```
URL:  https://sunghoonyk.duckdns.org/grafana/login
```

### 1.2 로그인

- ID: `admin`
- 비밀번호: 초기 설정한 비밀번호
- 첫 로그인 시 비밀번호 변경 안내가 나오면 변경 또는 Skip

### 1.3 메인 화면 이해

로그인 후 보이는 화면:

```
좌측 사이드바:
  ⚙️ Home          → 홈 대시보드
  ➕ Create         → 새 대시보드/패널 만들기
  🔍 Explore        → 쿼리 테스트 (메트릭/로그 직접 확인)
  ⭐ Starred        → 즐겨찾기한 대시보드
  📂 Dashboards     → 대시보드 목록
  ⚙️ Administration → 설정

우측 상단:
  🔔 알림 아이콘
  👤 프로필 (로그아웃)
  ⏰ 타임피커 (시간 범위 선택)
```

---

## 2.Datasource 확인 (데이터 연결)

대시보드를 만들기 전, 데이터가 제대로 들어오는지 확인합니다.

### 2.1 Prometheus 확인

1. 좌측 사이드바 → **Explore** 클릭
2. 상단 드롭다운에서 **Prometheus** 선택
3. 다음 쿼리를 입력하고 **Run query** 클릭:

```
up
```

4. 결과가 `1`로 표시되면 정상:
   - `up{job="prometheus"} 1` → Prometheus 자체
   - `up{job="node-exporter"} 1` → 서버 메트릭
   - `up{job="spring-boot"} 1` → Spring Boot 앱
   - `up{job="mysql"} 1` → MariaDB

5. 다음 쿼리로 CPU 사용률 확인:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

→ 숫자가 나오면 메트릭이 수집되고 있다는 뜻

### 2.2 Loki 확인

1. Explore → 상단 드롭다운에서 **Loki** 선택
2. 다음 쿼리 입력:

```
{job="nginx"}
```

3. 결과에 nginx 로그가 나타나면 정상

---

## 3. 첫 번째 대시보드 만들기 (CPU/메모리/디스크)

### 3.1 새 대시보드 생성

1. 좌측 사이드바 → **➕ Create** → **Dashboard** 클릭
2. 빈 대시보드 화면이 나타남
3. **Add visualization** 클릭

### 3.2 CPU 사용률 패널 만들기

#### Step 1: Data source 선택

- 패널 편집 화면에서 우측 **Data source** 드롭다운
- **Prometheus** 선택

#### Step 2: 쿼리 입력

하단 **Query** 탭에서:

- **A** 필드에 다음 입력:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

→ 그래프에 선이 나타남

#### Step 3: 패널 설정

**우측 Settings 패널:**

| 항목 | 값 | 설명 |
|------|-----|------|
| Title | `CPU 사용률 (%)` | 패널 이름 |
| Description | `서버 CPU 사용률 (0-100%)` | 설명 (호버 시 표시) |

**Visualization 설정 (좌측):**

| 항목 | 값 |
|------|-----|
| Visualization | `Time series` |
| Legend | `{{job}}` |

**Panel options → Standard options:**

| 항목 | 값 |
|------|-----|
| Unit | `Percent (0-100)` |
| Min | `0` |
| Max | `100` |
| Decimals | `1` |

#### Step 4: Apply

- 상단 우측 **Apply** 클릭
- 대시보드에 패널 추가됨
- **대시보드 저장** (우측 상단 💾 아이콘 → 이름 입력 → Save)

### 3.3 메모리 사용률 패널 추가

1. 대시보드 상단 → **Add** → **Visualization**
2. Data source: **Prometheus**
3. 쿼리 입력:

```
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100
```

4. 설정:

| 항목 | 값 |
|------|-----|
| Title | `메모리 사용률 (%)` |
| Unit | `Percent (0-100)` |
| Min | `0` |
| Max | `100` |

5. **Apply** 클릭

### 3.4 디스크 사용률 패널 추가

1. **Add** → **Visualization**
2. 쿼리:

```
(node_filesystem_size_bytes{mountpoint="/"} - node_filesystem_avail_bytes{mountpoint="/"}) / node_filesystem_size_bytes{mountpoint="/"} * 100
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `디스크 사용률 (%)` |
| Unit | `Percent (0-100)` |
| Min | `0` |
| Max | `100` |

4. **Apply** 클릭

### 3.5 네트워크 트래픽 패널 추가

1. **Add** → **Visualization**
2. 쿼리 2개 입력:

**Query A:**
```
rate(node_network_receive_bytes_total{device!="lo"}[5m])
```

**Query B:**
```
rate(node_network_transmit_bytes_total{device!="lo"}[5m])
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `네트워크 트래픽` |
| Unit | `bytes/sec (Bps)` |

4. Legend: `{{device}} - {{direction}}` (Legend 필드에 입력)
5. **Apply** 클릭

### 3.6 대시보드 저장

1. 상단 우측 💾 **Save dashboard** 클릭
2. **Title**: `서버 모니터링`
3. **Folder**: (선택 안 함 또는 새 폴더)
4. **Save** 클릭

---

## 4. Spring Boot 대시보드 만들기

### 4.1 새 대시보드 생성

1. ➕ Create → Dashboard
2. 이름: `Spring Boot 모니터링`

### 4.2 JVM 헙 사용률

1. Add visualization
2. 쿼리:

```
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `JVM 헙 사용률 (%)` |
| Unit | `Percent (0-100)` |
| Legend | `{{instance}}` |

### 4.3 HTTP 요청수 (요청/sec)

1. Add visualization
2. 쿼리:

```
rate(http_server_requests_seconds_count[5m])
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `HTTP 요청수 (req/sec)` |
| Unit | `reqps` |
| Legend | `{{method}} {{uri}}` |

### 4.4 HTTP 응답 시간

1. Add visualization
2. 쿼리:

```
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `HTTP 응답 시간 (평균)` |
| Unit | `seconds` |
| Legend | `{{method}} {{uri}}` |

### 4.5 HikariCP 커넥션 풀

1. Add visualization
2. 쿼리:

```
hikaricp_connections_active
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `HikariCP 활성 커넥션` |
| Unit | `short` |

### 4.6 에러율

1. Add visualization
2. 쿼리:

```
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)
```

3. 설정:

| 항목 | 값 |
|------|-----|
| Title | `HTTP 5xx 에러율 (req/sec)` |
| Unit | `reqps` |

---

## 5. Prometheus 대시보드 만들기

### 5.1 새 대시보드

1. 이름: `Prometheus 모니터링`

### 5.2 Prometheus 수집 메트릭 수

1. 쿼리:

```
prometheus_tsdb_head_series
```

2. 설정:

| 항목 | 값 |
|------|-----|
| Title | `수집된 시리즈 수` |
| Unit | `short` |

### 5.3 쿼리 처리 시간

1. 쿼리:

```
histogram_quantile(0.99, rate(prometheus_engine_query_duration_seconds_bucket[5m]))
```

2. 설정:

| 항목 | 값 |
|------|-----|
| Title | `쿼리 처리 시간 (99th percentile)` |
| Unit | `seconds` |

---

## 6. Grafana 대시보드 임포트 (빠른 방법)

매번 직접 만들 필요 없이, 공유된 대시보드를 가져올 수 있습니다.

### 6.1 Node Exporter Full (서버 모니터링)

1. ➕ Create → **Import**
2. **Import via grafana.com** 입력란에: `1860`
3. **Load** 클릭
4. Data source에서 **Prometheus** 선택
5. **Import** 클릭
6. 완성! 수백 개의 서버 메트릭 패널이 자동 생성됨

### 6.2 Spring Boot Statistics

1. ➕ Create → **Import**
2. 입력란에: `12900`
3. **Load** → Data source: **Prometheus** → **Import**

### 6.3 MariaDB Dashboard

1. ➕ Create → **Import**
2. 입력란에: `14057`
3. **Load** → Data source: **Prometheus** → **Import**

---

## 7. 패널 종류와 사용법

### 7.1 Time series (시계열 그래프)

- 가장 기본적인 그래프
- 시간에 따른 값 변화 표시
- 사용: CPU, 메모리, 요청수 등

### 7.2 Gauge (게이지)

- 현재 값을 원형 게이지로 표시
- 사용: CPU 사용률, 디스크 사용률 등

**설정 방법:**

1. 패널 → Visualization → **Gauge** 선택
2. Standard options:
   - Min: `0`, Max: `100`
   - Thresholds:
     - Green: 0-60
     - Yellow: 60-80
     - Red: 80-100

### 7.3 Stat (통계)

- 큰 숫자로 현재 값 표시
- 사용: 현재 연결 수, 시리즈 수 등

### 7.4 Bar chart (막대 그래프)

- 범주별 값 비교
- 사용: URI별 요청수, 에러 카운트 등

### 7.5 Table (테이블)

- 로그나 상세 데이터 표시
- 사용: 최근 에러 목록 등

---

## 8. 변수(Variable)로 필터 만들기

대시보드 상단에 드롭다운 필터를 만들어서, job별/URI별로 전환할 수 있습니다.

### 8.1 job 변수 만들기

1. 대시보드 → ⚙️ **Dashboard settings** (우측 상단 톱니바퀴)
2. 좌측 **Variables** → **New variable**
3. 설정:

| 항목 | 값 |
|------|-----|
| Name | `job` |
| Label | `Job` |
| Type | `Query` |
| Query | `label_values(node_cpu_seconds_total, job)` |
| Refresh | `On time range change` |

4. **Add** 클릭 → **Save** 클릭

### 8.2 패널에서 변수 사용

쿼리에서 `$job`을 변수로 참조:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle", job="$job"}[5m])) * 100)
```

→ 대시보드 상단 드롭다운에서 job을 선택하면 해당 job의 CPU만 표시

---

## 9. 알림(Alert) 설정

### 9.1 알림 채널 설정

1. 좌측 사이드바 → ⚙️ **Administration** → **Alerting**
2. **Contact points** 탭
3. **Add contact point** 클릭

#### 이메일 알림 설정

| 항목 | 값 |
|------|-----|
| Name | `Email` |
| Type | `Email` |
| Addresses | `your-email@example.com` |

#### Slack 알림 설정 (webhook)

| 항목 | 값 |
|------|-----|
| Name | `Slack` |
| Type | `Slack` |
| URL | Slack Incoming Webhook URL |
| Channel | `#alerts` |

### 9.2 알림 규칙 만들기

1. **Alert rules** 탭 → **New alert rule** 클릭

#### CPU 80% 알림 예시

**Rule name:** `CPU 사용률 80% 초과`

**Query:**
```
A: 100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

**Condition:**
```
IS ABOVE 80
```

**Evaluate:**
| 항목 | 값 |
|------|-----|
| Evaluate every | `1m` |
| For | `5m` |

→ 5분 동안 CPU가 80% 이상이면 알림 발생

### 9.3 알림 규칙 테스트

1. Grafana에서 알림 규칙이 **Pending** 상태로 변환
2. 조건 충족 시 **Firing**으로 변경
3. 설정한 채널로 알림 발송

---

## 10. 타임피커 (시간 범위 선택)

### 10.1 우측 상단 타임피커

```
[Last 6 hours ▼] [Refresh ▼]
```

### 10.2 자주 쓰는 시간 범위

| 선택 | 의미 |
|------|------|
| Last 5 minutes | 최근 5분 |
| Last 15 minutes | 최근 15분 |
| Last 1 hour | 최근 1시간 |
| Last 6 hours | 최근 6시간 |
| Last 24 hours | 최근 24시간 |
| Last 7 days | 최근 7일 |
| Last 30 days | 최근 30일 |
| Custom absolute range | 직접 시간 입력 |

### 10.3 새로고침

- **Auto refresh**: 자동 새로고침 간격 설정
  - 5s, 10s, 30s, 1m, 5m 등
- 수동 새로고침: **Refresh** 버튼 클릭

---

## 11. Explore에서 쿼리 테스트

대시보드를 만들기 전, Explore에서 쿼리가 제대로 동작하는지 테스트합니다.

### 11.1 Prometheus 쿼리 테스트

1. **Explore** → **Prometheus** 선택
2. 쿼리 입력 후 **Run query**
3. 결과 확인:
   - **Table** 탭: 숫자 값 확인
   - **Time series** 탭: 그래프 확인
   - **DataFrame** 탭: 원시 데이터

### 11.2 Loki 쿼리 테스트

1. **Explore** → **Loki** 선택
2. 쿼리 입력:

```
{job="spring-boot"} |= "ERROR"
```

3. **Run query** → 로그 결과 확인

### 11.3 Useful PromQL Queries (복붙용)

```promql
# 시스템 업타임
node_time_seconds - node_boot_time_seconds

# 디스크 사용량 (GB)
node_filesystem_size_bytes{mountpoint="/"} / 1024 / 1024 / 1024

# 디스크 여유 공간 (GB)
node_filesystem_avail_bytes{mountpoint="/"} / 1024 / 1024 / 1024

# 네트워크 수신 (bytes/sec)
rate(node_network_receive_bytes_total{device!="lo"}[5m])

# 네트워크 송신 (bytes/sec)
rate(node_network_transmit_bytes_total{device!="lo"}[5m])

# CPU 코어 수
count(count by (cpu) (node_cpu_seconds_total))

# JVM 메모리 (힙 외)
jvm_memory_used_bytes{area="nonheap"} / 1024 / 1024

# HikariCP 커넥션 풀
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_pending

# Tomcat 스레드
tomcat_threads_busy_threads
tomcat_threads_current_threads
```

---

## 12. 자주 쓰는 단축키

| 단축키 | 기능 |
|--------|------|
| `d` | 대시보드 목록으로 이동 |
| `e` | Explore 열기 |
| `n` | 새 대시보드 |
| `p` | 패널 편집 |
| `f` | 전체화면 |
| `⌘/ctrl + S` | 대시보드 저장 |
| `⌘/ctrl + Z` | 실행 취소 |
| `⌘/ctrl + Y` | 다시 실행 |
| `Escape` | 편집 모드 종료 |

---

## 13. 문제 해결

### Q: "No data" 표시

1. Explore에서 쿼리 직접 실행 → 데이터 존재 확인
2. 시간 범위 확인 (우측 상단 타임피커)
3. Prometheus 데이터소스 연결 확인

### Q: 그래프가 안 나옴

1. PromQL 문법 오류 확인
2. 라벨 이름 확인: Explore에서 `label_values()` 실행

```
label_values(node_cpu_seconds_total, job)
```

### Q: 대시보드 저장 실패

1. 로그인 세션 만료 → 재로그인
2. 권한 확인 → admin 계정 사용

### Q: Grafana 접근 불가

```bash
# Grafana 상태 확인
sudo systemctl status grafana-server

# 로그 확인
sudo tail -f /var/log/grafana/grafana.log

# 재시작
sudo systemctl restart grafana-server
```

---

## 14. 다음 단계

1. **위 3개 대시보드 생성** → 서버/Spring Boot/Prometheus
2. **공유 대시보드 임포트** → ID 1860, 12900, 14057
3. **알림 설정** → 이메일/Slack
4. **Explore에서 쿼리 연습** → 다양한 PromQL 시도

---

## 15. 참고 자료

- [개념 가이드](logging-concept.md)
- [설치 가이드](logging-install.md)
- [사용 가이드](logging-guide.md)
- [Prometheus 공식 문서](https://prometheus.io/docs/)
- [Grafana 공식 문서](https://grafana.com/docs/)
- [PromQLcheatsheet](https://promlabs.com/promql-cheat-sheet/)
