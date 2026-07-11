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

## 물리 배포 (Free Tier 2-Tier)

```
WEB (A1.Flex 1/6GB)              DB (A1.Flex 1/8GB)
┌──────────────────────┐        ┌──────────────────┐
│ nginx (80/443)       │        │ MariaDB          │
│ ├── / → React        │───────→│ ├── sh_pass      │
│ └── /api/* → :8080   │        │ └── ai_housing   │
│                      │        │                  │
│ Spring Boot (:8080)  │        │ (3306 내부만)    │
│ kakao-bot            │        │                  │
│ job-scraper          │        │                  │
└──────────────────────┘        └──────────────────┘
  140.245.95.162                   161.33.138.23
  10.0.0.47                        10.0.0.39
```

> WEB → DB, 내부망(10.0.0.0/24) 통신. 외부는 WEB:80/443만 오픈.
