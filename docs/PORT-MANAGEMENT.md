# 포트 관리 가이드

## 포트 매핑 전체표

### Spring Boot 서비스

| 포트 | 서비스 | systemd | Gradle 모듈 | Swagger | URL 프리픽스 |
|------|--------|---------|------------|---------|-------------|
| 8080 | auth | sh-platform-auth | `:modules:auth:backend` | `/swagger-ui/` | `/api/*`, `/oauth2/*`, `/login/*` |
| 8081 | scraper | sh-platform-scraper | `:modules:scraper:backend` | `/scraper/swagger-ui/` | `/scraper/*` |
| 8082 | resume | sh-platform-resume | `:modules:resume:backend` | `/resume/swagger-ui/` | `/resume/*` |
| 8083 | portfolio | sh-platform-portfolio | `:modules:portfolio:backend` | `/portfolio/swagger-ui/` | `/portfolio/*` |

### 인프라 서비스

| 포트 | 서비스 | 설명 |
|------|--------|------|
| 80 | nginx | HTTP 리버스 프록시 |
| 443 | nginx | HTTPS 리버스 프록시 |
| 3306 | MariaDB | DB (10.0.0.39) |
| 9090 | Prometheus | 메트릭 수집 |
| 3000 | Grafana | 대시보드 시각화 |
| 9100 | Node Exporter | 서버 메트릭 |

### nginx 라우팅 규칙

```
sunghoonyk.duckdns.org/
├── /                       → modules/auth/frontend/dist (로그인)
├── /platform/              → platform/frontend/dist (플랫폼 프레임)
├── /api/*                  → localhost:8080 (auth)
├── /oauth2/*               → localhost:8080 (auth)
├── /login/*                → localhost:8080 (auth)
├── /swagger-ui/*           → localhost:8080 (auth Swagger)
├── /v3/api-docs/*          → localhost:8080 (auth OpenAPI)
├── /scraper/*              → localhost:8081
├── /resume/*               → localhost:8082
├── /portfolio/*            → localhost:8083
├── /test-reports/          → auth test report (정적)
├── /javadoc/               → auth javadoc (정적)
├── /scraper/test-reports/  → scraper test report (정적)
├── /scraper/javadoc/       → scraper javadoc (정적)
├── /schemaSpy/             → DB 문서 (정적)
├── /prometheus/            → localhost:9090
└── /grafana/               → localhost:3000
```

## systemd 서비스 관리

### 서비스 파일 위치

```
/etc/systemd/system/sh-platform-auth.service
/etc/systemd/system/sh-platform-scraper.service
/etc/systemd/system/sh-platform-resume.service
/etc/systemd/system/sh-platform-portfolio.service
```

### 서비스 파일 구조 (auth 예시)

```ini
[Unit]
Description=SH Platform Auth Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/sh-platform
EnvironmentFile=/home/ubuntu/sh-platform/.env
ExecStart=/home/ubuntu/sh-platform/gradlew :modules:auth:backend:bootRun \
    --args=--server.port=8080
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### 주요 명령어

```bash
# 상태 확인
sudo systemctl status sh-platform-auth

# 시작/중지/재시작
sudo systemctl start sh-platform-auth
sudo systemctl stop sh-platform-auth
sudo systemctl restart sh-platform-auth

# 로그 확인
sudo journalctl -u sh-platform-auth --since "5 min ago" -f

# 활성화/비활성화
sudo systemctl enable sh-platform-auth
sudo systemctl disable sh-platform-auth
```

## 포트 충돌 해결

### 진단

```bash
# 포트 8080 사용 프로세스 확인
ss -tlnp | grep 8080

# 모든 Java 프로세스 확인
ps aux | grep java | grep -v grep

# Gradle daemon 상태
./gradlew --status
```

### 해결

```bash
# 1. systemd 서비스 중지
sudo systemctl stop sh-platform-auth

# 2. 포트 점유 프로세스 강제 종료
sudo fuser -k 8080/tcp

# 3. 확인
ss -tlnp | grep 8080

# 4. 재시작
sudo systemctl start sh-platform-auth
```

### 흔한 원인

| 원인 | 증상 | 해결 |
|------|------|------|
| 이전 모너리포 잔존 프로세스 | `java -jar sh-platform-auth/build/libs/...` | `kill {PID}` |
| systemd 재시작 루프 | `Active: activating (auto-restart)` | 포트 확인 후 `fuser -k` |
| Gradle daemon 포트 충돌 | 새 bootRun 실패 | `./gradlew --stop` |
| 수동 실행 잔존 | `java -jar`로 실행한 프로세스 | `kill` 또는 `fuser -k` |

## 전체 서비스 재시작 순서

```bash
# 1. 모든 서비스 중지
sudo systemctl stop sh-platform-portfolio sh-platform-resume sh-platform-scraper sh-platform-auth

# 2. 포트 정리
sudo fuser -k 8080/tcp 8081/tcp 8082/tcp 8083/tcp 2>/dev/null

# 3. Gradle daemon 정리
cd /home/ubuntu/sh-platform && ./gradlew --stop

# 4. 순차 시작 (common 의존성 때문에 auth 먼저)
sudo systemctl start sh-platform-auth
sleep 25  # auth가 완전히 올라갈 때까지 대기
sudo systemctl start sh-platform-scraper
sudo systemctl start sh-platform-resume
sudo systemctl start sh-platform-portfolio

# 5. 헬스체크
curl -s http://localhost:8080/api/health && echo ""
curl -s http://localhost:8081/actuator/health && echo ""
curl -s http://localhost:8082/actuator/health && echo ""
curl -s http://localhost:8083/actuator/health && echo ""
```

## .env 설정

`/home/ubuntu/sh-platform/.env` — systemd와 Spring Boot 모두에서 읽음

```bash
# 포트
AUTH_PORT=8080
SCRAPER_PORT=8081
RESUME_PORT=8082
PORTFOLIO_PORT=8083

# 도메인
DOMAIN=sunghoonyk.duckdns.org

# DB
DB_HOST=10.0.0.39
DB_PORT=3306
DB_USER=sh_user
DB_PASS=SHpass1234!

# API URL
AUTH_BASE_URL=https://sunghoonyk.duckdns.org
SCRAPER_BASE_URL=https://sunghoonyk.duckdns.org/scraper
RESUME_BASE_URL=https://sunghoonyk.duckdns.org/resume
PORTFOLIO_BASE_URL=https://sunghoonyk.duckdns.org/portfolio

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```
