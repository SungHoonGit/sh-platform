# 034-260929-frontend-auth-common-design

# 프론트 공통 패키지 @sh-platform/core — 인증 redirect 공통화 설계

## 개요
- **목적**: SaaS/MSA 방향성에 맞춰 프론트 4개 앱이 중복·제각각 처리하던 **인증 흐름(세션 만료 redirect·로그인 URL·open-redirect 가드)**을 공통 패키지로 통일
- **범위**: `packages/core`(`@sh-platform/core`) 신설 + platform/scraper/resume/auth 4개 앱 적용
- **작성일**: 2026-09-29
- **작성자**: AI Assistant + 사용자

## 1. 배경 및 이유
- 사용자 제기: "세션 만료 시 로그인 화면으로는 이동하지만 **이전 화면으로 돌아가지 못한다**. 공통(common)으로 핸들링될 줄 알았는데 아닌가 보네."
- 조사 결과:
  - `common/` 모듈은 **백엔드 공통 라이브러리**(ApiResponse/예외처리/JWT)일 뿐 프론트 공통 코드는 없음
  - 로그인 페이지(`Login.tsx`)는 `?redirect=` 를 지원하지만 각 앱이 **현재 위치를 담아 보내지 않음**
    | 앱 | 401/가드 시 이동 | 문제 |
    |----|------------------|------|
    | platform | `/` (redirect 없음) | 현재 경로 손실 |
    | scraper | `/?redirect=/scraper/` 하드코딩 | 항상 앱 루트로만 복귀 |
    | resume | `/?redirect=/resume/` 또는 `/` | hash 라우팅이라 실제 화면(`#/r/1/edit`) 손실 |
  - FE 공용 패키지 인프라는 이미 존재: `packages/ui-shared`(`@sh-platform/ui`, 가이드 011) — 4개 앱이 tsconfig paths+vite alias로 소스 직접 참조

## 2. 요구 사항
- [ ] FR-401: `packages/core`(`@sh-platform/core`) 신설 — 인증 redirect 유틸 단일 구현
- [ ] FR-402: 세션 만료(401)/미로그인 가드 → `?redirect=`에 **현재 위치** 담아 이동 (resume은 hash 포함)
- [ ] FR-403: 로그인 페이지 `redirect` 값을 **상대경로만 허용** (open-redirect 방지: `//`, `\`, 제어문자 차단)
- [ ] FR-404: 로그아웃 → 로그인 페이지 + 앱 루트 redirect (로그인 후 동일 앱으로 복귀)
- [ ] FR-405: 4개 앱 모두 `@sh-platform/core` 워링(paths/include/vite alias) — ui-shared와 동일 패턴
- [ ] FR-406: deploy 워크플로우 paths에 `packages/**` 추가 (공용 패키지 단독 변경도 배포 발동)

### 비기능
- 의존성 0 (Pure TS, React 불필요) — 어떤 앱에서도 즉시 사용 가능
- 기존 `@sh-platform/ui` 패턴과 동일한 소스 참조 방식 (빌드 단계 추가 없음)

## 3. 설계

### 3.1 패키지 구조
```
packages/core/
├── package.json          # name: @sh-platform/core, exports: ./src/index.ts
└── src/
    ├── index.ts          # export
    └── auth.ts           # redirect 유틸
```

### 3.2 공통 API (`src/auth.ts`)
```ts
/** 현재 주소 기준 로그인 URL. hashRoute=true면 hash 포함(hash 라우팅 앱: resume) */
loginUrlHere(hashRoute = false): string        // "/?redirect=" + encodeURIComponent(pathname[+hash])
/** 고정 경로 기준 로그인 URL (앱 루트 등) */
loginUrl(path: string): string
/** 현재 주소 기준 로그인 페이지로 이동 (location.replace) */
redirectToLogin(hashRoute = false): void
/** ?redirect 값 검증 — "/"로 시작하는 상대경로만 통과, 아니면 fallback (FR-403) */
sanitizeRedirect(raw: string | null | undefined, fallback: string): string
```
`sanitizeRedirect` 차단 규칙: 빈 값, `/`로 시작 안 함, `//`(protocol-relative), 백slash 포함, 개행/제어문자.

### 3.3 앱별 적용 표
| 앱 | 지점 | 변경 |
|----|------|------|
| platform | `App.tsx` 미로그인 가드 | `/` → `loginUrlHere()` |
| platform | `AccountSettings` 401 | `/` → `redirectToLogin()` |
| platform | `useAuth.logout` | `/` → `loginUrl("/platform")` |
| scraper | `AuthGuard`, `api/scraper.ts` 401 | 하드코딩 `/scraper/` → `redirectToLogin()` (현재 pathname) |
| scraper | `useAuth.logout` | 동일 동작을 `loginUrl("/scraper/")`로 통일 |
| resume | 페이지 로그인 안내 버튼 3곳 + GlobalHeader | `redirect=/resume/` → `loginUrlHere(true)` (hash 보존) |
| resume | `client.ts` 401 흐름 | 신규 `expireSession()`: 토큰 삭제 + `redirectToLogin(true)` |
| resume | `useAuth.logout`, `client.logout` | `loginUrl("/resume/")` |
| auth | `Login.tsx` redirect | `sanitizeRedirect(..., "/platform")` |
| auth | `AuthCallback.tsx` returnUrl | 동일 가드 |

- 401 자동 이동형(scraper)과 안내 버튼형(resume 페이지)의 기존 UX는 유지하되 **redirect 값만 현재 위치로 통일**
- 세션 만료 시 resume `ApplicationsPage`/`PostingsBrowsePage`의 `logout()` 호출 → `expireSession()`으로 교체(현재 화면 복귀)

### 3.4 워링 (4개 앱 공통, 가이드 011과 동일 패턴)
1. `tsconfig.app.json` paths: `"@sh-platform/core": ["{../}packages/core/src/index.ts"]`
2. `tsconfig.app.json` include: `packages/core/src` 추가
3. `vite.config.ts` alias: `@sh-platform/core` → 해당 경로

## 4. 구현 계획
| 단계 | 내용 | 비고 |
|------|------|------|
| Phase 1 | core 패키지 + 4앱 워링 + 인증 redirect 통일 + open-redirect 가드 + deploy paths | ✅ 완료 (336892a) |
| Phase 2 | 토큰 저장/삭제 공통 + `apiFetch` 래퍼(401 자동 redirect) | ✅ 완료 (본 문서 4.1 참조) |
| Phase 3 (후보) | 공통 `useProfile`( /me 조회), 앱 셸(헤더/드로어) 공통화 → `@sh-platform/shell` | MSA 셸 방향, 별도 설계 |

### 4.1 Phase 2 — 토큰 저장 + `apiFetch` 래퍼 (2026-09-29 구현)

**신규 (`packages/core/src/`)**
- `tokens.ts`: `getAccessToken` / `getRefreshToken` / `setTokens` / `clearTokens` / `hasAccessToken`
  — localStorage 키(`accessToken`/`refreshToken`)의 단일 소스. 4앱 + ui-shared에서 키 문자열 직접 사용 제거.
- `apiFetch.ts`: `apiFetch(url, { auth?, redirectOn401?, hashRoute? })`
  - `auth`(기본 true): `Authorization: Bearer` 자동 부여 (호출부가 지정한 Authorization은 유지)
  - `redirectOn401`: 401 → `clearTokens()` + `redirectToLogin(hashRoute)` — **어떤 401 경로든 현재 화면 보존 복귀**
  - 주의: 로그인/회원가입 등 "401이 정상인" 인증 엔드포인트에는 사용 금지

**적용 표**

| 앱 | 지점 | 변경 |
|----|------|------|
| platform | `api/admin`·`api/master`·`api/dashboard` request | `apiFetch(redirectOn401)` — **기존 401 미처리 공백 해소** (세션 만료 시 stale UI) |
| platform | `useAuth` /me, `AccountSettings` 4곳 | `apiFetch(redirectOn401)` |
| platform | `App.tsx` 미로그인 가드 | `hasAccessToken()` |
| scraper | `api/scraper` request·export·blacklistReq | `apiFetch(redirectOn401)` (기존 명시 `redirectToLogin()` 호출부는 제거 — apiFetch가 위임) |
| scraper | `api/companies`, `api/auth`, `Search` 스크랩, push subscribe/unsubscribe, `CompanySlideOver` export | `apiFetch(redirectOn401)` 또는 `getAccessToken()` |
| scraper | `useAuth` /me·logout | `getAccessToken`/`clearTokens` |
| resume | `client.ts` (request/upload/download/refresh) | **토큰 유틸만 적용** — refresh 갱신 흐름이 401을 선행 처리해야 하므로 `apiFetch` 미사용, throw 정책 유지 |
| resume | `ApplicationsPage`·`PostingsBrowsePage` fetchScraper/blacklistReq | `apiFetch(redirectOn401, hashRoute)` — 기존 `expireSession()` 호출 제거 (동일 동작) |
| resume | `useAuth` /me, 이미지 로더 3종(CrudSection 사진·shared 사진·FileThumb) | `apiFetch(redirectOn401, hashRoute)` |
| resume | `App.tsx` 로그인 가드 | `hasAccessToken()` |
| auth | `Login`·`AuthCallback` 토큰 저장 | `setTokens()` |
| ui-shared | `BlockConfirmDialog` searchReasons | `getAccessToken()`만 (401 자동 redirect는 앱마다 hashRoute 정책이 달라 미적용) |

**제거/미적용 (의도)**
- 제거: resume `client.ts`의 `expireSession()` — Phase 1 신규 API였으나 `apiFetch({redirectOn401, hashRoute:true})`로 대체.
- 미적용: resume `client.ts`의 인증 request 3종(refresh 흐름 선행 필요), auth 앱 인증 엔드포인트(401이 정상 응답), 공유(share) 공개 엔드포인트.

## 5. 참고 자료
- `docs/guides/011-260828-ui-shared-guide.md` — 공용 패키지 워링 표준 절차
- `docs/errors/012-260828-ui-shared-react-build-error.md` — paths `..` 개수 주의
- `packages/ui-shared/` — 선례 패키지
- `modules/auth/frontend/src/pages/Login.tsx` — redirect 수신부

---
*작성일: 2026-09-29*
