# SH Platform

SH(SungHoon) SaaS 기반 통합 플랫폼. MSA 구조로 점진적 확장.

## 목표

- 통합 로그인 (SH Pass) — 일반 + 소셜(카카오/네이버/구글/깃헙)
- API Gateway — 서비스 라우팅/JWT 검증
- 점진적 MSA 확장 — ai-housing, job-scraper 등 연결

## 저장소 구조

```
sh-platform/
├── README.md
├── docs/
│   ├── architecture.md     # 전체 아키텍처
│   ├── login-plan.md       # 로그인/인증 기획
│   └── roadmap.md          # 로드맵
├── sh-platform-core/       # 향후 Spring Boot 프로젝트
│   ├── auth-service        # 인증 서비스
│   └── gateway             # API Gateway
└── (추후 서비스 모듈)
```

## OCI 인프라

| VM | IP | 역할 | 비고 |
|----|-----|------|------|
| VM1 | 140.245.80.117 | Python 앱 + React Web + SH Platform (Spring) | E2.1.Micro |
| VM2 | 161.33.165.118 | MariaDB (housing) + Spring API (ai-housing) | E2.1.Micro |

> 인프라 설계 상세: https://github.com/SungHoonGit/OCI
