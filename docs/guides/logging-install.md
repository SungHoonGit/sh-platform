# 중앙 집중 로깅 설치 가이드

## 1. 사전 요구사항

| 항목 | 값 |
|------|-----|
| 서버 | Oracle A1.Flex (ARM64) |
| OS | Ubuntu 24.04 LTS |
| 메모리 | 2GB 여유 (현재 12GB 중 ~4GB 사용) |
| 디스크 | 로그 보관 기간에 따라 |

---

## 2. Loki 설치

### 2.1 바이너리 다운로드

```bash
# 최신 버전 확인
curl -sL https://api.github.com/repos/grafana/loki/releases/latest | grep tag_name

# ARM64 바이너리 다운로드
cd /tmp
curl -sL -o loki-linux-arm64.zip https://github.com/grafana/loki/releases/download/v3.7.3/loki-linux-arm64.zip

# 압축 해제 및 설치
unzip -o loki-linux-arm64.zip
sudo mv loki-linux-arm64 /usr/local/bin/loki
sudo chmod +x /usr/local/bin/loki

# 확인
loki --version
```

### 2.2 사용자 및 디렉토리 생성

```bash
# loki 사용자 생성
sudo useradd --no-create-home --shell /bin/false loki

# 디렉토리 생성
sudo mkdir -p /etc/loki /var/lib/loki /var/log/loki

# 권한 설정
sudo chown -R loki:loki /var/lib/loki /var/log/loki
```

### 2.3 설정 파일 생성

```bash
sudo tee /etc/loki/loki-config.yaml > /dev/null << LOKEOF
auth_enabled: false

server:
  http_listen_port: 3100
  grpc_listen_port: 9096

common:
  path_prefix: /var/lib/loki
  storage:
    filesystem:
      chunks_directory: /var/lib/loki/chunks
      rules_directory: /var/lib/loki/rules
  replication_factor: 1
  ring:
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: "2024-01-01"
      store: tsdb
      object_store: filesystem
      schema: v13
      index:
        prefix: index_
        period: 24h

limits_config:
  reject_old_samples: true
  reject_old_samples_max_age: 168h
  max_query_series: 5000
  ingestion_rate_mb: 10
  ingestion_burst_size_mb: 20

compactor:
  working_directory: /var/lib/loki/compactor
  compaction_interval: 10m
  retention_enabled: true
  retention_delete_delay: 2h
  retention_delete_worker_count: 150
  delete_request_store: filesystem

analytics:
  reporting_enabled: false
LOKEOF
```

### 2.4 systemd 서비스 등록

```bash
sudo tee /etc/systemd/system/loki.service > /dev/null << SVCEOF
[Unit]
Description=Loki Log Aggregation System
Documentation=https://grafana.com/docs/loki/
After=network-online.target
Wants=network-online.target

[Service]
User=loki
Group=loki
Type=simple
ExecStart=/usr/local/bin/loki -config.file=/etc/loki/loki-config.yaml
Restart=always
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
SVCEOF

sudo systemctl daemon-reload
sudo systemctl enable loki
sudo systemctl start loki
```

### 2.5 동작 확인

```bash
# 상태 확인
sudo systemctl status loki

# 헬스 체크
curl -s http://localhost:3100/ready

# 라벨 확인
curl -s http://localhost:3100/loki/api/v1/labels
```

---

## 3. Promtail 설치

### 3.1 바이너리 다운로드

```bash
# Promtail은 v3.4.x에서 마지막 포함 (이후 버전에서는 제외)
cd /tmp
curl -sL -o promtail-linux-arm64.zip https://github.com/grafana/loki/releases/download/v3.4.2/promtail-linux-arm64.zip

# 압축 해제 및 설치
unzip -o promtail-linux-arm64.zip
sudo mv promtail-linux-arm64 /usr/local/bin/promtail
sudo chmod +x /usr/local/bin/promtail

# 확인
promtail --version
```

### 3.2 설정 파일 생성

```bash
sudo mkdir -p /etc/promtail /var/lib/promtail

sudo tee /etc/promtail/promtail-config.yaml > /dev/null << PREOF
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /var/lib/promtail/positions.yaml

clients:
  - url: http://localhost:3100/loki/api/v1/push

scrape_configs:
  - job_name: spring-boot
    static_configs:
      - targets:
          - localhost
        labels:
          job: spring-boot
          __path__: /home/ubuntu/sh-platform/logs/*.log

  - job_name: nginx-access
    static_configs:
      - targets:
          - localhost
        labels:
          job: nginx
          type: access
          __path__: /var/log/nginx/access.log

  - job_name: nginx-error
    static_configs:
      - targets:
          - localhost
        labels:
          job: nginx
          type: error
          __path__: /var/log/nginx/error.log

  - job_name: syslog
    static_configs:
      - targets:
          - localhost
        labels:
          job: syslog
          __path__: /var/log/syslog

  - job_name: auth-log
    static_configs:
      - targets:
          - localhost
        labels:
          job: auth
          __path__: /var/log/auth.log
PREOF
```

### 3.3 systemd 서비스 등록

```bash
sudo tee /etc/systemd/system/promtail.service > /dev/null << SVCEOF
[Unit]
Description=Promtail Log Agent
Documentation=https://grafana.com/docs/loki/latest/send-data/promtail/
After=network-online.target loki.service
Wants=network-online.target

[Service]
User=root
Group=root
Type=simple
ExecStart=/usr/local/bin/promtail -config.file=/etc/promtail/promtail-config.yaml
Restart=always
RestartSec=5
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
SVCEOF

sudo systemctl daemon-reload
sudo systemctl enable promtail
sudo systemctl start promtail
```

### 3.4 동작 확인

```bash
# 상태 확인
sudo systemctl status promtail

# 로그 확인
sudo journalctl -u promtail -f
```

---

## 4. Grafana에 Loki 등록

### 4.1 Datasource Provisioning

```bash
sudo tee /etc/grafana/provisioning/datasources/loki.yml > /dev/null << DSEOF
apiVersion: 1

datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://localhost:3100
    isDefault: false
    editable: true
    jsonData:
      maxLines: 1000
DSEOF

# Grafana 재시작
sudo systemctl restart grafana-server
```

### 4.2 Datasource 확인

1. Grafana 접속: `https://sunghoonyk.duckdns.org/grafana/login`
2. ⚙️ Connections → Data sources
3. `Loki` 확인 → **Save & Test** 클릭

---

## 5. 테스트

### 5.1 Loki API 테스트

```bash
# 라벨 확인
curl -s http://localhost:3100/loki/api/v1/labels

# job 값 확인
curl -s http://localhost:3100/loki/api/v1/label/job/values

# nginx 로그 조회
curl -s "http://localhost:3100/loki/api/v1/query_range?query=%7Bjob%3D%22nginx%22%7D&limit=5"

# spring-boot 로그 조회
curl -s "http://localhost:3100/loki/api/v1/query_range?query=%7Bjob%3D%22spring-boot%22%7D&limit=5"
```

### 5.2 Grafana 테스트

1. Explore 선택 → Loki 데이터소스 선택
2. 쿼리 입력: `{job="nginx"}`
3. **Run query** 클릭 → 로그 표시 확인

---

## 6. 서비스 관리

```bash
# Loki
sudo systemctl status loki
sudo systemctl restart loki
sudo systemctl stop loki
sudo journalctl -u loki -f

# Promtail
sudo systemctl status promtail
sudo systemctl restart promtail
sudo systemctl stop promtail
sudo journalctl -u promtail -f
```

---

## 7. 설정 파일 위치

| 파일 | 경로 |
|------|------|
| Loki 설정 | `/etc/loki/loki-config.yaml` |
| Promtail 설정 | `/etc/promtail/promtail-config.yaml` |
| Loki systemd | `/etc/systemd/system/loki.service` |
| Promtail systemd | `/etc/systemd/system/promtail.service` |
| Loki 데이터 | `/var/lib/loki/` |
| Grafana Loki Datasource | `/etc/grafana/provisioning/datasources/loki.yml` |

---

## 8. 리소스 사용량

| 컴포넌트 | CPU | 메모리 | 디스크 |
|----------|-----|--------|--------|
| Loki | ~50ms | ~50MB | 로그 보관량에 따라 |
| Promtail | ~10ms | ~15MB | positions.yaml만 |
| **합계** | ~60ms | ~65MB | - |

---

## 9. 문제 해결

### Q: Loki 시작 실패

```bash
# 로그 확인
sudo journalctl -u loki --no-pager -n 50

# 설정 파일 검증
loki -config.file=/etc/loki/loki-config.yaml -verify-config
```

### Q: Promtail이 로그를 수집하지 않음

```bash
# Promtail 로그 확인
sudo journalctl -u promtail --no-pager -n 50

# 로그 파일 존재 여부 확인
ls -la /home/ubuntu/sh-platform/logs/
ls -la /var/log/nginx/

# 파일 권한 확인
stat /home/ubuntu/sh-platform/logs/sh-platform.log
```

### Q: Grafana에서 Loki 연결 실패

```bash
# Loki 상태 확인
curl -s http://localhost:3100/ready

# Grafana 로그 확인
sudo tail -f /var/log/grafana/grafana.log | grep loki
```

---

## 10. 참고 자료

- [개념 가이드](logging-concept.md)
- [사용 가이드](logging-guide.md)
- [Loki 공식 문서](https://grafana.com/docs/loki/latest/)
