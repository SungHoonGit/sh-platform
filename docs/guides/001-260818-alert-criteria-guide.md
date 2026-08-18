# 001-260818-alert-criteria-guide.md

## 개요
- **목적**: SH Platform 서버에 적용할 모니터링 알림(Alert) 기준 정리
- **대상**: Grafana Alerting (Prometheus 데이터 소스)
- **작성일**: 2026-08-18
- **서버 사양**: OCI 단일 코어 (ARM64), RAM 5.77GB, MariaDB + 4개 Spring Boot 서비스 + Prometheus/Grafana

---

## 1. 알림 설계 원칙

대중적인 SRE 관행을 요약하면 **"알림은 페이지(즉시 조치)보다 신중하게"** 입니다.

| 원칙 | 설명 |
|------|------|
| **과다 알림 금지 (Alert Fatigue)** | 알림이 너무 많으면 무시하는 습관이 생김. "알림 5회 이상 인간이 조치 안 함" = 알림 삭제 대상 |
| **증상보다 사용자 영향** | CPU 90% 그 자체보다 "사용자가 느끼는 장애"(서비스 다운, 응답 느림)가 중요 |
| **지속 시간 (Sustain/For)** | 순간 스파이크(배포, 백업)는 알림 X. 일정 시간 유지 시에만 알림 |
| **경고(Warning) vs 위급(Critical)** | Warning = "편할 때 확인", Critical = "즉시 조치" |
| **쿨다운 (Cooldown)** | 동일 조건 반복 알림 방지. 30~60분 |
| **가장 중요한 알림 = "메트릭 수집 중단"** | 서버가 죽으면 모든 알림이 조용해짐 → 죽음 감지가 최우선 |

---

## 2. 대중적인 알림 기준 (업계 표준 요약)

| 구분 | 항목 | Warning | Critical | 지속시간 |
|------|------|---------|----------|----------|
| **생존** | 서버 offline / 메트릭 수신 중단 | - | **즉시** | - |
| **CPU** | CPU 사용률 | 85% | 95% | 5분 |
| **CPU** | Load per core | > 2 (30분) | > 4 (15분) | - |
| **메모리** | 사용률 | 85% | 95% | 2~10분 |
| **디스크** | 사용률 | 75~80% | 90% | 5분 |
| **디스크** | inode 사용률 | 85% | - | 5분 |
| **서비스** | 프로세스 다운 / 헬스체크 실패 | - | 즉시 | - |
| **앱** | HTTP 5xx 비율 | - | SLO 초과 시 | - |
| **앱** | 응답 지연 (P99) | - | SLO 초과 시 | - |

> **참고**: CPU 80~90% 그 자체는 알림 대상이 아닐 수 있음 (배포/크롤링 등 정상 작업일 수 있음). "부하가 사용자에게 영향을 줄 때"를 기준으로.

---

## 3. SH Platform 적용 기준

우리 서버 특성(단일 코어, RAM 5.77GB, JVM 힙 768m×3 + 1024m×1)을 반영해 **경고만 먼저 설정** 후 관찰하는 것을 권장.

### 3.1 서버 수준 (Node Exporter)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 |
|---|--------|---------------|------|------|--------|
| 1 | `Server Offline` | `up{job="node-exporter"}` | `== 0` | 즉시 | 🔴 Critical |
| 2 | `CPU 90% 초과` | `100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)` | `> 90` | 5분 | 🟡 Warning |
| 3 | `Memory 90% 초과` | `(1 - node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes) * 100` | `> 90` | 5분 | 🟡 Warning |
| 4 | `Disk 85% 초과` | `(1 - node_filesystem_avail_bytes{mountpoint="/"} / node_filesystem_size_bytes{mountpoint="/"}) * 100` | `> 85` | 5분 | 🟡 Warning |
| 5 | `Load 1분 평균 초과` | `node_load1` | `> 4` (단일코어) | 10분 | 🟡 Warning |

### 3.2 서비스 수준 (Spring Boot Micrometer)

| # | 알림명 | 쿼리 (PromQL) | 조건 | 지속 | 심각도 |
|---|--------|---------------|------|------|--------|
| 6 | `Auth 서비스 다운` | `up{job="sh-platform-auth"}` | `== 0` | 1분 | 🔴 Critical |
| 7 | `Scraper 서비스 다운` | `up{job="sh-platform-scraper"}` | `== 0` | 1분 | 🔴 Critical |
| 8 | `5xx 에러 증가` | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))` | `> 0.05` | 5분 | 🟡 Warning |
| 9 | `응답 지연 (P95)` | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le))` | `> 2s` | 5분 | 🟡 Warning |

> `job` 라벨은 실제 Prometheus 스크랩 설정(`prometheus.yml`)과 일치해야 함.

---

## 4. 이번 장애(재시작 루프) 관점에서 본 경험칙

지난 8/18 장애를 되돌아보면:

| 상황 | 대중적 기준 | 이번 장애에서 |
|------|-------------|---------------|
| CPU 100% | 알림 안 보냄 (정상 작업일 수 있음) | ✅ 재시작 루프라 실제 문제였음 |
| 프로세스 재시작 반복 | - | ⚠️ **`process_restarts` 또는 `up` 요동**으로 감지해야 함 |
| 서비스 다운 | 즉시 알림 | resume/portfolio가 이 기준이면 알림 왔을 것 |

**교훈**: "CPU 알림"만으로는 재시작 루프를 구분 못함. **"up{job} == 0" (서비스 다운) 알림이 실질적으로 이 장애를 잡을 수 있음.**

---

## 5. 알림 설정 방법 (Grafana)

상세 절차는 `docs/grafana-practical-guide.md` 11장 참조. 요약:

1. **SMTP 설정** (`/etc/grafana/grafana.ini`)
   ```ini
   [smtp]
   enabled = true
   host = smtp.gmail.com:587
   user = {MAIL_USERNAME}
   password = {MAIL_PASSWORD}
   from_address = noreply@shplatform.com
   ```
   ```bash
   sudo systemctl restart grafana-server
   ```

2. **Contact points** → Email 채널 등록 (수신 이메일 주소)

3. **Alert rules** → 위 표의 쿼리로 규칙 생성
   - Evaluate every: `1m`
   - For: 표의 지속시간

4. **Notification policies** → 규칙을 이메일 채널에 연결

---

## 6. 적용 우선순위 (권장)

| 단계 | 항목 | 이유 |
|------|------|------|
| 1 | SMTP 설정 + 이메일 테스트 | 모든 알림의 전제 |
| 2 | `Server Offline` (메트릭 중단) | 서버 사망 감지 최우선 |
| 3 | 서비스 다운 (`up == 0`) | 재시작 루프 등 잡을 수 있음 |
| 4 | Disk 85% | DB/로그 디스크 풀림 예방 |
| 5 | Memory 90% | JVM 힙 상향 필요 감지 |
| 6 | CPU 90% (Warning만) | 배포/크롤링과 구분해 관찰 |

---

## 7. 참고 자료
- [Google SRE Book — Monitoring Distributed Systems](https://sre.google/sre-book/monitoring-distributed-systems/)
- [The Four Golden Signals (InfoQ)](https://www.infoq.com/articles/monitoring-SRE-golden-signals)
- [Setting Effective Alert Thresholds (ServerScout)](https://www.serverscout.ie/kb/best-practices/setting-effective-alert-thresholds)
- [Server Monitoring Alert Thresholds (Hyperping)](https://hyperping.com/blog/server-monitoring-alert-thresholds)

---
*작성일: 2026-08-18*