# 중앙 집중 로깅 - 검토 및 계획서

## 1. 왜 중앙 집중 로깅이 필요한가?

```
현재 상황:
  Spring Boot 로그 → 서버 로컬 파일에만 저장
  nginx 로그 → 서버 로컬 파일에만 저장
  
문제:
  - 장애 발생 시 SSH로 서버에 접속하여 로그 확인
  - 로그가 로테이션되어 사라질 수 있음
  - 여러 서버의 로그를 비교/분석 어려움
  
해결:
  - 모든 로그를 중앙으로 모아서 검색/분석
  - Grafana에서 메트릭 + 로그를 한 화면에서 확인
```

---

## 2. 업계 표준 솔루션 비교

| 솔루션 | 구성 | 최소 메모리 | 특징 |
|--------|------|-------------|------|
| **Loki + Grafana** | Promtail + Loki | 2GB | ✅ Grafana 기본 연동, 가벼움 |
| **ELK Stack** | Elasticsearch + Logstash + Kibana | 36GB | 강력한 전문 검색, 무거움 |
| **OpenSearch** | OpenSearch + Dashboards | 8GB | ELK의 오픈소스 포크 |
| **Graylog** | Graylog + MongoDB + ES | 4GB | 알림 기능 내장 |

---

## 3. 왜 Loki인가? (우리 상황)

### 3.1 비교 분석

| 항목 | ELK | Loki |
|------|-----|------|
| **리소스** | 14 CPU, 36GB RAM | 1 CPU, 2GB RAM |
| **저장 비용** | 로그 원본의 10~30배 | 로그 원본의 1~3배 |
| **검색** | 전문 검색 (모든 단어) | 라벨 기반 검색 |
| **Grafana 연동** | 별도 설정 | **기본 연동** |
| **학습곡선** | 높음 | 낮음 |
| **OCI Free 적합** | ❌ (메모리 부족) | ✅ (가벼움) |

### 3.2 Loki의 동작 방식

```
[Spring Boot 로그]  ──▶  Promtail  ──▶  Loki  ──▶  Grafana
[nginx 로그]        ──▶  Promtail  ──▶  Loki  ──▶  Grafana
[서버 syslog]       ──▶  Promtail  ──▶  Loki  ──▶  Grafana
```

**핵심**: 로그 내용이 아닌 **라벨(메타데이터)** 만 인덱싱
- `{job="spring-boot", level="ERROR"}` → 라벨로 필터링
- 그 다음 필터링된 로그 스트림 내에서 검색

---

## 4. Loki vs ELK 상세 비교

### 4.1 검색 기능

| 기능 | ELK | Loki |
|------|-----|------|
| 전체 텍스트 검색 | ✅ (초당 수백만 줄) | ❌ (라벨 필터 후 검색) |
| 라벨 기반 검색 | ✅ | ✅ (빠름) |
| 정규식 검색 | ✅ | ✅ |
| 로그 상관관계 | ✅ | ✅ (Grafana에서) |

### 4.2 운영 편의성

| 항목 | ELK | Loki |
|------|-----|------|
| 설치 복잡도 | 높음 (3개 이상 컴포넌트) | 낮음 (2개) |
| 업그레이드 | 복잡 (샤드 리밸런싱) | 간단 |
| 모니터링 | 별도 | Grafana에서 통합 |

---

## 5. 우리 프로젝트 적용 계획

### 5.1 아키텍처

```
[WEB 서버]
  ├── Spring Boot 로그 → Promtail → Loki
  ├── nginx 로그 → Promtail → Loki
  └── 시스템 로그 → Promtail → Loki

[DB 서버]
  └── MariaDB 로그 → Promtail → Loki (추후)

[Grafana]
  └── Loki Datasource 연결
```

### 5.2 설치 순서

| 단계 | 작업 | 소요 시간 |
|------|------|-----------|
| 1 | Loki 설치 | 10분 |
| 2 | Promtail 설치 및 설정 | 15분 |
| 3 | Spring Boot 로그 설정 | 5분 |
| 4 | nginx 로그 경로 설정 | 5분 |
| 5 | Grafana에 Loki Datasource 등록 | 5분 |
| 6 | 테스트 | 10분 |

**총 예상 시간: 50분**

### 5.3 필요한 리소스

| 항목 | 값 |
|------|-----|
| Loki | 1 CPU, 2GB RAM |
| Promtail | 1 CPU, 100MB RAM |
| 디스크 | 로그 보관 기간에 따라 (기본 7일) |

---

## 6. PromQL vs LogQL

### 6.1 PromQL (메트릭 - 현재 사용)

```promql
# CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

### 6.2 LogQL (로그 - Loki 사용 시)

```logql
# spring-boot ERROR 로그 검색
{job="spring-boot"} |~ "ERROR"

# nginx 5xx 에러 검색
{job="nginx"} |= "500" or |= "502" or |= "503"

# 특정 키워드 검색
{job="spring-boot"} |= "timeout" != "DEBUG"

# 로그 빈도 (에러율)
rate({job="spring-boot"} |= "ERROR" [5m])
```

---

## 7. 테스트 계획

### 7.1 사전 확인

- [ ] Loki 서비스 정상 동작 확인
- [ ] Promtail이 로그를 수집하는지 확인
- [ ] Grafana에서 Loki Datasource 연결 확인

### 7.2 기능 테스트

- [ ] Spring Boot 로그 검색 테스트
- [ ] nginx access/error 로그 검색 테스트
- [ ] 시스템 로그 검색 테스트
- [ ] 라벨 필터링 테스트 (`{job="spring-boot"}`)
- [ ] 로그 필터링 테스트 (`|= "ERROR"`)
- [ ] 시간 범위 지정 검색 테스트

### 7.3 통합 테스트

- [ ] Grafana 대시보드에서 메트릭 + 로그 동시 확인
- [ ] 로그 기반 알림 설정 테스트

---

## 8. 로그 보관 정책

| 로그 유형 | 보관 기간 | 비고 |
|-----------|-----------|------|
| Spring Boot | 7일 | 에러 로그는 30일 |
| nginx | 7일 | access 로그는 7일 |
| 시스템 | 7일 | auth 로그는 30일 |

---

## 9. 주의사항

### 9.1 Loki의 한계

- **전문 검색 불가**: "이 IP로 접근한 모든 로그" 같은 쿼리는 느림
- **고카디널리티 라벨 지양**: 사용자 IP를 라벨로 만들면 성능 저하
- **ELK 대비 검색 속도 느림**: 대용량 로그에서 전문 검색이 필요하면 ELK 고려

### 9.2 OCI Free tier 고려사항

- 현재 WEB 서버: 2 OCPU / 12GB
- Spring Boot: ~200MB 사용
- nginx: ~10MB 사용
- Promtail: ~100MB 사용
- Loki: ~200MB 사용
- **여유 공간 충분**

---

## 10. 추후 확장

| 단계 | 내용 | 시점 |
|------|------|------|
| 1단계 | WEB 서버 로그 수집 | 현재 |
| 2단계 | DB 서버 로그 수집 | 필요 시 |
| 3단계 | 알림 규칙 설정 | 운영 안정화 후 |
| 4단계 | 로그 아카이빙 (S3 등) | 로그 보관 정책 확대 시 |

---

## 11. 참고 자료

- [Loki 공식 문서](https://grafana.com/docs/loki/latest/)
- [Promtail 공식 문서](https://grafana.com/docs/promtail/latest/)
- [LogQLcheatsheet](https://grafana.com/docs/loki/latest/query/)
- [Grafana 사용 가이드](grafana-guide.md)
- [모니터링 설정 가이드](monitoring-guide.md)
