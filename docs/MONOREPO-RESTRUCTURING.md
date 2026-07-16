# Monorepo 리팩토링 계획

## 1. 현재 구조 (문제점)

```
sh-platform/
├── frontend/web/              ← React (scraper만, 위치 불일치)
├── sh-platform-auth/          ← Spring Boot (이름 불일치)
├── sh-platform-common/        ← 공통 라이브러리 (이름 불일치)
├── scraper-platform-backend/  ← Spring Boot (이름 불일치)
├── resume-platform/           ← Spring Boot (이름 불일치)
├── portfolio-platform/        ← Spring Boot (이름 불일치)
├── docs/
├── scripts/
└── keys/
```

**문제점:**
- 네이밍 불일치 (`-platform`, `-platform-backend`, `frontend/web/`)
- 모듈별로 분리되어 있어 확장 시 복잡
- 프론트엔드 위치가 모듈과 분리됨
- 공통 라이브러리 위치 불분명

## 2. 목표 구조

```
sh-platform/
├── modules/
│   ├── auth/
│   │   ├── backend/           ← Spring Boot (8080)
│   │   └── frontend/          ← React (로그인 + 회원가입)
│   ├── scraper/
│   │   ├── backend/           ← Spring Boot (8081)
│   │   └── frontend/          ← React (통합검색, 스케줄, 뷰어)
│   ├── resume/
│   │   ├── backend/           ← Spring Boot (8082)
│   │   └── frontend/          ← React (추후)
│   └── portfolio/
│       ├── backend/           ← Spring Boot (8083)
│       └── frontend/          ← React (추후)
├── platform/                  ← 플랫폼 프레임 (공통 레이아웃)
│   └── frontend/
│       └── src/
│           ├── components/    ← 공통 컴포넌트 (네비게이션, 헤더)
│           ├── layouts/       ← 레이아웃
│           └── pages/         ← 대시보드, 설정 등
├── common/                    ← sh-platform-common (공통 라이브러리)
├── docs/
├── scripts/
└── keys/
```

## 3. 리팩토링 순서

### Phase 1: 디렉토리 이동
1. `sh-platform-auth/` → `modules/auth/backend/`
2. `sh-platform-common/` → `common/`
3. `scraper-platform-backend/` → `modules/scraper/backend/`
4. `resume-platform/` → `modules/resume/backend/`
5. `portfolio-platform/` → `modules/portfolio/backend/`
6. `frontend/web/` → `modules/scraper/frontend/`

### Phase 2: 빌드 설정 업데이트
1. Gradle settings.gradle 수정
2. 각 모듈의 build.gradle 경로 수정
3. nginx 설정 업데이트

### Phase 3: 플랫폼 프레임 구현
1. `platform/frontend/` 생성
2. 공통 레이아웃 컴포넌트
3. 네비게이션 + 헤더
4. 각 모듈 임베딩

### Phase 4: Auth 프론트엔드 구현
1. 로그인 페이지
2. 회원가입 페이지
3. OAuth2 소셜 로그인 버튼
4. JWT 토큰 관리

## 4. 플랫폼 프레임 레이아웃

```
┌─────────────────────────────────────────────────────────┐
│  SH Platform                                    [로그인] │
├──────────┬──────────────────────────────────────────────┤
│ 네비게이션 │                                              │
│          │  ┌────────────────────────────────────────┐  │
│ 📊 대시보드│  │                                        │  │
│ 🔍 스크래퍼│  │  각 모듈의 React SPA가 여기에 렌더링    │  │
│ 📄 이력서  │  │  (iframe 또는 동적 임포트)              │  │
│ 💼 포트폴리오│ │                                        │  │
│          │  └────────────────────────────────────────┘  │
├──────────┴──────────────────────────────────────────────┤
│  © 2026 SH Platform                                     │
└─────────────────────────────────────────────────────────┘
```

## 5. 라우팅 구조

```
https://sunghoonyk.duckdns.org/
├── /                           → 로그인 페이지 (auth/frontend)
├── /signup                     → 회원가입 페이지 (auth/frontend)
├── /platform                   → 플랫폼 프레임 (platform/frontend)
│   ├── /platform/dashboard     → 대시보드
│   ├── /platform/scraper/*     → 스크래퍼 모듈 (scraper/frontend)
│   ├── /platform/resume/*      → 이력서 모듈 (resume/frontend)
│   └── /platform/portfolio/*   → 포트폴리오 모듈 (portfolio/frontend)
├── /api/auth/*                 → 인증 API (auth/backend)
├── /scraper/*                  → 스크래퍼 API (scraper/backend)
├── /resume/*                   → 이력서 API (resume/backend)
└── /portfolio/*                → 포트폴리오 API (portfolio/backend)
```

## 6. nginx 설정

```nginx
# 플랫폼 프레임
location /platform {
    root /home/ubuntu/sh-platform/platform/frontend/dist;
    try_files $uri $uri/ /platform/index.html;
}

# Auth 프론트엔드
location / {
    root /home/ubuntu/sh-platform/modules/auth/frontend/dist;
    try_files $uri $uri/ /index.html;
}

# 스크래퍼 프론트엔드
location /scraper-ui {
    alias /home/ubuntu/sh-platform/modules/scraper/frontend/dist;
    try_files $uri $uri/ /scraper-ui/index.html;
}

# API 프록시
location /api/auth/ {
    proxy_pass http://127.0.0.1:8080/api/auth/;
}
location /scraper/ {
    proxy_pass http://127.0.0.1:8081/;
}
```

## 7. 주의사항

- Gradle 멀티모듈 설정 시 경로 주의
- 각 모듈의 context-path 유지 (`/api/auth`, `/scraper`, etc.)
- 프론트엔드 간 독립적 빌드/배포
- 플랫폼 프레임은 공통 컴포넌트 관리

## 8. 검토 사항

- [ ] OAuth2 콜백 URL 업데이트 필요
- [ ] CORS 설정 업데이트
- [ ] 환경변수 (.env) 업데이트
- [ ] CI/CD 파이프라인 업데이트 (추후)
