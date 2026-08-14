# Auth Frontend

React + TypeScript + Vite 인증 UI

## 개요

- 로그인/회원가입 폼
- 소셜 로그인 (카카오, 네이버, 구글, 깃험)
- 비밀번호 찾기

## 기술 스택

- React 19
- Vite 8
- TypeScript 6
- Tailwind CSS 4

## 시작

```bash
cd modules/auth/frontend
npm install
npm run dev
```

## 구조

```
frontend/
├── src/
│   ├── components/         # 컴포넌트
│   │   ├── LoginForm.tsx        # 로그인 폼
│   │   ├── SignupForm.tsx       # 회원가입 폼
│   │   └── SocialLoginButtons.tsx  # 소셜 로그인 버튼
│   ├── pages/              # 페이지
│   │   ├── AuthCallback.tsx     # 소셜 콜백 처리
│   │   └── AuthError.tsx        # 에러 페이지
│   ├── lib/                # API 클라이언트, 유틸
│   └── App.tsx             # 라우팅
├── index.html
└── package.json
```

## 배포

GitHub Actions에서 master push 시 자동 빌드 및 nginx 배포.
