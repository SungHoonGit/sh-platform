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
│   ├── architecture.md     # 3-Tier + MSA 아키텍처
│   ├── login-plan.md       # 로그인/인증 기획
│   ├── standards.md        # ⭐ 개발 표준 정의서 (패키지/Naming/응답)
│   ├── erd.md              # ⭐ ERD (users/tokens/codes)
│   ├── i18n.md             # ⭐ 다국어 처리 방안
│   ├── api-auth.md         # ⭐ 인증 API 명세
│   └── roadmap.md          # 로드맵
└── sh-platform-core/       # 향후 Spring Boot 프로젝트
    └── (Phase 2 생성 예정)
```

## OCI 인프라

| VM | Public IP | 내부 IP | 역할 | 사양 |
|----|-----------|---------|------|------|
| WEB | 140.245.95.162 | 10.0.0.47 | nginx + React + Spring Boot + Python | A1.Flex 1/6GB |
| DB | 161.33.138.23 | 10.0.0.39 | MariaDB | A1.Flex 1/8GB |

> Always Free 한도: A1.Flex 2OCPU/12GB (2026.06 변경)
> 
> 인프라 설계 상세: https://github.com/SungHoonGit/OCI
