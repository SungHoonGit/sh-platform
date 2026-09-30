import { useCallback, useEffect, useSyncExternalStore } from "react";
import { apiFetch, getAccessToken } from "@sh-platform/core";
import { APP_HASH_ROUTE, type ShellApp } from "./config";

/** /me 응답 프로필 (슈퍼타입 — 앱은 필요한 필드만 사용) */
export interface UserProfile {
  id: number;
  email: string;
  name: string;
  role: string;
  provider?: string | null;
  emailVerified?: boolean;
  locale?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ProfileState {
  user: UserProfile | null;
  loading: boolean;
  error: string | null;
  refetch: () => void;
}

interface ProfileStore {
  token: string | null;
  user: UserProfile | null;
  loading: boolean;
  error: string | null;
  version: number;
}

let store: ProfileStore = { token: null, user: null, loading: false, error: null, version: 0 };
let inflight = false;
const listeners = new Set<() => void>();

function setStore(next: Partial<ProfileStore>): void {
  store = { ...store, ...next, version: store.version + 1 };
  listeners.forEach((l) => l());
}

function subscribe(onStoreChange: () => void): () => void {
  listeners.add(onStoreChange);
  return () => {
    listeners.delete(onStoreChange);
  };
}

/** (명령형) 프로필 캐시 무효화 — 로그아웃 시 clearTokens()와 함께 호출한다. */
export function invalidateProfileCache(): void {
  setStore({ token: null, user: null, loading: false, error: null });
}

async function fetchProfile(token: string, app: ShellApp): Promise<void> {
  if (inflight) return;
  inflight = true;
  setStore({ token, loading: true, error: null });
  try {
    const res = await apiFetch("/api/v1/auth/me", {
      redirectOn401: true,
      hashRoute: APP_HASH_ROUTE[app],
    });
    if (!res.ok) throw new Error(`ME_${res.status}`);
    const json = await res.json();
    setStore({ token, user: json.data as UserProfile, loading: false, error: null });
  } catch (e) {
    setStore({ token, user: null, loading: false, error: e instanceof Error ? e.message : String(e) });
  } finally {
    inflight = false;
  }
}

/**
 * (명령형) 공통 프로필 훅 — /me 조회를 전역 캐시로 공유한다 (설계 037).
 * 같은 토큰으로 트리 내 여러 곳에서 호출해도 fetch는 1회다.
 *
 * @param app 현재 앱 — 401 redirect의 hashRoute(APP_HASH_ROUTE) 결정
 * @return ProfileState (user·loading·error·refetch)
 */
export function useProfile(app: ShellApp): ProfileState {
  const token = getAccessToken();
  const snap = useSyncExternalStore(subscribe, () => store);

  useEffect(() => {
    if (!token) return;
    const done = snap.token === token && !snap.loading && (snap.user !== null || snap.error !== null);
    if (done) return;
    void fetchProfile(token, app);
  }, [token, app, snap]);

  const refetch = useCallback(() => {
    setStore({ user: null, error: null, loading: false });
  }, []);

  const valid = !!token && snap.token === token;
  const user = valid ? snap.user : null;
  const error = valid ? snap.error : null;
  const loading = !!token && !(valid && !snap.loading && (snap.user !== null || snap.error !== null));

  return { user, loading, error, refetch };
}
