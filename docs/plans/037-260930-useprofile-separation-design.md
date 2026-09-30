# 037-260930-useprofile-separation-design 설계 문서

## 개요
- **목적**: 035 Phase 3a에서 `useAuth`에 결합된 `/me` 프로필 조회·캐싱을 `useProfile` 훅으로 분리한다 (034 Phase 4)
- **범위**: `packages/shell`, 3앱 어댑터·소비처(호환 유지 목표)
- **작성일**: 2026-09-30
- **작성자**: AI Assistant / 사용자

## 1. 배경 및 이유
- 035 3a의 공통 `useAuth(app)`는 인증 상태(토큰·로그아웃·401 redirect)와 `/me` 조회(`user/loading/error`)를 한 훅에 결합했다.
- **문제점**:
  1. **중복 `/me` 호출**: 같은 트리의 `Layout`·`Schedule`·`Dashboard`가 각각 `useAuth()`를 호출하면 각각 독립 fetch가 발생한다 (스크래퍼 상단 레이아웃 + 페이지에서 동시 호출 시 2~3회).
  2. **관심사 결합**: `AuthGuard`는 `isAuthenticated`/`loading`만 필요한데 훅 호출 자체가 `/me` 조회를 유발한다.
  3. **제어 부재**: 프로필 갱신(데이터 수정 후 재조회)용 `refetch`가 없다.

## 2. 요구 사항
### 2.1 기능 요구 사항
- [ ] FR-001: `useProfile(app)` 신규 — `/me` 조회, `user/loading/error/refetch` 제공
- [ ] FR-002: **전역 캐시**로 같은 토큰에 대한 `/me` fetch를 앱 라이프사이클 내 1회로 제한 (트리 내 다중 호출 공유)
- [ ] FR-003: `useAuth(app)`는 인증 상태·로그아웃을 담당하되 기존 `AuthState` API(user/loading/error 포함)를 **유지** — 소비처·어댑터 무변경 (백워드 호환)
- [ ] FR-004: 로그아웃 시 프로필 캐시 무효화
- [ ] FR-005: 401 redirect 정책(`redirectOn401`+`hashRoute`)은 `/me` fetch 위치(`useProfile`)에 유지

### 2.2 비기능 요구 사항
- React StrictMode 이중 effect에도 fetch 1회 (진행 가드)
- 토큰 변경(access 교체) 시 캐시 미스 → 재조회

## 3. 설계
### 3.1 아키텍처
```
packages/shell/src/
├── useProfile.ts  (신규)  UserProfile 타입 + 전역 캐시 + useProfile + invalidateProfileCache
├── useAuth.ts     (개선)  useProfile 조립 → AuthState 반환, logout에서 invalidateProfileCache 호출
└── index.ts               useProfile/ProfileState/invalidateProfileCache export 추가
```

### 3.2 데이터 모델
```ts
// 전역(모듈 스코프) 캐시 — 토큰과 결합
interface ProfileCache { token: string; profile: UserProfile | null; error: string | null }
let cache: ProfileCache | null = null;
let inflight = false;
let refetchTick = 0;                 // refetch() 강제 재조회용 버전
const listeners = new Set<() => void>();  // 구독 → useSyncExternalStore로 리렌더

interface ProfileState {
  user: UserProfile | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;               // 캐시 비우고 재조회
}
```
- 캐시 히트 조건: `cache.token === getAccessToken()` → fetch 생략
- 미스 시: `apiFetch("/api/v1/auth/me", { redirectOn401: true, hashRoute: APP_HASH_ROUTE[app] })`
- `invalidateProfileCache()`: `cache=null` + emit (logout 시 호출)

### 3.3 API 설계
| 훅 | 반환 | 메모 |
|----|------|------|
| `useProfile(app)` | `{ user, loading, error, refetch }` | 전역 캐시 공유 |
| `useAuth(app)` | 기존 `AuthState` 동일 | 내부에서 `useProfile` 조립, `/me` 직접 호출 제거 |

- `isAuthenticated = !!token && !!user` (기존 정의 유지 — user 미확정 시 보호 로직 불변)

### 3.4 소비처 영향
- **무변경**: 3앱 어댑터(`hooks/useAuth.ts`), `Layout`·`AuthGuard`·`Schedule`·`PlatformLayout`·`Dashboard`·`resume App.tsx` — `AuthState` API 불변
- 기능적 변화: 동일 트리 내 다중 `useAuth` 호출이 `/me` **1회**로 수렴 (캐시)

## 4. 구현 계획
| 단계 | 내용 | 상태 |
|------|------|------|
| Phase 1 | `useProfile.ts` 신규(캐시+훅) + `useAuth` 개선 + index export | 예정 |
| Phase 2 | 게이트: 4앱 `npm run build` + platform lint | 예정 |
| Phase 3 | 배포 + 라이브 검증(마커: `useProfile`/`refetch` 번들 포함, 회귀 마커) | 예정 |
| Phase 4 | 일지·AGENTS 갱신 | 예정 |

## 5. 참고 자료
- 035 3a `d30c348` (packages/shell 신설, useAuth)
- `docs/daily/2026-09-30-work-log.md`

---
*작성일: 2026-09-30*
