# 002-260818-micrometer-histogram-learning.md

## 개요
- **주제**: Micrometer HTTP 히스토그램 (`_bucket` 메트릭)과 P95 응답지연
- **학습일**: 2026-08-18
- **수준**: 중급

## 1. 개념 설명

### 1.1 정의
**히스토그램(Histogram)** 은 메트릭을 **정해진 구간(버킷)별 누적 개수**로 기록하는 방식입니다.

```
요청이 0.3초 걸림 → "0.25s~0.5s 구간" 카운터 +1
```

Micrometer는 HTTP 요청의 응답시간을 **메모리 안의 버킷 카운터**로만 기록하며, 파일/로그로 남기지 않습니다.

### 1.2 왜 필요한가
Spring Boot는 기본적으로 HTTP 요청에 대해:
- `http_server_requests_seconds_count` — **개수** (counter)
- `http_server_requests_seconds_sum` — **합계** (summary)

**만 기록합니다.** 이 두 개로는 **"95%의 요청이 몇 초 이내에 끝났는가"** 같은 **백분위수(percentile)** 를 계산할 수 없습니다.

백분위 계산에는 **분포(버킷)** 가 필요 → 히스토그램 활성화 필요.

### 1.3 관련 개념
- **Counter**: 계속 증가하는 누적 수 (예: 요청 개수)
- **Gauge**: 오르내리는 값 (예: CPU 사용률)
- **Summary**: 개수 + 합계 (Spring 기본)
- **Histogram**: 구간별 누적 (percentile 계산 가능)
- **Percentile (P95/P99)**: "하위 95%/99% 요청이 이 시간 안에 끝남"

## 2. 사용법

### 2.1 활성화 설정
각 모듈 `application.yml`:
```yaml
management:
  metrics:
    distribution:
      percentiles-histogram:
        http:
          server:
            requests: true
```

### 2.2 생성되는 메트릭
활성화 후 `/actuator/prometheus`에:
```
http_server_requests_seconds_bucket{le="0.25", ...}  5
http_server_requests_seconds_bucket{le="0.5", ...}   8
http_server_requests_seconds_bucket{le="1.0", ...}   9
...
```

`le`(less than or equal) = "이 값 이하인 요청 수". 누적 구조.

### 2.3 P95 쿼리
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))
```

- `rate(_bucket[5m])`: 버킷별 초당 증가율
- `sum(...) by (le)`: 모든 라벨 합산 후 le 기준으로 유지
- `histogram_quantile(0.95, ...)`: 버킷 분포에서 95번째 백분위 추정

## 3. 주의사항
- 히스토그램 활성화는 **배포 후 요청이 들어와야** 버킷에 값이 쌓임 (요청 없으면 No data)
- **부하 영향 최소**: 요청당 메모리 카운터 +1 연산, 파일 I/O 없음, 버킷 수 고정이라 메모리 증가 제한적
- **로그가 아님**: 히스토그램은 로그를 남기지 않음. Loki(로그)와 별개 영역
- 기본 버킷 ~10개 × 라벨 조합으로 메모리 사용량은 제한적

## 4. 실전 적용

### 4.1 이 프로젝트에서의 적용
- 2026-08-18: 4개 모듈(auth/scraper/resume/portfolio) `application.yml`에 히스토그램 활성화
- 목적: Grafana 알림 규칙 **"응답 지연 P95"** (`histogram_quantile(0.95, ...) > 2s`) 지원
- 변경 커밋: `aceea04`

### 4.2 관련 파일
- `modules/auth/backend/src/main/resources/application.yml`
- `modules/scraper/backend/src/main/resources/application.yml`
- `modules/resume/backend/src/main/resources/application.yml`
- `modules/portfolio/backend/src/main/resources/application.yml`

## 5. 참고 자료
- [Micrometer Histogram 공식 문서](https://micrometer.io/docs/concepts#_histograms)
- [Spring Boot Metrics 문서](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [PromQL histogram_quantile](https://prometheus.io/docs/prometheus/latest/querying/functions/#histogram_quantile)

---
*작성일: 2026-08-18*