---
title: README
description: SH Platform - Monorepo 멀티모듈 MSA 플랫폼
category: readme
created: 2026-07-10
updated: 2026-07-21
---

# SH Platform

SH(SungHoon) SaaS 기반 통합 플랫폼. Monorepo + Multi-Module 구조.

## 목표

- 통합 로그인 (SH Pass) — 일반 + 소셜(카카오/네이버/구글/깃험)
- 플랫폼 프레임 — 대시보드, 네비게이션, 관리자
- 독립 모듈 — 각 모듈마다 backend + frontend 독립 개발
- 점진적 MSA 확장 — 게시판, 쇼핑몰 등 연결

## 저장소 구조

```
sh-platform/
├── common/                          # 공통 라이브러리
├── modules/
│   ├── auth/backend/                # 인증 서비스 (port 8080)
│   ├── auth/frontend/               # React 로그인/회원가입
│   ├── scraper/backend/             # 채용공고 수집 (port 8081)
│   ├── scraper/frontend/            # React 스크래퍼 SPA
│   ├── resume/backend/              # 이력서 서비스 (port 8082)
│   └── portfolio/backend/           # 포트폴리오 서비스 (port 8083)
├── platform/frontend/               # 플랫폼 프레임 (대시보드+관리자)
├── docs/                            # 프로젝트 문서
├── scripts/                         # DB 파티션 등 유틸
├── AGENTS.md                        # AI 개발 규칙
└── .env                             # 중앙 설정
```

## 문서 규칙

모든 `.md` 문서는 YAML frontmatter로 시작합니다:

```yaml
---
title: 문서 제목
description: 요약
category: 카테고리명
created: 작성일
updated: 수정일
---
```

카테고리 및 템플릿: [`docs/frontmatter-template.md`](docs/frontmatter-template.md)
AI 개발 규칙: [`AGENTS.md`](AGENTS.md)

## 모듈 현황

| 모듈 | 포트 | 설명 | 상태 |
|------|------|------|------|
| auth | 8080 | 인증 (로그인, OAuth2, JWT, 관리자 API) | 완료 |
| common | - | 공통 라이브러리 (스케줄링, 알림, 파일뷰어) | 완료 |
| scraper | 8081 | 채용공고 수집 + 통합검색 + 스케줄러 | 완료 |
| platform | - | 플랫폼 프레임 (대시보드, 관리자) | 완료 |
| resume | 8082 | 이력서 서비스 | 예정 |
| portfolio | 8083 | 포트폴리오 서비스 | 예정 |

## 서비스 URL

| 서비스 | URL |
|--------|-----|
| 로그인/회원가입 | https://sunghoonyk.duckdns.org/ |
| 플랫폼 프레임 | https://sunghoonyk.duckdns.org/platform/ |
| Auth Swagger | https://sunghoonyk.duckdns.org/swagger-ui/ |
| Scraper Swagger | https://sunghoonyk.duckdns.org/scraper/swagger-ui/ |
| 채용공고 뷰어 | https://sunghoonyk.duckdns.org/scraper/docs/view |
| Javadoc | https://sunghoonyk.duckdns.org/javadoc/ |
| 테스트 리포트 | https://sunghoonyk.duckdns.org/test-reports/ |
| DB 문서 | https://sunghoonyk.duckdns.org/schemaSpy/ |
| Grafana | https://sunghoonyk.duckdns.org/grafana/ |

## 인프라

| VM | IP | 역할 |
|----|-----|------|
| WEB | 140.245.95.162 | nginx + Spring Boot (4개 서비스) |
| DB | 10.0.0.39 | MariaDB 10.11.14 |

- 도메인: sunghoonyk.duckdns.org
- SSL: Let Encrypt (만료 2026-10-10)
- Oracle Always Free A1.Flex: 2 OCPU / 12GB (ARM64)

## 빌드 및 실행

```bash
# 전체 빌드
./gradlew clean build

# 특정 모듈 테스트
./gradlew :modules:auth:backend:test

# 서비스 재시작
sudo systemctl restart sh-platform-auth
sudo systemctl restart sh-platform-scraper
sudo systemctl restart sh-platform-resume
sudo systemctl restart sh-platform-portfolio
```

## 개발 규칙

자세한 내용은 [AGENTS.md](AGENTS.md) 참조.

### 개발 사이클

```
설계(docs/) → DB 설계 → Javadoc → 구현 → JUnit 테스트 → 빌드 검증 → 커밋/푸시
                                                                    ↓
                                          산출물 자동 생성: Swagger + Javadoc + 테스트 리포트 + DB 문서
```

### 커밋 컨벤션

```
feat: 새 기능 / fix: 버그 수정 / docs: 문서 / refactor: 리팩토링 / test: 테스트 / chore: 빌드
```

## 문서

| 문서 | 내용 |
|------|------|
| AGENTS.md | AI 개발 규칙 (구조, 포트, 개발 사이클) |
| docs/development-guide.md | 개발 가이드 (Javadoc, JUnit, Swagger) |
| docs/architecture/standards.md | 개발 표준 (네이밍, 패키지, 컨벤션) |
| docs/PORT-MANAGEMENT.md | 포트/서비스 관리 |
| docs/architecture/erd.md | ERD |
| SCRAPER-GUIDE.md | 스크래퍼 상세 가이드 |
