---
title: Architecture
description: Architecture - architecture module documentation
category: architecture
created: 2026-07-13
updated: 2026-07-21
---

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

## 인증 시스템 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                  SecurityConfig                      │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐ │
│  │ RateLimiter │  │JwtAuthFilter │  │ OAuth2Login│ │
│  │  (분당 5회) │  │  (JWT 검증)  │  │  (소셜)    │ │
│  └──────┬──────┘  └──────┬───────┘  └─────┬──────┘ │
│         │                │                │         │
│         ▼                ▼                ▼         │
│  ┌──────────────────────────────────────────────┐  │
│  │              SecurityFilterChain              │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    ┌─────────┐    ┌──────────┐    ┌───────────┐
    │  Auth   │    │  OAuth2  │    │  Token    │
    │Controller│   │ Success/ │    │ Provider  │
    │         │    │ Failure  │    │ (RS256)   │
    └────┬────┘    └────┬─────┘    └─────┬─────┘
         │              │                │
         ▼              ▼                ▼
    ┌──────────────────────────────────────────────┐
    │              AuthService                      │
    │  - signup() / login() / refresh() / logout() │
    │  - 이벤트 로그 (ISMS-P 2.9.4)               │
    └──────────────────┬───────────────────────────┘
                       │
         ┌─────────────┼─────────────┐
         ▼             ▼             ▼
    ┌─────────┐  ┌───────────┐  ┌──────────┐
    │  User   │  │  Refresh  │  │  Email   │
    │  Repo   │  │  Token    │  │ Service  │
    │         │  │  Repo     │  │          │
    └─────────┘  └───────────┘  └──────────┘
```

## OAuth2 전략 패턴

```
OAuth2UserInfo (인터페이스)
    │
    ├── KakaoOAuth2UserInfo   (kakao_account.email, properties.nickname)
    ├── NaverOAuth2UserInfo   (response.email, response.name)
    ├── GoogleOAuth2UserInfo  (email, name, sub)
    └── GithubOAuth2UserInfo  (login, email)
    
    └── OAuth2UserInfoFactory.create(provider, attributes)
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

## 보안 아키텍처 (ISMS-P 준수)

| ISMS-P 기준 | 적용 항목 | 구현 방법 |
|---|---|---|
| 2.5.3 사용자 인증 | 비밀번호 + OAuth2 | BCrypt + OAuth2 |
| 2.7.1 암호정책 | RS256 JWT | RSA 2048 비대칭키 |
| 2.9.4 로그 관리 | 인증 이벤트 로그 | slf4j + logback |
| 2.11.1 사고예방 | Rate Limiting | 분당 5회 (로그인) |
