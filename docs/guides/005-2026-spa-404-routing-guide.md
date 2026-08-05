# SPA 404 페이지 및 라우팅 정리 (2026-08-01)

## 배경

에러 페이지 전략(`docs/security/error-pages.md`)에 따라 SPA 라우팅 404/런타임 에러를 처리.
대표 구현(platform)에서 **전체 프론트(auth·scraper·platform)로 확대** 적용했다.

## 구현 내용

| 항목 | auth | scraper | platform |
|------|------|---------|----------|
| NotFound 페이지 + `*` catch-all | ✅ (다크 테마) | ✅ | ✅ |
| ErrorBoundary 래핑 (`main.tsx`) | ✅ | ✅ | ✅ |
| 라우팅 시점 | `App.tsx` `<Route path="*">` | `App.tsx` `<Route path="*">` | `App.tsx` `<Route path="*">` |

- `NotFound.tsx`: 404 번호 + 안내 문구 + 홈 링크
  - auth → `/` 로그인 화면, scraper → `/search` 통합검색, platform → `/platform` 대시보드
- `ErrorBoundary.tsx`: 런타임 오류 시 ⚠️ + "새로고침" 버튼 (`componentDidCatch`에서 console.error)

## 로그아웃 404 버그 (커밋 e4b2470)

### 증상
- `/platform` 로그아웃 클릭 → **404 페이지 표시**, "대시보드로 돌아가기"/로그아웃 모두 동작 안 함

### 원인
- `PlatformLayout.handleLogout`이 `navigate("/")` 사용 (React Router 내부 이동)
- `App.tsx`는 `localStorage` 토큰을 **렌더 시 한 번만** 읽음 → 토큰 제거 후에도 App이 재렌더되지 않음
- 그 상태에서 `/`로 이동 → 어느 라우트에도 안 맞음 → 새로 추가한 `*` catch-all(NotFound)에 걸림

### 수정
```ts
// PlatformLayout.tsx
const handleLogout = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  window.location.replace("/");   // 전체 리로드 (기존: navigate("/"))
};
```
- scraper는 이미 `window.location.href = "/"` 방식이라 영향 없음

### 교훈
- 토큰 존재를 렌더 타임에 판단하는 앱에서, 로그아웃은 **상태 갱신 없이 전체 리로드**로 처리해야
  catch-all 라우트에 걸리지 않는다.

## 커밋

| 커밋 | 내용 |
|------|------|
| `fa677f8` | platform NotFound + ErrorBoundary + catch-all (대표 구현) |
| `ccee36a` | auth·scraper NotFound + ErrorBoundary 확대 |
| `e4b2470` | 로그아웃 404 수정 (navigate → 전체 리로드) |
