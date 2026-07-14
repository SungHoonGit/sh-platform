# 신규 모듈 표준: 모니터링 + 로깅

> 모든 신규 모듈은 반드시 모니터링과 로깅을 포함해야 합니다.

---

## 1. 표준 구성

```
신규 모듈
├── 모니터링 (Prometheus)
│   ├── 시스템 메트릭 (Node Exporter - 자동)
│   ├── 앱 메트릭 (Micrometer/prometheus_client)
│   └── DB 메트릭 (mysqld_exporter - 자동)
│
└── 로깅 (Loki + Promtail)
    ├── 앱 로그 (파일 또는 journald)
    └──慢查询 로그 (MariaDB - 자동)
```

---

## 2. 언어별 설정 가이드

### 2.1 Spring Boot (Java/Kotlin)

**모니터링:**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
```

```xml
implementation("io.micrometer:micrometer-registry-prometheus")
```

**로깅:**

```yaml
logging:
  file:
    name: /home/ubuntu/{project}/logs/{project}.log
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 7
```

**Promtail 설정:**

```yaml
- job_name: {project}
  static_configs:
    - targets:
        - localhost
      labels:
        job: {project}
        __path__: /home/ubuntu/{project}/logs/*.log
```

### 2.2 Python (Flask/Django/FastAPI)

**모니터링:**

```python
# requirements.txt
prometheus_client==0.21.0

# app.py
from prometheus_client import start_http_server, Counter, Histogram

REQUEST_COUNT = Counter("app_requests_total", "Total requests", ["method", "endpoint"])
REQUEST_LATENCY = Histogram("app_request_duration_seconds", "Request latency", ["endpoint"])

start_http_server(9090)
```

**로깅:**

```python
import logging
from logging.handlers import RotatingFileHandler

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(name)s] %(levelname)s %(message)s",
    handlers=[
        RotatingFileHandler(
            "/home/ubuntu/{project}/logs/{project}.log",
            maxBytes=10*1024*1024,
            backupCount=7
        ),
        logging.StreamHandler()
    ]
)
```

**Promtail 설정:**

```yaml
- job_name: {project}
  journal:
    max_age: 12h
    labels:
      job: {project}
  relabel_configs:
    - source_labels: ["__journal__systemd_unit"]
      regex: "{project}.service"
      target_label: systemd_unit
```

### 2.3 Node.js

**모니터링:**

```javascript
const promClient = require("prom-client");
promClient.collectDefaultMetrics();
app.listen(9090);
```

**로깅:**

```javascript
const winston = require("winston");
const logger = winston.createLogger({
  transports: [
    new winston.transports.File({
      filename: "/home/ubuntu/{project}/logs/{project}.log",
      maxsize: 10485760,
      maxFiles: 7
    })
  ]
});
```

---

## 3. 필수 설정 체크리스트

- [ ] **모니터링**: 메트릭 엔드포인트 노출 (포트 9090)
- [ ] **로깅**: 로그 파일 경로 + 로테이션 설정
- [ ] **Promtail 설정 추가**
- [ ] **systemd 서비스 등록**

---

## 4. 현재 등록된 서비스

| 서비스 | 모니터링 | 로깅 | 비고 |
|--------|----------|------|------|
| Spring Boot | ✅ Micrometer | ✅ 파일 로그 | Actuator |
| nginx | ✅ Node Exporter | ✅ 파일 로그 | access/error |
| MariaDB | ✅ mysqld_exporter | ✅慢查询 로그 | slow_query_log |
| kakao-bot-oci | ❌ 미설정 | ✅ journald | prometheus_client 필요 |

---

## 5. 참고 자료

- [중앙 집중 로깅 개념](logging-concept.md)
- [설치 가이드](logging-install.md)
- [사용 가이드](logging-guide.md)
- [Grafana 실습 가이드](grafana-practical-guide.md)
