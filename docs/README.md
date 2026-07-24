---
title: SH Platform 문서
description: 프로젝트 전체 문서 인덱스
category: root
created: 2026-07-14
updated: 2026-07-23
---

# SH Platform 문서

## 디렉토리 구조

```
docs/
├── architecture/    # 아키텍처 설계, DB 표준
├── auth/            # 인증 시스템 (JWT, OAuth2, 이메일)
├── common/          # 공통 모듈 (scheduling, notification)
├── daily/           # 작업 일지
├── database/        # DB 파티셔닝, 스키마
├── development/     # 개발 가이드, 모듈 표준
├── front/           # 프론트엔드 설계 (React, Vite, Tailwind)
├── guides/          # 운영/설정 가이드 (nginx, Swagger, 모니터링 등)
├── infra/           # 인프라 (OCI, SSL, 포트 관리)
├── plans/           # 이슈 분석, 개선 계획
├── saas/            # SaaS 테넌트 관리 설계
├── scraper/         # 스크래퍼 모듈 문서
└── archive/         #Obsolete 문서 (V1, V2 프론트엔드 설계)
```

## 문서 목록

### 아키텍처 (`architecture/`)
| 문서 | 설명 |
|------|------|
| [아키텍처 개요](./architecture/architecture.md) | 전체 시스템 구조 |
| [ERD](./architecture/erd.md) | 데이터 모델 |
| [DB 설계 표준](./architecture/db-standards/db-design-standard.md) | 테이블/컬럼 네이밍 컨벤션 |
| [SQL DDL](./architecture/sql-ddl.md) | 테이블 생성 스크립트 |
| [플랫폼 아키텍처 설계](./architecture/platform-architecture-design.md) | MSA 구조 설계 |
| [코딩 표준](./architecture/standards.md) | Java/Kotlin 코딩 컨벤션 |
| [Monorepo 리팩토링](./architecture/MONOREPO-RESTRUCTURING.md) | 멀티모듈 전환 계획 |

### 인증 (`auth/`)
| 문서 | 설명 |
|------|------|
| [API 인증 가이드](./auth/api-auth.md) | JWT/OAuth2 인증 흐름 |
| [로그인 설계](./auth/login-plan.md) | 로그인/회원가입 플로우 |
| [OAuth2 제공자 등록](./auth/oauth2-registration-guide.md) | 카카오/네이버/구글/GitHub 설정 |
| [OAuth2 표준 가이드](./auth/oauth2-provider-standard-guide.md) | 소셜 로그인 표준 |
| [계정 연동 설계](./auth/account-linking-design.md) | 소셜 계정 연동 |
| [프론트엔드 인증 가이드](./auth/frontend-auth-guide.md) | 프론트 JWT 처리 |
| [Gmail SMTP 설정](./auth/gmail-smtp-setup.md) | 이메일 발송 설정 |
| [Redis 개념](./auth/redis-concept.md) | JWT 블랙리스트용 Redis |
| [Redis 도입 계획](./auth/redis-integration-plan.md) | Redis 통합 계획 |

### 프론트엔드 (`front/`)
| 문서 | 설명 |
|------|------|
| [프론트엔드 설계 V3](./front/REACT-FRONTEND-DESIGN-V3.md) | **최신** React/Vite/Tailwind 설계 |
| [관리자 플랫폼 설계](./front/ADMIN-PLATFORM-DESIGN.md) | 플랫폼 프레임 레이아웃 |
| [다국어 처리](./front/i18n.md) | i18n 방안 |
| [프론트 통합 가이드](./front/integration-guide.md) | 프론트-백엔드 연동 |
| [프론트 README](./front/README.md) | 프론트엔드 모듈 개요 |

### 가이드 (`guides/`)
| 문서 | 설명 |
|------|------|
| [개발 가이드](./development/development-guide.md) | 전체 개발 프로세스 |
| [Nginx 가이드](./guides/nginx-guide.md) | 리버스 프록시 설정 |
| [Nginx 설정 관리](./infra/nginx-management.md) | nginx 설정 방식 분석 |
| [Swagger 가이드](./guides/swagger-guide.md) | API 문서 자동 생성 |
| [모니터링 가이드](./guides/monitoring-guide.md) | Prometheus/Grafana 설정 |
| [Grafana 실습](./guides/grafana-practical-guide.md) | Grafana 대시보드 구축 |
| [Javadoc 가이드](./guides/javadoc-guide.md) | 코드 문서 자동 생성 |
| [테스트 리포트 가이드](./guides/test-report-guide.md) | JUnit 테스트 결과 |
| [스키마스파이 가이드](./guides/schemaSpy-guide.md) | DB 문서 자동 생성 |
| [로깅 가이드](./guides/logging-guide.md) | 로깅 설정 |
| [GitHub CLI 가이드](./guides/github-cli-guide.md) | gh CLI 설치/사용 |
| [opencode 가이드](./guides/OPENCODE-GUIDE.md) | AI 코딩 에이전트 설정 |

### 인프라 (`infra/`)
| 문서 | 설명 |
|------|------|
| [OCI 인프라](./infrastructure/oci-infrastructure.md) | Oracle Cloud 서버/DB 설정 |
| [OCI 플랫폼 가이드](./infra/OCI-PLATFORM-GUIDE.md) | OCI A1.Flex 설정 |
| [도메인/SSL 설정](./infra/domain-ssl-setup-guide.md) | DuckDNS + Let's Encrypt |
| [포트 관리](./infra/PORT-MANAGEMENT.md) | 포트/서비스 매핑 |

### 데이터베이스 (`database/`)
| 문서 | 설명 |
|------|------|
| [DB 파티셔닝](./database/partitioning-guide.md) | MariaDB 파티셔닝 |

### 모듈
| 문서 | 위치 | 설명 |
|------|------|------|
| [공통 모듈](./common/modules.md) | common/ | scheduling, notification |
| [스크래퍼 아키텍처](./scraper/architecture.md) | scraper/ | 스크래퍼 모듈 구조 |
| [스크래퍼 로드맵](./scraper/roadmap.md) | scraper/ | 스크래퍼 개발 계획 |

### 기타
| 문서 | 설명 |
|------|------|
| [로드맵](./roadmap.md) | 전체 프로젝트 로드맵 |
| [SaaS 테넌트 설계](./saas/tenant-management-design.md) | 테넌트 관리 설계 |
| [작업 일지](./daily/) | 일별 작업 기록 |

## 빠른 링크

### 서버 접속
```bash
ssh oci-web  # 웹 서버 (140.245.95.162)
ssh oci-db   # DB 서버 (10.0.0.39)
```

### 주요 URL
- 메인: https://sunghoonyk.duckdns.org/
- Swagger: https://sunghoonyk.duckdns.org/swagger-ui/
- Grafana: https://sunghoonyk.duckdns.org/grafana/
- Prometheus: https://sunghoonyk.duckdns.org/prometheus/
- 스크래퍼: https://sunghoonyk.duckdns.org/scraper-ui/
- 플랫폼: https://sunghoonyk.duckdns.org/platform/
