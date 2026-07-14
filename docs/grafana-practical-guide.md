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
좌측 사이드바 (세로 메뉴):
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

## 2. Explore 화면 이해 (중요!)

### 2.1 Explore 열기

- **좌측 사이드바**에서 🔍 **Explore** 클릭
- 또는 키보드 단축키 `e` 누름

### 2.2 Explore 화면 구조

Explore 화면이 열리면 다음과 같이 보입니다:

```
┌─────────────────────────────────────────────────────────────────┐
│ [Prometheus ▼]   [Split]   [Add]                               │  ← 상단: 데이터소스 선택
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Go to queryless                                                │
│  Queries                                                        │
│                                                                 │
│  A  (Prometheus)                                                │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Kick start your query                                  │   │
│  │                                                          │   │
│  │  Explain                                                 │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  Metric                                                  │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │ Select metric                                    │    │   │  ← 메트릭 선택
│  │  └─────────────────────────────────────────────────┘    │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  Label filters                                           │   │
│  │  ┌──────────┐  ┌───┐  ┌──────────┐  [+]               │   │  ← 라벨 필터
│  │  │Select lbl│  │ = │  │Select val│                      │   │
│  │  └──────────┘  └───┘  └──────────┘                      │   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  Operations                                              │   │  ← 연산자 추가
│  ├─────────────────────────────────────────────────────────┤   │
│  │  Options                                                 │   │
│  │  Legend: [Auto]  Min step: [auto]  Format: [Time series]│   │
│  ├─────────────────────────────────────────────────────────┤   │
│  │  Exemplars                                               │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  [Add query]                                                    │  ← 쿼리 추가
│                                                                 │
│  [Query history]  [Query inspector]                             │  ← 하단 도구
│                                                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  (결과 그래프 또는 테이블이 여기에 표시됨)                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 각 요소 설명

| 요소 | 위치 | 설명 |
|------|------|------|
| **데이터소스 드롭다운** | 맨 위 왼쪽 | Prometheus, Loki 등 선택 |
| **Split** | 상단 | 화면을 양쪽으로 분할 (비교용) |
| **Add** | 상단 | 새 쿼리 패널 추가 |
| **Go to queryless** | 쿼리 패널 상단 | 시각적 쿼리 빌더 ↔ 코드 모드 전환 |
| **Kick start your query** | 쿼리 패널 안 | 쿼리 힌트/예시 제공 |
| **Metric** | 쿼리 패널 안 | 메트릭 이름 선택 (드롭다운) |
| **Label filters** | Metric 아래 | 라벨 필터 추가 |
| **Operations** | Label filters 아래 | 연산자 (rate, sum 등) 추가 |
| **Options** | Operations 아래 | Legend, Min step, Format 설정 |
| **Add query** | 쿼리 패널 아래 | 새 쿼리 (B, C 등) 추가 |
| **Query history** | 하단 왼쪽 | 이전 쿼리 기록 |
| **Query inspector** | 하단 오른쪽 | 쿼리 상세 정보/디버깅 |

---

## 3. Explore에서 쿼리 실행하는 방법

### 3.1 방법 1: 시각적 쿼리 빌더 (쉬운 방법)

#### Step 1: 메트릭 선택

- **Metric** 섹션에서 **Select metric** 클릭
- 드롭다운이 나타남
- `node_cpu_seconds_total` 입력 또는 선택

#### Step 2: 라벨 필터 추가 (선택)

- **Label filters**에서 **Select label** 클릭
- `mode` 선택
- 값 입력란에 `idle` 입력
- **+** 버튼으로 필터 추가

#### Step 3: 연산자 추가 (Operations)

- **Operations**에서 **+ Add operation** 클릭
- 드롭다운 메뉴가 나타남:

```
+ Add operation
├── Range functions
│   ├── rate
│   ├── increase
│   └── irate
├── Aggregations
│   ├── sum
│   ├── avg
│   ├── min
│   ├── max
│   ├── count
│   └── ...
├── Group by
│   ├── by
│   └── without
├── Binary operations
│   ├── Math          ← 여기! (수학 연산)
│   └── Offset
└── ...
```

**CPU 사용률 계산 순서:**

1. **Range functions** → **rate** 선택 → `[5m]` 입력
2. **Aggregations** → **avg** 선택
3. **Binary operations** → **Math** 선택
4. Math 필드에 `100 - x * 100` 입력 (x는 이전 결과를 의미)

```
연산자 체인:
rate([5m]) → avg → Math(100 - x * 100)
```

> **참고:** Math 연산자는 **Binary operations** 카테고리에 있습니다.
> 드롭다운 메뉴를 아래로 스크롤하면 보입니다.

#### Step 4: 실행

- 쿼리 빌더 하단에 결과가 자동으로 표시됨
- 수동 실행이 필요하면 **Run query** 버튼 클릭

### 3.2 방법 2: 코드 모드 (PromQL 직접 입력) - 추천!

시각적 빌더가 복잡하면 **코드 모드**를 사용하는 것이 더 쉽습니다.

#### Step 1: 코드 모드로 전환

- 쿼리 패널 상단의 **Go to queryless** 클릭
  - 시각적 빌더 → 코드 모드로 전환됨

#### Step 2: 쿼리 입력

코드 모드에서 입력칸에 직접 PromQL 입력:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

#### Step 3: 실행

- **Run query** 버튼 클릭 (입력칸 오른쪽)
- 또는 **Shift + Enter** 키 누름

> **팁:** 복잡한 수학 연산은 코드 모드가 훨씬 쉽습니다.
> 시각적 빌더는 단순한 쿼리에 적합합니다.

### 3.3 방법 3: Kick start your query 활용

- **Kick start your query** 클릭
- 자주 쓰는 쿼리 패턴이 나옴
- 원하는 패턴 클릭 → 자동으로 쿼리 생성
- 필요에 따라 수정

---

## 4. Datasource 확인 (데이터 연결)

### 4.1 Prometheus 확인

#### Step 1: Explore 열기

- **좌측 사이드바**에서 🔍 **Explore** 클릭

#### Step 2: 데이터소스 선택

- **화면 맨 위** 드롭다운에서 **Prometheus** 선택

#### Step 3: 쿼리 실행

**방법 A (시각적 빌더):**

1. **Metric**에서 `up` 선택
2. 결과에 값이 `1`로 표시되면 정상

**방법 B (코드 모드 - 추천):**

1. **Go to queryless** 클릭 → 코드 모드로 전환
2. 입력칸에 `up` 입력
3. **Run query** 클릭

#### Step 4: 결과 확인

결과에 다음이 표시되면 정상:

```
up{job="prometheus"}       1
up{job="node-exporter"}   1
up{job="spring-boot"}     1
up{job="mysql"}           1
```

→ 값이 `1`이면 정상 수집 중

#### Step 5: CPU 쿼리 테스트

**방법 A (시각적 빌더):**

1. **Metric**: `node_cpu_seconds_total`
2. **Label filters**: `mode` = `idle`
3. **Operations**: 
   - Range functions → **rate**: `[5m]`
   - Aggregations → **avg**
   - Binary operations → **Math**: `100 - x * 100`

**방법 B (코드 모드 - 추천):**

1. **Go to queryless** 클릭 → 코드 모드로 전환
2. 입력:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

3. **Run query** 클릭
4. 그래프가 나타나면 CPU 메트릭이 수집되고 있다는 뜻

### 4.2 Loki 확인

#### Step 1: 데이터소스 변경

- Explore 화면 맨 위 드롭다운에서 **Loki** 선택

#### Step 2: 쿼리 실행

**방법 A (시각적 빌더):**

1. **Metric**: `choose` → `logs` 선택
2. **Label filters**: `job` = `nginx`
3. 결과에 nginx 로그가 나타남

**방법 B (코드 모드 - 추천):**

1. **Go to queryless** 클릭 → 코드 모드로 전환
2. 입력:

```
{job="nginx"}
```

3. **Run query** 클릭

---

## 5. 첫 번째 대시보드 만들기 (CPU/메모리/디스크)

### 5.1 새 대시보드 생성

#### Step 1: Create 메뉴

- **좌측 사이드바**에서 ➕ **Create** 클릭
- 드롭다운 메뉴가 나타남
- **Dashboard** 클릭

#### Step 2: 빈 대시보드

빈 대시보드 화면이 나타남:

```
┌─────────────────────────────────────────────────────┐
│  Untitled Dashboard                    [Save] [⚙️]  │  ← 상단
├─────────────────────────────────────────────────────┤
│                                                     │
│              ┌─────────────────────┐                │
│              │   Add visualization │                │  ← 클릭!
│              │   (아이콘 + 텍스트)  │                │
│              └─────────────────────┘                │
│                                                     │
└─────────────────────────────────────────────────────┘
```

- **Add visualization** 클릭 (화면 중앙 또는 상단의 + 버튼)

### 5.2 CPU 사용률 패널 만들기

패널 편집 화면이 열리면:

```
┌──────────────────────────────────────────────────────┐
│  Panel Title: [ Untitled panel          ]            │  ← 제목 입력
│  Description: [ (선택사항)              ]            │
├──────────────────────────────────────────────────────┤
│                                                      │
│  [Query]  [Transformations]  [Alert]  [...]         │  ← 탭 메뉴
│                                                      │
│  A  ┌────────────────────────────────────────────┐  │
│     │ (여기에 PromQL 쿼리 입력)                  │  │  ← 쿼리 입력
│     └────────────────────────────────────────────┘  │
│                                                      │
│  [Visualize] 탭에서 패널 종류 선택                     │
│                                                      │
├──────────────────────────────────────────────────────┤
│  [Standard options]  [Thresholds]  [...]            │  ← 우측 설정
└──────────────────────────────────────────────────────┘
```

#### Step 1: Data source 선택

- 패널 편집 화면 **상단**에 Data source 드롭다운이 있음
- **Prometheus** 선택

#### Step 2: 쿼리 입력

- **Query** 탭이 선택되어 있는지 확인
- **A** 옆 입력칸에 다음 입력:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

- 입력 후 **Run query** 클릭 (입력칸 오른쪽)
- 또는 **Shift + Enter**

#### Step 3: 패널 종류 선택

- **Visualize** 탭 클릭 (Query 탭 옆)
- **Time series** 선택 (기본값일 수 있음)

#### Step 4: 패널 설정 (좌측)

**Panel options:**

| 항목 | 위치 | 값 |
|------|------|-----|
| Title | 패널 상단 입력칸 | `CPU 사용률 (%)` |
| Description | Title 아래 입력칸 | `서버 CPU 사용률 (0-100%)` |

#### Step 5: 옵션 설정 (우측)

**우측 사이드바**에서:

- **Standard options** 섹션 펼침
- **Unit** 드롭다운 클릭 → **Percent (0-100)** 선택
- **Min** 입력칸에 `0` 입력
- **Max** 입력칸에 `100` 입력
- **Decimals** 입력칸에 `1` 입력

#### Step 6: Apply

- 패널 편집 화면 **상단 우측**에 **Apply** 버튼 클릭
- 대시보드에 패널이 추가됨

#### Step 7: 대시보드 저장

- 대시보드 **상단 우측**에 💾 **Save dashboard** 아이콘 클릭
- **Title** 입력칸에 `서버 모니터링` 입력
- **Save** 클릭

### 5.3 메모리 사용률 패널 추가

1. 대시보드 **상단**에서 **Add** → **Visualization** 클릭
2. Data source: **Prometheus** 선택
3. **Query** 탭에서 쿼리 입력:

```
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100
```

4. **Run query** 클릭
5. 우측 **Standard options**:
   - Unit: `Percent (0-100)`
   - Min: `0`, Max: `100`
6. **Panel options**에서 Title: `메모리 사용률 (%)`
7. **Apply** 클릭

### 5.4 디스크 사용률 패널 추가

1. **Add** → **Visualization**
2. 쿼리:

```
(node_filesystem_size_bytes{mountpoint="/"} - node_filesystem_avail_bytes{mountpoint="/"}) / node_filesystem_size_bytes{mountpoint="/"} * 100
```

3. **Run query** 클릭
4. 우측 설정:
   - Unit: `Percent (0-100)`
   - Min: `0`, Max: `100`
5. Title: `디스크 사용률 (%)`
6. **Apply** 클릭

### 5.5 네트워크 트래픽 패널 추가

1. **Add** → **Visualization**
2. **Query** 탭에서 쿼리 2개 입력:

**Query A** (수신):
```
rate(node_network_receive_bytes_total{device!="lo"}[5m])
```

**Query B** (송신):
```
rate(node_network_transmit_bytes_total{device!="lo"}[5m])
```

3. **Run query** 클릭
4. 우측 **Standard options**: Unit: `bytes/sec (Bps)`
5. **Legend** 입력칸에 `{{device}} - {{direction}}` 입력
6. Title: `네트워크 트래픽`
7. **Apply** 클릭

### 5.6 대시보드 저장

1. 상단 우측 💾 **Save dashboard** 클릭
2. **Title**: `서버 모니터링`
3. **Save** 클릭

---

## 6. Spring Boot 대시보드 만들기

### 6.1 새 대시보드 생성

1. ➕ Create → Dashboard
2. 이름: `Spring Boot 모니터링`

### 6.2 JVM 헙 사용률

1. Add visualization
2. Data source: **Prometheus**
3. 쿼리 입력:

```
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

4. **Run query** 클릭
5. 설정:
   - Title: `JVM 헙 사용률 (%)`
   - Unit: `Percent (0-100)`
   - Legend: `{{instance}}`
6. **Apply** 클릭

### 6.3 HTTP 요청수 (요청/sec)

1. Add visualization
2. 쿼리:

```
rate(http_server_requests_seconds_count[5m])
```

3. **Run query** 클릭
4. 설정:
   - Title: `HTTP 요청수 (req/sec)`
   - Unit: `reqps`
   - Legend: `{{method}} {{uri}}`
5. **Apply** 클릭

### 6.4 HTTP 응답 시간

1. Add visualization
2. 쿼리:

```
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])
```

3. **Run query** 클릭
4. 설정:
   - Title: `HTTP 응답 시간 (평균)`
   - Unit: `seconds`
5. **Apply** 클릭

### 6.5 HikariCP 커넥션 풀

1. Add visualization
2. 쿼리:

```
hikaricp_connections_active
```

3. **Run query** 클릭
4. 설정:
   - Title: `HikariCP 활성 커넥션`
   - Unit: `short`
5. **Apply** 클릭

### 6.6 에러율

1. Add visualization
2. 쿼리:

```
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)
```

3. **Run query** 클릭
4. 설정:
   - Title: `HTTP 5xx 에러율 (req/sec)`
   - Unit: `reqps`
5. **Apply** 클릭

---

## 7. Prometheus 대시보드 만들기

### 7.1 새 대시보드

1. ➕ Create → Dashboard
2. 이름: `Prometheus 모니터링`

### 7.2 Prometheus 수집 메트릭 수

1. Add visualization
2. 쿼리:

```
prometheus_tsdb_head_series
```

3. **Run query** 클릭
4. 설정:
   - Title: `수집된 시리즈 수`
   - Unit: `short`
5. **Apply** 클릭

### 7.3 쿼리 처리 시간

1. Add visualization
2. 쿼리:

```
histogram_quantile(0.99, rate(prometheus_engine_query_duration_seconds_bucket[5m]))
```

3. **Run query** 클릭
4. 설정:
   - Title: `쿼리 처리 시간 (99th percentile)`
   - Unit: `seconds`
5. **Apply** 클릭

---

## 8. Grafana 대시보드 임포트 (빠른 방법)

매번 직접 만들 필요 없이, 공유된 대시보드를 가져올 수 있습니다.

### 8.1 Node Exporter Full (서버 모니터링)

1. **좌측 사이드바** → ➕ **Create** → **Import** 클릭
2. **Import via grafana.com** 입력란에: `1860` 입력
3. **Load** 클릭 (입력란 오른쪽)
4. **Data source** 드롭다운에서 **Prometheus** 선택
5. **Import** 클릭 (하단)
6. 완성! 수백 개의 서버 메트릭 패널이 자동 생성됨

### 8.2 Spring Boot Statistics

1. ➕ Create → **Import**
2. 입력란에: `12900` 입력
3. **Load** 클릭
4. Data source: **Prometheus** 선택
5. **Import** 클릭

### 8.3 MariaDB Dashboard

1. ➕ Create → **Import**
2. 입력란에: `14057` 입력
3. **Load** 클릭
4. Data source: **Prometheus** 선택
5. **Import** 클릭

---

## 9. 패널 종류와 사용법

### 9.1 Time series (시계열 그래프)

- 가장 기본적인 그래프
- 시간에 따른 값 변화 표시
- 사용: CPU, 메모리, 요청수 등

### 9.2 Gauge (게이지)

- 현재 값을 원형 게이지로 표시
- 사용: CPU 사용률, 디스크 사용률 등

**설정 방법:**

1. 패널 → **Visualize** 탭 → **Gauge** 선택
2. 우측 **Standard options**:
   - Min: `0`, Max: `100`
   - **Thresholds** 섹션:
     - Green: 0-60
     - Yellow: 60-80
     - Red: 80-100

### 9.3 Stat (통계)

- 큰 숫자로 현재 값 표시
- 사용: 현재 연결 수, 시리즈 수 등

### 9.4 Bar chart (막대 그래프)

- 범주별 값 비교
- 사용: URI별 요청수, 에러 카운트 등

### 9.5 Table (테이블)

- 로그나 상세 데이터 표시
- 사용: 최근 에러 목록 등

---

## 10. 변수(Variable)로 필터 만들기

대시보드 상단에 드롭다운 필터를 만들어서, job별/URI별로 전환할 수 있습니다.

### 10.1 job 변수 만들기

#### Step 1: 설정 열기

- 대시보드 **상단 우측**에 ⚙️ **Dashboard settings** (톱니바퀴 아이콘) 클릭

#### Step 2: Variables 메뉴

- **좌측 사이드바**에서 **Variables** 클릭
- **New variable** 버튼 클릭

#### Step 3: 변수 설정

| 항목 | 위치 | 값 |
|------|------|-----|
| Name | 상단 입력칸 | `job` |
| Label | Name 아래 | `Job` |
| Type | 드롭다운 | `Query` |
| Query | 입력칸 | `label_values(node_cpu_seconds_total, job)` |
| Refresh | 드롭다운 | `On time range change` |

#### Step 4: 저장

- 하단 **Add** 클릭
- **Save** 클릭

### 10.2 패널에서 변수 사용

쿼리에서 `$job`을 변수로 참조:

```
100 - (avg(rate(node_cpu_seconds_total{mode="idle", job="$job"}[5m])) * 100)
```

→ 대시보드 상단 드롭다운에서 job을 선택하면 해당 job의 CPU만 표시

---

## 11. 알림(Alert) 설정

### 11.1 알림 채널 설정

#### Step 1: 알림 메뉴

- **좌측 사이드바** → ⚙️ **Administration** → **Alerting** 클릭

#### Step 2: Contact points

- **Contact points** 탭 클릭
- **Add contact point** 버튼 클릭

#### Step 3: 이메일 알림 설정

| 항목 | 값 |
|------|-----|
| Name | `Email` |
| Type | `Email` 드롭다운 선택 |
| Addresses | `your-email@example.com` 입력 |

#### Step 4: 저장

- **Save contact point** 클릭

### 11.2 알림 규칙 만들기

#### Step 1: Alert rules

- **Alert rules** 탭 클릭
- **New alert rule** 버튼 클릭

#### Step 2: 규칙 설정

**Rule name** 입력칸에: `CPU 사용률 80% 초과`

**Query and alert condition:**

- 데이터소스: **Prometheus** 선택
- 쿼리 입력:

```
A: 100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

- **Condition** 드롭다운: `IS ABOVE` 선택
- 값 입력칸에: `80` 입력

**Evaluate:**

| 항목 | 값 |
|------|-----|
| Evaluate every | `1m` |
| For | `5m` |

→ 5분 동안 CPU가 80% 이상이면 알림 발생

#### Step 3: 저장

- 하단 **Save rule and exit** 클릭

---

## 12. 타임피커 (시간 범위 선택)

### 12.1 우측 상단 타임피커

대시보드 **우측 상단**에:

```
[Last 6 hours ▼] [Refresh ▼]
```

### 12.2 시간 범위 변경

- **Last 6 hours** 클릭 → 드롭다운 메뉴
- 원하는 시간 범위 선택:
  - Last 5 minutes
  - Last 15 minutes
  - Last 1 hour
  - Last 6 hours
  - Last 24 hours
  - Last 7 days
  - Custom absolute range (직접 입력)

### 12.3 새로고침

- **Refresh** 버튼 클릭: 수동 새로고침
- **Auto refresh** 드롭다운: 자동 새로고침 간격 설정 (5s, 10s, 30s, 1m 등)

---

## 13. Useful PromQL Queries (복붙용)

### 13.1 시스템 메트릭

```promql
# CPU 사용률 (%)
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 메모리 사용률 (%)
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# 디스크 사용률 (%)
(node_filesystem_size_bytes{mountpoint="/"} - node_filesystem_avail_bytes{mountpoint="/"}) / node_filesystem_size_bytes{mountpoint="/"} * 100

# 디스크 사용량 (GB)
node_filesystem_size_bytes{mountpoint="/"} / 1024 / 1024 / 1024

# 디스크 여유 공간 (GB)
node_filesystem_avail_bytes{mountpoint="/"} / 1024 / 1024 / 1024

# 네트워크 수신 (bytes/sec)
rate(node_network_receive_bytes_total{device!="lo"}[5m])

# 네트워크 송신 (bytes/sec)
rate(node_network_transmit_bytes_total{device!="lo"}[5m])

# 시스템 업타임 (초)
node_time_seconds - node_boot_time_seconds

# CPU 코어 수
count(count by (cpu) (node_cpu_seconds_total))
```

### 13.2 Spring Boot 메트릭

```promql
# JVM 헙 사용률 (%)
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100

# JVM 메모리 (힙 외, MB)
jvm_memory_used_bytes{area="nonheap"} / 1024 / 1024

# HTTP 요청수 (req/sec)
rate(http_server_requests_seconds_count[5m])

# HTTP 응답 시간 (초)
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# HTTP 5xx 에러율 (req/sec)
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (uri)

# HikariCP 활성 커넥션
hikaricp_connections_active

# HikariCP 유휴 커넥션
hikaricp_connections_idle

# HikariCP 대기 커넥션
hikaricp_connections_pending

# Tomcat 바쁜 스레드
tomcat_threads_busy_threads

# Tomcat 현재 스레드
tomcat_threads_current_threads
```

### 13.3 Prometheus 메트릭

```promql
# 수집된 시리즈 수
prometheus_tsdb_head_series

# 쿼리 처리 시간 (99th percentile)
histogram_quantile(0.99, rate(prometheus_engine_query_duration_seconds_bucket[5m]))
```

---

## 14. 자주 쓰는 단축키

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

## 15. 문제 해결

### Q: "No data" 표시

1. Explore에서 쿼리 직접 실행 → 데이터 존재 확인
2. 시간 범위 확인 (우측 상단 타임피커)
3. Prometheus 데이터소스 연결 확인

### Q: 그래프가 안 나옴

1. PromQL 문법 오류 확인
2. 라벨 이름 확인: Explore에서 다음 쿼리 실행:

```
label_values(node_cpu_seconds_total, job)
```

→ `Run query` 클릭 → job 목록이 나타나면 정상

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

## 16. 다음 단계

1. **위 3개 대시보드 생성** → 서버/Spring Boot/Prometheus
2. **공유 대시보드 임포트** → ID 1860, 12900, 14057
3. **알림 설정** → 이메일/Slack
4. **Explore에서 쿼리 연습** → 다양한 PromQL 시도

---

## 17. 참고 자료

- [개념 가이드](logging-concept.md)
- [설치 가이드](logging-install.md)
- [사용 가이드](logging-guide.md)
- [Prometheus 공식 문서](https://prometheus.io/docs/)
- [Grafana 공식 문서](https://grafana.com/docs/)
- [PromQLcheatsheet](https://promlabs.com/promql-cheat-sheet/)
