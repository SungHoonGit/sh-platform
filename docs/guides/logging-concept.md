# 중앙 집중 로깅 개념 가이드

## 1. 중앙 집중 로깅이란?

여러 서버/서비스의 로그를 **한 곳으로 모아서** 검색하고 분석하는 시스템.

```
[기존 방식]                    [중앙 집중 로깅]
WEB 서버 로그 → SSH 접속        모든 로그 → 중앙 저장소 → Grafana에서 검색
DB 서버 로그 → SSH 접속
Spring Boot 로그 → SSH 접속
```

### 왜 필요한가?

| 문제 | 해결 |
|------|------|
| 장애 발생 시 SSH로 일일이 접속 | 한 화면에서 모든 로그 확인 |
| 로그가 로테이션되어 사라짐 | 중앙 저장소에 보관 |
| 여러 서비스의 로그 비교 어려움 | 통합 인터페이스에서 검색 |
| 로그 기반 알림 불가 | 로그 패턴 기반 알림 가능 |

---

## 2. 구성 요소

### 2.1 스택 구성

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Spring Boot │    │    nginx     │    │    syslog    │
│     로그     │    │     로그     │    │     로그     │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────┐
│                    Promtail                         │
│              (로그 에이전트/수집기)                  │
│                                                     │
│  - 로그 파일 감시 (파일 시스템 스캐너)              │
│  - 라벨 태깅 (job, type 등)                        │
│  - Loki로 전송                                     │
└─────────────────────┬───────────────────────────────┘
                      │ HTTP Push
                      ▼
              ┌──────────────┐
              │     Loki     │
              │  (저장소)    │
              │              │
│  - 라벨 기반 인덱싱         │
│  - 청크 단위 저장           │
│  - LogQL 쿼리 지원         │
└──────────────┬──────────────┘
                      │
                      ▼
              ┌──────────────┐
              │   Grafana    │
              │  (시각화)    │
              │              │
│  - Explore에서 로그 검색    │
│  - 대시보드에 로그 패널     │
│  - 메트릭과 로그 연동       │
└──────────────────────────────┘
```

### 2.2 각 컴포넌트 역할

| 컴포넌트 | 역할 | 포트 |
|----------|------|------|
| **Promtail** | 로그 파일에서 로그를 읽어 Loki로 전송 | 9080 |
| **Loki** | 로그를 저장하고 검색하는 엔진 | 3100 |
| **Grafana** | 로그를 시각화하고 대시보드 제공 | 3000 |

---

## 3. 로그 수집 흐름

### 3.1 로그가 파일에서 Grafana까지 도달하는 과정

```
1단계: 로그 생성
  Spring Boot → /home/ubuntu/sh-platform/logs/sh-platform.log
  nginx → /var/log/nginx/access.log
  syslog → /var/log/syslog

2단계: Promtail 수집
  - 파일 변경 감시 (inotify)
  - 새 라인 읽기
  - 라벨 추가 (job, filename 등)
  - 일정량 배치로 묶어서 Loki로 전송

3단계: Loki 저장
  - 라벨로 인덱싱 (빠른 필터링)
  - 로그 내용은 청크 단위로 압축 저장
  - 리텐션 정책에 따라 자동 삭제

4단계: Grafana 조회
  - Explore에서 LogQL 쿼리
  - 대시보드에서 로그 패널
  - 메트릭 ↔ 로그 전환
```

### 3.2 라벨 시스템

Loki의 핵심은 **라벨 기반 인덱싱**.

```
{job="spring-boot"} → spring-boot 관련 로그만 필터
{job="nginx", type="error"} → nginx 에러 로그만 필터
{filename="/var/log/nginx/access.log"} → 특정 파일 로그
```

| 라벨 | 값 | 설명 |
|------|-----|------|
| `job` | spring-boot, nginx, syslog | 서비스 그룹 |
| `type` | access, error | 로그 유형 (nginx만) |
| `filename` | 파일 경로 | 실제 로그 파일 경로 |
| `detected_level` | INFO, WARN, ERROR | 로그 레벨 (자동 감지) |

---

## 4. LogQL (Log Query Language)

Loki의 쿼리 언어. SQL이나 PromQL과 유사.

### 4.1 기본 문법

```
{라벨="값"} | 필터 | 정렬 | 집계
```

### 4.2 예시

```logql
# 모든 spring-boot 로그
{job="spring-boot"}

# ERROR 로그만
{job="spring-boot"} |= "ERROR"

# DEBUG 제외
{job="spring-boot"} != "DEBUG"

# timeout 또는 error 포함
{job="spring-boot"} |~ "timeout|error"

# nginx 5xx 에러
{job="nginx"} |= "500" or |= "502" or |= "503"

# 특정 IP 접근 로그
{job="nginx"} |= "192.168.1.100"
```

### 4.3 연산자

| 연산자 | 의미 | 예시 |
|--------|------|------|
| `\|=` | 문자열 포함 | `\| "ERROR"` |
| `\|~` | 정규식 매칭 | `\|~ "timeout\|error"` |
| `!=` | 문자열 불포함 | `!= "DEBUG"` |
| `!~` | 정규식 불매칭 | `!~ "health"` |

### 4.4 메트릭 변환

```logql
# 에러 로그 비율 (초당)
rate({job="spring-boot"} |= "ERROR" [5m])

# 에러 카운트
sum(rate({job="spring-boot"} |= "ERROR" [5m])) by (job)

# 로그에서 숫자 추출
{job="spring-boot"} | logfmt | unwrap duration | (1000)
```

---

## 5. 메트릭 vs 로그

| 항목 | Prometheus (메트릭) | Loki (로그) |
|------|---------------------|-------------|
| **데이터 유형** | 숫자 (카운터, 게이지) | 텍스트 (로그 라인) |
| **인덱싱** | 라벨 | 라벨 |
| **검색** | PromQL | LogQL |
| **용도** | CPU, 메모리, 요청수 등 | 에러 메시지, 트레이스 등 |
| **보관** | 시계열 데이터 | 원본 로그 |
| **상관관계** | 메트릭 → 로그 | 로그 → 메트릭 |

### 5.1 Grafana에서 통합

```
메트릭 대시보드 (Prometheus)
  → CPU 80% 초과
  → 해당 시간대 로그 조회 (Loki)
  → 에러 원인 파악
```

---

## 6. 장단점

### 6.1 장점

- **가벼움**: Elasticsearch 대비 메모리/디스크 사용량 적음
- **Grafana 연동**: 기본 지원, 별도 설정 불필요
- **LogQL**: 학습곡선 낮음
- **확장성**: 새 서비스 추가 시 Promtail 설정만 하면 됨
- **비용**: OCI Free tier에서도 충분히 동작

### 6.2 단점

- **전문 검색 불가**: 전체 텍스트 검색은 Elasticsearch가 더 빠름
- **고카디널리티 지양**: 라벨에 너무 많은 고유값 사용 시 성능 저하
- **LSM-tree 기반**: 실시간 검색보다는 시계열 데이터에 최적화

---

## 7. 참고 자료

- [Loki 공식 문서](https://grafana.com/docs/loki/latest/)
- [LogQLcheatsheet](https://grafana.com/docs/loki/latest/query/)
- [Promtail 공식 문서](https://grafana.com/docs/promtail/latest/)
- [설치 가이드](logging-install.md)
- [사용 가이드](logging-guide.md)
