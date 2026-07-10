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

## 물리 배포 (현재, VM3 없음)

```
VM1 (E2.1.Micro, 1GB)         VM2 (E2.1.Micro, 1GB)
┌──────────────────┐          ┌──────────────────┐
│ nginx (80/443)   │          │ MariaDB          │
│ ├── / → React    │          │ └── ai_housing   │
│ └── /api/* → VM1 │          │                  │
│                  │          │ (ai-housing-api  │
│ sh-platform-core │          │  jar만 있음)     │
│ (Spring Boot)    │          │                  │
│ Python 앱들      │          │                  │
└──────────────────┘          └──────────────────┘
```

> VM3(A1.Flex) 생성 시 gateway/auth, ai-housing-api 각각 분리 배포
