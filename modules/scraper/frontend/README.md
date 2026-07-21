---
title: Readme
description: Readme - general module documentation
category: general
created: 2026-07-16
updated: 2026-07-21
---

# SH Platform Frontend

React + TypeScript + Vite.

## 시작

```bash
cd frontend/web
npm install
npm run dev
```

## 구조

```
frontend/web/
├── src/
│   ├── components/    # 공통 컴포넌트 (LoginForm, SocialLoginButtons 등)
│   ├── pages/         # 페이지 (AuthCallback, AuthError 등)
│   ├── lib/           # API 클라이언트, 유틸
│   └── App.tsx        # 라우팅
├── public/
└── index.html
```

## API

- Base URL: `https://api.sung-hoon.io/api/v1`
- 인증 API 명세: `docs/auth/api-auth.md`
- 프론트 인증 구현 가이드: `docs/auth/frontend-auth-guide.md`

## 배포

main 브랜치에 push 시 GitHub Actions → WEB VM nginx 자동 배포.
