# OCI Platform 가이드 (회사 opencode용)

## 플랫폼 개요

| 항목 | 값 |
|------|-----|
| 플랫폼명 | SH Platform (SaaS) |
| 클라우드 | Oracle Cloud Infrastructure (Always Free) |
| 서버 | Ubuntu 24.04, ARM64, 2 OCPU / 12GB RAM |
| IP | 140.245.95.162 |
| 도메인 | sunghoonyk.duckdns.org |
| DB | MariaDB 10.11.14 (내부 10.0.0.39) |

## 아키텍처

```
monorepo: sh-platform/
  sh-platform-auth        (8080) - 인증/SSO/OAuth2
  sh-platform-common             - 공통 라이브러리
  scraper-platform         (8081) - 스크래퍼 + 문서뷰어
  resume-platform          (8082) - 이력서
  portfolio-platform       (8083) - 포트폴리오
```

- **ID 전략**: Auto-increment (Long)
- **DB**: MariaDB 10.11, InnoDB, utf8mb4
- **파티셔닝**: crawl_log, schedule_log, notification_log -> 월별 RANGE

## 서비스 URL

| 서비스 | URL | Swagger |
|--------|-----|---------|
| Auth | https://sunghoonyk.duckdns.org/ | /swagger-ui/ |
| Scraper | https://sunghoonyk.duckdns.org/scraper/ | /scraper/swagger-ui/ |
| Resume | https://sunghoonyk.duckdns.org/resume/ | /resume/swagger-ui/ |
| Portfolio | https://sunghoonyk.duckdns.org/portfolio/ | /portfolio/swagger-ui/ |
| 문서뷰어 | https://sunghoonyk.duckdns.org/scraper/docs/view | - |
| Prometheus | https://sunghoonyk.duckdns.org/prometheus/ | - |
| Grafana | https://sunghoonyk.duckdns.org/grafana/ | - |

## 중앙 설정 (.env)

파일: `/home/ubuntu/sh-platform/.env`

```
AUTH_PORT=8080
SCRAPER_PORT=8081
RESUME_PORT=8082
PORTFOLIO_PORT=8083
DB_HOST=10.0.0.39
DB_USER=sh_user
DB_PASS=SHpass1234!
SCRAPER_BASE_URL=https://sunghoonyk.duckdns.org/scraper
RESUME_BASE_URL=https://sunghoonyk.duckdns.org/resume
PORTFOLIO_BASE_URL=https://sunghoonyk.duckdns.org/portfolio
```

## DB 접속

```bash
# SSH 접속
ssh oci-web

# DB 접속 (root 권한 필요)
sudo mysql

# 앱 계정 접속
mysql -u sh_user -p'SHpass1234!' scraper_platform
```

### DB 목록
| DB | 용도 |
|----|------|
| sh_pass | 인증 |
| scraper_platform | 스크래퍼 |
| resume_platform | 이력서 |
| portfolio_platform | 포트폴리오 |

## 서비스 관리

```bash
# 상태 확인
sudo systemctl status sh-platform-{auth|scraper|resume|portfolio}

# 재시작
sudo systemctl restart sh-platform-scraper

# 로그 확인
sudo journalctl -u sh-platform-scraper -f

# nginx 리로드
sudo nginx -t && sudo systemctl reload nginx
```

## 빌드 및 배포

```bash
# 프로젝트 경로
cd /home/ubuntu/sh-platform

# 빌드
./gradlew :scraper-platform-backend:build -x test

# 전체 빌드
./gradlew build -x test

# 서비스 재시작
sudo systemctl restart sh-platform-scraper
```

## 주요 파일 경로

| 파일 | 위치 |
|------|------|
| .env | /home/ubuntu/sh-platform/.env |
| nginx | /etc/nginx/sites-available/sh-platform |
| systemd | /etc/systemd/system/sh-platform-*.service |
| SSL | /etc/letsencrypt/live/sunghoonyk.duckdns.org/ |
| 스크래퍼 출력 | /home/ubuntu/job-scraper/daily/{java,react}/ |
| 문서뷰어 템플릿 | scraper-platform-backend/src/main/resources/templates/docs/viewer.html |

## SSH 접속

```bash
# SSH 키
~/.ssh/oci/140.245.95.162/ssh-key-2026-07-11.key

# SSH 별칭 (config에서 설정)
ssh oci-web    # 웹 서버
ssh oci-db     # DB 서버 (같은 머신)
```

## 모니터링

- **Prometheus**: 메트릭 수집 (포트 9090)
- **Grafana**: 대시보드 (포트 3000)
- **Loki + Promtail**: 로그 수집
- **Actuator**: /actuator/health, /actuator/prometheus

## 현재 구현된 기능

### Auth (8080)
- OAuth2 소셜 로그인 (카카오, 네이버, Google, GitHub)
- Rate Limiter: 30 req/min
- JWT 토큰 관리

### Scraper (8081)
- 크롤링 설정 관리 (CRUD)
- 사이트 정의 관리
- 스케줄러 설정
- 알림 설정
- **문서 뷰어**: 크롤러별 MD 파일 브라우저, 검색, PDF/Excel 내보내기

### 문서 뷰어
- URL: /scraper/docs/view
- 크롤러 선택 -> 파일 트리 표시
- MD 렌더링 (테이블 지원)
- 전체 검색
- PDF/Excel 내보내기
- 크롤러는 DB crawl_config에서 관리

## 다음 작업 (TODO)

- Swagger Javadoc + 테스트 리포트 통합
- 스케줄러 -> Python 스크래퍼 연동
- 이력서 플랫폼 구현
- 포트폴리오 플랫폼 구현

---
최종 업데이트: 2026-07-15
