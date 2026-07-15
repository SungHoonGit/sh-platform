# SH Platform

SH(SungHoon) SaaS 기반 통합 플랫폼. Monorepo + Multi-Module 구조.

## 목표

- 통합 로그인 (SH Pass) — 일반 + 소셜(카카오/네이버/구글/깃험)
- API Gateway — 서비스 라우팅/JWT 검증
- SSO — 여러 플랫폼에서 하나의 계정으로 로그인
- 점진적 MSA 확장 — 게시판, 쇼핑몰 등 연결

## 저장소 구조

```
sh-platform/
├── settings.gradle.kts
├── build.gradle.kts
├── sh-platform-auth/            # 인증 서비스
├── sh-platform-common/          # 공통 라이브러리
├── scraper-platform-backend/    # 채용공고 수집 서비스
├── resume-platform/             # 이력서 서비스
├── portfolio-platform/          # 포트폴리오 서비스
├── SCRAPER-GUIDE.md             # 스크래퍼 상세 가이드
└── docs/
```

## 모듈 설명

| 모듈 | 포트 | 설명 | 상태 |
|------|------|------|------|
| sh-platform-auth | 8080 | 인증 서비스 (로그인, OAuth2, JWT, SSO) | ✅ 완료 |
| sh-platform-common | - | 공통 라이브러리 (파일뷰어, 스케줄링, 알림) | ✅ 완료 |
| scraper-platform-backend | 8081 | 채용공고 수집 + 뷰어 | ✅ 완료 |
| resume-platform | 8082 | 이력서 서비스 | ⏳ 예정 |
| portfolio-platform | 8083 | 포트폴리오 서비스 | ⏳ 예정 |

## 서비스 URL

| 서비스 | URL |
|--------|-----|
| Auth Swagger | https://sunghoonyk.duckdns.org/swagger-ui/ |
| Scraper Swagger | https://sunghoonyk.duckdns.org/scraper/swagger-ui/index.html |
| 뷰어 | https://sunghoonyk.duckdns.org/scraper/docs/view |
| 크롤링 설정 API | https://sunghoonyk.duckdns.org/scraper/crawl-config |

## 인프라

| VM | IP | 역할 |
|----|-----|------|
| WEB | 140.245.95.162 | nginx + Spring Boot (4개 서비스) |
| DB | 10.0.0.39 (internal) | MariaDB 10.11.14 |

- 도메인: `sunghoonyk.duckdns.org`
- SSL: Let Expire 2026-10-10
- Oracle Always Free A1.Flex: 2 OCPU / 12GB (ARM64)

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 개별 모듈
./gradlew :scraper-platform-backend:compileJava
./gradlew :scraper-platform-backend:bootRun --args='--server.port=8081'

# systemd
sudo systemctl restart sh-platform-{auth,scraper,resume,portfolio}
```

## 문서

- [스크래퍼 가이드](SCRAPER-GUIDE.md) — 크롤러, API, 설정, 뷰어 상세
