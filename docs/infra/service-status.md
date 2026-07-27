---
title: Service Status
description: 현재 서비스 가동 현황 및 관리 방법
category: infra
created: 2026-07-27
updated: 2026-07-27
---

# 서비스 가동 현황

## 현재 가동 중인 서비스 (2026-07-27 기준)

| 서비스 | 포트 | systemd 서비스 | URL | 상태 |
|--------|------|----------------|-----|------|
| Auth Backend | 8080 | `sh-platform-auth` | `https://sunghoonyk.duckdns.org/api/*` | 가동중 |
| Scraper Backend | 8081 | `sh-platform-scraper` | `https://sunghoonyk.duckdns.org/scraper/*` | 가동중 |
| Resume Backend | 8082 | `sh-platform-resume` | `https://sunghoonyk.duckdns.org/resume/*` | 가동중 |
| Portfolio Backend | 8083 | `sh-platform-portfolio` | `https://sunghoonyk.duckdns.org/portfolio/*` | 가동중 |
| Auth Frontend | - | nginx | `https://sunghoonyk.duckdns.org/` | 배포됨 |
| Scraper Frontend | - | nginx | `https://sunghoonyk.duckdns.org/scraper-ui/` | 배포됨 |
| Platform Frontend | - | nginx | `https://sunghoonyk.duckdns.org/platform/` | 배포됨 |
| nginx | 443/80 | `nginx` | `https://sunghoonyk.duckdns.org` | 가동중 |

## 포트 매핑

| 포트 | 서비스 | 설명 | URL 프리픽스 |
|------|--------|------|-------------|
| 8080 | auth | 인증 (로그인, OAuth2, JWT) | `/api/*`, `/oauth2/*`, `/login/*` |
| 8081 | scraper | 채용공고 수집 + 통합검색 | `/scraper/*` |
| 8082 | resume | 이력서 서비스 | `/resume/*` |
| 8083 | portfolio | 포트폴리오 서비스 | `/portfolio/*` |
| 443 | nginx | SSL 리버스 프록시 | - |

## 프론트엔드 구조

| 경로 | 소스 | 설명 |
|------|------|------|
| `/` | `modules/auth/frontend/dist` | 로그인/회원가입 (SPA) |
| `/platform/` | `platform/frontend/dist` | 플랫폼 프레임 (대시보드+관리자) |
| `/scraper-ui/` | `modules/scraper/frontend/dist` | 채용공고 통합검색 (AuthGuard 적용) |

## 공통 프론트엔드 컴포넌트

`common/frontend/src/` — npm 패키지가 아닌 심볼릭 링크 방식으로 프론트 앱들이 직접 import.

| 컴포넌트 | 경로 | 용도 |
|----------|------|------|
| AuthGuard | `common/frontend/src/components/AuthGuard.tsx` | 미로그인 시 `"/"` 리다이렉트 |
| CommonHeader | `common/frontend/src/components/CommonHeader.tsx` | 상단 네비게이션 |
| useAuth | `common/frontend/src/hooks/useAuth.ts` | 세션 확인 (fetchProfile + refresh) |

- Platform: `platform/frontend/src/layouts/PlatformLayout.tsx`에서 AuthGuard 래핑
- Scraper: `modules/scraper/frontend/src/components/Layout.tsx`에서 AuthGuard 래핑

## 서비스 관리 명령어

```bash
# 상태 확인
sudo systemctl status sh-platform-{auth,scraper,resume,portfolio}

# 전체 상태 한눈에
sudo systemctl status sh-platform-auth sh-platform-scraper sh-platform-resume sh-platform-portfolio --no-pager

# 개별 재시작
sudo systemctl restart sh-platform-auth

# 전체 재시작 (순서 중요: common 의존성 순서)
sudo systemctl stop sh-platform-{portfolio,resume,scraper,auth}
sudo fuser -k 8080/tcp 8081/tcp 8082/tcp 8083/tcp 2>/dev/null
sudo systemctl start sh-platform-auth && sleep 20 && \
sudo systemctl start sh-platform-scraper sh-platform-resume sh-platform-portfolio

# 포트 충돌 해결
ss -tlnp | grep 8080
sudo fuser -k 8080/tcp
```

## 배포 방법

### Backend (Spring Boot)
- **방식**: GitHub Actions CI → SSH 배포 (`gradlew bootRun`)
- **파일**: `.github/workflows/deploy-backend.yml`
- **자동 배포**: `master` 브랜치 push 시 자동 실행

### Frontend (React + Vite)
- **방식**: 서버에서 `git pull && npm install && npm run build`
- **nginx**: `alias`로 `dist/` 디렉토리를 직접 가리키므로 별도 복사 불필요
- **경로**:
  - Scraper: `modules/scraper/frontend/dist/`
  - Platform: `platform/frontend/dist/`
  - Auth: `modules/auth/frontend/dist/`

```bash
# Scraper 프론트엔드 배포
cd /home/ubuntu/sh-platform/modules/scraper/frontend
git pull && npm install && npm run build

# Platform 프론트엔드 배포
cd /home/ubuntu/sh-platform/platform/frontend
git pull && npm install && npm run build
```

## nginx 설정

- 설정 파일: `/etc/nginx/sites-available/sh-platform.conf`
- git 저장소: `nginx/sh-platform.conf`
- 리버스 프록시: `/api/*` → 8080, `/scraper/*` → 8081, `/resume/*` → 8082, `/portfolio/*` → 8083
- SSL: Let Encrypt (만료 2026-10-10)

## 인프라 정보

| 항목 | 값 |
|------|-----|
| VM | OCI A1.Flex 2 OCPU / 12GB ARM64 |
| IP | 140.245.95.162 |
| DB | MariaDB 10.11.14 (10.0.0.39, internal) |
| 도메인 | sunghoonyk.duckdns.org |
| SSH | `ubuntu` / `Whatever4q!` |
| .env | `/home/ubuntu/sh-platform/.env` |

## 문제 해결

| 증상 | 해결 |
|------|------|
| 포트 충돌 | `ss -tlnp \| grep 8080` → `sudo fuser -k 8080/tcp` |
| systemd 무한 재시작 | `Restart=always`로 인한 포트 충돌 → 기존 프로세스 먼저 종료 |
| Swagger에 새 API 안 나옴 | 빈 생성 실패 가능 → 로그 확인 |
| 502 에러 | 해당 포트 백엔드 서비스 다운 → `systemd status` 확인 |
| AuthGuard 리다이렉트 루프 | auth 서비스(8080) 다운 시 발생 → auth 재시작 |
