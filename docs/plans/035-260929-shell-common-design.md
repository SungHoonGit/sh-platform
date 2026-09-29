# 035-260929-shell-common-design

# 공통 셸 패키지 @sh-platform/shell — useAuth·셸 컴포넌트 공통화 (034 Phase 3)

## 개요
- **목적**: 3개 앱에 "복제-동기화" 상태로 복제되어 있던 인증 훅(useAuth)과 통합 셸 컴포넌트를 단일 패키지로 통일 — MSA 셸 방향(034 Phase 3)
- **범위**: `packages/shell`(`@sh-platform/shell`) 신설 + platform/scraper/resume 적용 (auth 앱은 로그인 페이지만 사용하므로 제외)
- **작성일**: 2026-09-29
- **작성자**: AI Assistant + 사용자

## 1. 배경 및 이유

2026-09-29 실측 증거:

| 항목 | 실측 결과 |
|------|-----------|
| `shell/` 5종(AppShell/GlobalHeader/SideDrawer/SubNav/tokens) × 3앱 | **15파일** |
| AppShell·SideDrawer·SubNav·tokens.ts | **MD5 바이트 동일** (3벌 완전 사본) |
| GlobalHeader.tsx | platform==scraper 동일, **resume만 1줄 차이** (`loginUrlHere(true)` — hash 라우팅) |
| tokens.ts 주석 | "3개 앱이 동일한 값을 사용한다. **수정 시 3벌 동기화 필수!**" |
| GlobalHeader·AppShell 주석 | `SHELL_VERSION: 1` 마커 — 사본 관리의 증거 |
| useAuth | **3사본** — platform/resume은 주석에 "복제-동기화", scraper만 error 상태 추가 |

- 문제: 셸/인증 변경 시 3곳 동시 수정이 필요하고, 드리프트(사본 간 불일치) 발생 촉매
- 034 Phase 1~2가 redirect·토큰·apiFetch를 공통화했다면, 본 작업은 **코드 사본 자체를 없애는** 마지막 단계

## 2. 요구 사항

- [ ] FR-501: `packages/shell`(`@sh-platform/shell`) 신설 — React 셸 컴포넌트+인증 훅 단일 구현
      (core는 React 무의존 Pure TS이므로 훅은 셸 패키지에 둔다)
- [ ] FR-502: `useAuth` 공통 — `/me` 조회·401 자동 redirect(hashRoute 반영)·logout(appRoot 이동)·error 상태를 **앱 설정 파라미터**로 통일
- [ ] FR-503: 셸 4종(AppShell/GlobalHeader/SideDrawer/SubNav) + 디자인 토큰(T) 공통 — 3앱 사본 삭제
- [ ] FR-504: hash 라우팅 사실 **설정화** `APP_HASH_ROUTE` — resume의 로그인 링크/401 redirect에만 hash 포함 (GlobalHeader 1줄 차이 해소)
- [ ] FR-505: 기존 호출부 API 유지 — AppShell props· DrawerSection 등은 그대로, 앱 레이아웃 코드는 **import 경로만** 변경

### 비기능
- 기존 UX 유지. 의도적 통일은 1곳: logout 이동을 `location.href` → `location.replace`로 통일 (로그아웃 후 뒤로가기 시 만료 페이지 복귀 방지)
- 워링은 034/가이드 011과 동일 패턴 (tsconfig paths + vite alias, 소스 직접 참조)

## 3. 설계

### 3.1 패키지 구조
```
packages/shell/
├── package.json          # name: @sh-platform/shell, exports: ./src/index.ts
├── tsconfig.json         # core paths (패키지 자체 타입체크용)
└── src/
    ├── index.ts
    ├── config.ts         # ShellApp · T · APP_LABELS · APP_HREFS · APP_HASH_ROUTE (app tokens.ts 이전)
    ├── useAuth.ts        # 공통 인증 훅
    ├── AppShell.tsx      # (Phase 3b 이전)
    ├── GlobalHeader.tsx  # (3b — hashRoute는 APP_HASH_ROUTE에서 도출)
    ├── SideDrawer.tsx    # (3b)
    └── SubNav.tsx        # (3b)
```

### 3.2 공통 `useAuth` API
```ts
useAuth(app: ShellApp): AuthState
// AuthState = { isAuthenticated, user, loading, error, logout }
```
- `/me`: `apiFetch(redirectOn401: true, hashRoute: APP_HASH_ROUTE[app])` — 034 Phase 2와 동일 정책
- `logout`: POST `/api/v1/auth/logout`(fire-and-forget) → `clearTokens()` → `setUser(null)` →
  `location.replace(loginUrl(APP_HREFS[app]))` (앱 루트 redirect, 034 FR-404)
- `user`: 슈퍼타입 UserProfile (`role` 포함 — PlatformLayout의 `user?.role === "ADMIN"` 호환)
- 앱 어댑터: 각 앱 `hooks/useAuth.ts`는 아래와 같은 1줄 위임 (호출부 6곳 무변경)
  ```ts
  import { useAuth as useShellAuth } from "@sh-platform/shell";
  export function useAuth() { return useShellAuth("platform"); }  // scraper / resume 동일
  ```

### 3.3 hash 라우팅 설정화
```ts
export const APP_HASH_ROUTE: Record<ShellApp, boolean> = {
  platform: false, scraper: false, resume: true,
};
```
- GlobalHeader 로그인 링크: `loginUrlHere(APP_HASH_ROUTE[currentApp])` — resume 사본의 1줄 차이 해소
- useAuth 401 redirect: 위와 동일 설정 사용

### 3.4 적용 표

| Phase | 내용 | 대상 |
|-------|------|------|
| **3a** | `config.ts` + `useAuth.ts` 신설, 3앱 `hooks/useAuth.ts` 어댑터화, 스크래퍼 `api/auth.ts` 삭제(fetchProfile/logout/useAuth이 흡수) | platform·scraper·resume |
| **3b** | AppShell·GlobalHeader·SideDrawer·SubNav 이전(`./tokens` → 패키지 `config`), 호출부 import 경로 변경, 3앱 `shell/` 디렉터리 삭제(15 → 0) | PlatformLayout, scraper `components/Layout`·`crawlNotifications`, resume `App.tsx` |

### 3.5 워링 (034와 동일 패턴)
1. `tsconfig.app.json` paths: `"@sh-platform/shell": ["{../}packages/shell/src/index.ts"]`
2. `tsconfig.app.json` include: `packages/shell/src` 추가
3. `vite.config.ts` alias: `@sh-platform/shell` → 해당 경로
- 대상: platform·scraper·resume 3앱 (auth는 미사용 — 미워링)

## 4. 구현 계획

| 단계 | 내용 | 비고 |
|------|------|------|
| Phase 3a | shell 패키지(config+useAuth) + 3앱 어댑터 + 스크래퍼 api/auth 삭제 | 즉시 (본 문서) |
| Phase 3b | 셸 4종 이전 + import 재귀 + 앱 shell/ 삭제 | 3a 배포·검증 후 |

## 5. 참고 자료
- `docs/plans/034-260929-frontend-auth-common-design.md` — 인증 redirect·토큰·apiFetch 공통화 (Phase 1~2)
- `docs/guides/011-260828-ui-shared-guide.md` — 공용 패키지 워링 표준 절차
- `docs/errors/012-260828-ui-shared-react-build-error.md` — paths `..` 개수 주의
- `packages/core/` — React 무의존 공통 코어 (tokens/apiFetch/auth)

---
*작성일: 2026-09-29*
