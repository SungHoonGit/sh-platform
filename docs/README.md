# SH Platform 문서

> SH Platform 개발 문서 및 가이드

---

## 문서 구조

```
docs/
├── architecture/        # 아키텍처 설계
│   ├── architecture.md  # 전체 아키텍처
│   ├── erd.md          # ERD (데이터 모델)
│   ├── sql-ddl.md      # SQL DDL
│   └── standards.md    # 개발 표준
│
├── auth/               # 인증 시스템
│   ├── api-auth.md     # 인증 API 명세
│   ├── login-plan.md   # 로그인 기획
│   ├── oauth2-registration-guide.md  # OAuth2 설정
│   └── ...
│
├── development/        # 개발 가이드
│   └── module-standard.md  # 신규 모듈 표준
│
├── front/              # 프론트엔드
│   └── integration-guide.md  # 연동 가이드
│
├── guides/             # 운영 가이드
│   ├── grafana-guide.md     # Grafana 사용법
│   ├── logging-*.md         # 로깅 설정
│   ├── monitoring-guide.md  # 모니터링
│   └── ...
│
├── infra/              # 인프라 설정
│   └── domain-ssl-setup-guide.md  # 도메인/SSL
│
├── saas/               # SaaS 테넌트 관리
│   └── tenant-management-design.md  # 테넌트 설계
│
└── daily/              # 작업 이력
    └── work-history.md  # 일별 작업 기록
```

---

## 빠른 링크

| 항목 | 문서 |
|------|------|
| 전체 아키텍처 | [architecture/architecture.md](architecture/architecture.md) |
| ERD | [architecture/erd.md](architecture/erd.md) |
| 개발 표준 | [architecture/standards.md](architecture/standards.md) |
| 인증 API | [auth/api-auth.md](auth/api-auth.md) |
| 모듈 표준 | [development/module-standard.md](development/module-standard.md) |
| Grafana | [guides/grafana-guide.md](guides/grafana-guide.md) |
| 테넌트 설계 | [saas/tenant-management-design.md](saas/tenant-management-design.md) |

---

## 현재 진행 상황

| Phase | 내용 | 상태 |
|-------|------|------|
| Phase 1 | 인프라 구축 | ✅ 완료 |
| Phase 2 | 인증 시스템 | ✅ 완료 |
| Phase 3 | 모니터링/로깅 | ✅ 완료 |
| Phase 4 | SaaS 테넌트 | ✅ 기반 완료 |
| Phase 5 | 모듈 개발 | 🔜 진행 예정 |

---

## 관련 프로젝트

| 프로젝트 | 위치 | 설명 |
|----------|------|------|
| sh-platform | /home/ubuntu/sh-platform | 메인 플랫폼 |
| scraper-platform | /home/ubuntu/scraper-platform | 스크래퍼 관리 |
| kakao-bot-oci | /home/ubuntu/kakao-bot-oci | 카카오 봇 |
| job-scraper | /home/ubuntu/job-scraper | 채용 공고 스크래퍼 |
