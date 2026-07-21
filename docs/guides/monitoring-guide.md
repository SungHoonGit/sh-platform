---
title: Monitoring Guide
description: Monitoring Guide - guide module documentation
category: guide
created: 2026-07-13
updated: 2026-07-21
---

# 모니터링 설정 가이드

## 1. 구성 요소

```
[서버 메트릭]  →  Node Exporter  →  Prometheus  →  Grafana
[Spring Boot]  →  Micrometer     →  Prometheus  →  Grafana
[MariaDB]      →  mysqld_exporter →  Prometheus  →  Grafana
```

| 도구 | 용도 | 포트 | 서비스명 |
|------|------|------|----------|
| Prometheus | 메트릭 수집/저장 | :9090 | prometheus |
| Grafana | 대시보드 시각화 | :3000 | grafana-server |
| Node Exporter | 서버 메트릭 (CPU/RAM/Disk) | :9100 | prometheus-node-exporter |
| mysqld_exporter | MariaDB 메트릭 | :9104 | mysqld-exporter |

---

## 2. 접속 경로

| 도구 | URI |
|------|-----|
| Grafana | https://sunghoonyk.duckdns.org/grafana/ |
| Prometheus | https://sunghoonyk.duckdns.org/prometheus/ |

---

## 3. 서비스 관리

```bash
# 상태 확인
sudo systemctl status prometheus
sudo systemctl status grafana-server
sudo systemctl status prometheus-node-exporter
sudo systemctl status mysqld-exporter

# 재시작
sudo systemctl restart prometheus
sudo systemctl restart grafana-server
sudo systemctl restart prometheus-node-exporter
sudo systemctl restart mysqld-exporter
```

---

## 4. 설정 파일 위치

| 파일 | 경로 |
|------|------|
| Prometheus 설정 | `/etc/prometheus/prometheus.yml` |
| Grafana 설정 | `/etc/grafana/grafana.ini` |
| Grafana Datasource provisioning | `/etc/grafana/provisioning/datasources/prometheus.yml` |
| Node Exporter | systemd 기본 설정 |
| nginx | `/etc/nginx/sites-available/sh-platform` |

---

## 5. Prometheus 설정 변경

```bash
sudo nano /etc/prometheus/prometheus.yml
sudo systemctl restart prometheus
```

---

## 6. 문제 해결 (트러블슈팅)

### 6.1 Grafana - "failed to load its application files" (리버스 프록시 오류)

**증상**: Grafana 접속 시 "If you host grafana under a subpath make sure your grafana.ini root_url setting includes subpath" 메시지 표시

**원인**: Grafana를 `/grafana/` 서브패스로 프록시할 때 설정 누락

**해결**:

```bash
# 1. grafana.ini 수정
sudo nano /etc/grafana/grafana.ini

[server]
domain = sunghoonyk.duckdns.org
root_url = https://sunghoonyk.duckdns.org/grafana/
serve_from_sub_path = true

# 2. Grafana 재시작
sudo systemctl restart grafana-server
```

### 6.2 Grafana - 무한 리다이렉트 (301 루프)

**증상**: `/grafana/login` 접속 시 계속 같은 URL로 리다이렉트

**원인**: nginx `proxy_pass`에 trailing slash (`/`)가 있어 Grafana에 전달되는 경로가 잘림

- `proxy_pass http://127.0.0.1:3000/;` → `/grafana/login` → `/login` 으로 변환
- Grafana는 `serve_from_sub_path = true` 상태에서 `/grafana/` prefix를 기대 → 리다이렉트

**해결**:

```nginx
# 수정 전 (문제)
location /grafana/ {
    proxy_pass http://127.0.0.1:3000/;  # trailing slash → 경로 잘림
}

# 수정 후 (해결)
location /grafana/ {
    proxy_pass http://127.0.0.1:3000;   # trailing slash 없음 → 경로 보존
}
```

```bash
sudo nginx -t && sudo systemctl reload nginx
```

**핵심 원리**:
- `proxy_pass` URL에 trailing slash가 있으면 → location 경로가 제거됨
- `proxy_pass` URL에 trailing slash가 없으면 → location 경로가 그대로 전달됨

### 6.3 Spring Boot Actuator 엔드포인트 차단

**증상**: `/actuator/prometheus` 접근 시 302 리다이렉트 (로그인 페이지로 이동)

**원인**: SecurityConfig에서 Actuator 엔드포인트가 permitAll에 포함되지 않음

**해결**:

```java
// SecurityConfig.java
.requestMatchers(
    "/api/v1/auth/signup", "/api/v1/auth/login",
    ...
    "/actuator/health", "/actuator/prometheus",
    "/actuator/info", "/actuator/metrics"
).permitAll()
```

### 6.4 Prometheus 타겟이 down

**증상**: Prometheus UI → Targets 에서 상태가 "down" 표시

**확인 방법**:
```bash
# Node Exporter 확인
curl http://localhost:9100/metrics | head

# Spring Boot 확인
curl http://localhost:8080/actuator/prometheus | head

# mysqld_exporter 확인
curl http://localhost:9104/metrics | head
```

### 6.5 Grafana "No data" 표시

**확인 방법**:
1. Explore → Prometheus 선택
2. 쿼리 입력하여 데이터 존재 확인
3. 시간 범위 설정 확인 (우측 상단 타임피커)

---

## 7. 관련 문서

- [Grafana 사용 가이드](grafana-guide.md)
- [개발 가이드](development-guide.md)
