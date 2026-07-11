# 아키텍처

## MSA 구성

```
Client (Web / App)
    │
    ▼
┌──────────────────────────────┐
│  Gateway (sh-platform-core)  │ ← API 진입점 + JWT 검증
│  └── /api/auth/*             │
│  └── /api/housing/* → ai-housing-api
│  └── /api/jobs/*    → job-scraper-api (예정)
└──────────────┬───────────────┘
               │
    ┌──────────┴──────────┐
    │                     │
    ▼                     ▼
┌──────────────┐   ┌──────────────┐
│ Auth Service  │   │ ai-housing   │
│ (JWT 발급)    │   │ (공고 API)   │
│ users DB     │   │ housing DB   │
└──────────────┘   └──────────────┘
```

## 물리 배포 (PAYG 3-Tier)

```
WEB (E2.1.Micro, 1GB)       WAS (A1.Flex, 2/14GB)     DB (A1.Flex, 1/8GB)
┌──────────────────┐       ┌──────────────────┐       ┌──────────────────┐
│ nginx (80/443)   │       │ sh-platform-core │       │ MariaDB          │
│ ├── / → React    │──────→│ (Gateway + Auth) │──────→│ ├── sh_pass      │
│ └── /api/* → WAS │       │ ai-housing-api   │       │ └── ai_housing   │
│                  │       │ (Tomcat :8080)   │       │                  │
│ Python 앱들      │       │                  │       │                  │
└──────────────────┘       └──────────────────┘       └──────────────────┘
```

> WEB → WAS → DB, 모두 내부망(10.0.0.0/24) 통신. 외부는 WEB:80/443만 오픈.
