---
title: Module Standard
description: Module Standard - development module documentation
category: development
created: 2026-07-14
updated: 2026-07-21
---

# 신규 모듈 표준: 모니터링 + 로깅 + 테넌트 관리

> 모든 신규 모듈은 반드시 모니터링, 로깅, 테넌트 관리를 포함해야 합니다.

---

## 1. 표준 구성

```
신규 모듈
├── 모니터링 (Prometheus)
│   ├── 시스템 메트릭 (Node Exporter - 자동)
│   ├── 앱 메트릭 (Micrometer/prometheus_client)
│   └── DB 메트릭 (mysqld_exporter - 자동)
│
├── 로깅 (Loki + Promtail)
│   ├── 앱 로그 (파일 또는 journald)
│   └── 慢查询 로그 (MariaDB - 자동)
│
└── 테넌트 관리 (SaaS)
    ├── 테넌트 CRUD (생성/조회/수정/삭제)
    ├── 테넌트 멤버 관리 (초대/역할/제거)
    └── 테넌트 컨텍스트 (요청별 테넌트 식별)
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

**테넌트 관리:**

```java
// 1. 테넌트 컨텍스트
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        Long userId = extractUserId(request);
        Tenant tenant = tenantService.getTenantByUserId(userId);
        TenantContext.setCurrent(tenant);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, Exception ex) {
        TenantContext.clear();
    }
}

// 2. 테넌트별 데이터 격리
@Repository
public class BoardRepository {
    
    @Query("SELECT b FROM Board b WHERE b.tenantId = :tenantId")
    List<Board> findByTenantId(@Param("tenantId") Long tenantId);
}
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

## 3. 테넌트 관리 표준

### 3.1 테넌트 모델

```
단일 테넌트 모델 (기본)
→ 사용자당 1개 테넌트 소유
→ 나중에 다중 테넌트로 확장 가능
```

### 3.2 DB 스키마

```sql
-- 테넌트 테이블
CREATE TABLE sh_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) UNIQUE NOT NULL,
    status ENUM(ACTIVE,SUSPENDED,DELETED) DEFAULT ACTIVE,
    plan_type ENUM(FREE,BASIC,PRO,ENTERPRISE) DEFAULT FREE,
    max_users INT DEFAULT 5,
    settings JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 테넌트-사용자 관계
CREATE TABLE sh_tenant_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role ENUM(OWNER,ADMIN,MEMBER,GUEST) DEFAULT MEMBER,
    status ENUM(ACTIVE,INVITED,SUSPENDED) DEFAULT INVITED,
    invited_at TIMESTAMP NULL,
    joined_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_tenant_user (tenant_id, user_id)
);

-- 테넌트 초대장
CREATE TABLE sh_tenant_invitation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    email VARCHAR(200) NOT NULL,
    role ENUM(ADMIN,MEMBER,GUEST) DEFAULT MEMBER,
    token VARCHAR(100) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES sh_tenant(id)
);
```

### 3.3 API 엔드포인트

```
테넌트 관리:
  POST   /api/v1/tenants              - 테넌트 생성
  GET    /api/v1/tenants              - 내 테넌트 목록
  GET    /api/v1/tenants/{id}         - 테넌트 상세
  PUT    /api/v1/tenants/{id}         - 테넌트 수정
  DELETE /api/v1/tenants/{id}         - 테넌트 삭제

테넌트 멤버 관리:
  GET    /api/v1/tenants/{id}/members - 멤버 목록
  POST   /api/v1/tenants/{id}/members - 멤버 초대
  PUT    /api/v1/tenants/{id}/members/{userId} - 멤버 역할 변경
  DELETE /api/v1/tenants/{id}/members/{userId} - 멤버 제거

초대장 관리:
  POST   /api/v1/invitations/{token}/accept - 초대 수락
  DELETE /api/v1/invitations/{token}         - 초대 취소
```

### 3.4 테넌트 컨텍스트

```java
// ThreadLocal 기반 테넌트 컨텍스트
public class TenantContext {
    private static final ThreadLocal<Tenant> currentTenant = new ThreadLocal<>();
    
    public static void setCurrent(Tenant tenant) {
        currentTenant.set(tenant);
    }
    
    public static Tenant getCurrent() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
```

### 3.5 데이터 격리

```java
// 모든 쿼리에 테넌트 ID 필터링
@Repository
public class BoardRepository {
    
    @Query("SELECT b FROM Board b WHERE b.tenantId = :tenantId")
    List<Board> findByTenantId(@Param("tenantId") Long tenantId);
    
    // 자동 테넌트 필터링
    @Query("SELECT b FROM Board b WHERE b.tenantId = :#{#tenantContext.id}")
    List<Board> findAll();
}
```

---

## 4. 필수 설정 체크리스트

- [ ] **모니터링**: 메트릭 엔드포인트 노출 (포트 9090)
- [ ] **로깅**: 로그 파일 경로 + 로테이션 설정
- [ ] **Promtail 설정 추가**
- [ ] **systemd 서비스 등록**
- [ ] **테넌트 관리**: 테넌트 CRUD API 구현
- [ ] **테넌트 멤버**: 초대/역할/제거 API 구현
- [ ] **테넌트 컨텍스트**: 요청별 테넌트 식별
- [ ] **데이터 격리**: 모든 쿼리에 테넌트 ID 필터링

---

## 5. 현재 등록된 서비스

| 서비스 | 모니터링 | 로깅 | 테넌트 관리 | 비고 |
|--------|----------|------|------------|------|
| Spring Boot | ✅ Micrometer | ✅ 파일 로그 | ❌ 미구현 | Actuator |
| nginx | ✅ Node Exporter | ✅ 파일 로그 | N/A | access/error |
| MariaDB | ✅ mysqld_exporter | ✅慢查询 로그 | N/A | slow_query_log |
| kakao-bot-oci | ❌ 미설정 | ✅ journald | N/A | prometheus_client 필요 |

---

## 6. 참고 자료

- [중앙 집중 로깅 개념](logging-concept.md)
- [설치 가이드](logging-install.md)
- [사용 가이드](logging-guide.md)
- [Grafana 실습 가이드](grafana-practical-guide.md)
- [테넌트 관리 설계](../saas/tenant-management-design.md)
