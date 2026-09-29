import { useCallback, useEffect, useState } from "react";
import {
  apiFetch,
  clearTokens,
  getAccessToken,
  getRefreshToken,
  loginUrl,
} from "@sh-platform/core";
import { APP_HASH_ROUTE, APP_HREFS, type ShellApp } from "./config";

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

export interface AuthState {
  isAuthenticated: boolean;
  user: UserProfile | null;
  loading: boolean;
  error: string | null;
  logout: () => void;
}

/**
 * (명령형) 공통 인증 훅 — /me 조회, 세션 만료(401) 자동 redirect, 로그아웃.
 * 3개 앱의 사본 useAuth를 대체한다 (설계 035). 앱별 설정은 app 파라미터로 전달한다.
 *
 * @param app 현재 앱 — logout 이동 경로(APP_HREFS)와 hashRoute(APP_HASH_ROUTE) 결정
 * @return AuthState (isAuthenticated·user·loading·error·logout)
 */
export function useAuth(app: ShellApp): AuthState {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const token = getAccessToken();

  useEffect(() => {
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    apiFetch("/api/v1/auth/me", { redirectOn401: true, hashRoute: APP_HASH_ROUTE[app] })
      .then((res) => (res.ok ? res.json() : Promise.reject(new Error(`ME_${res.status}`))))
      .then((json) => setUser(json.data))
      .catch((e) => {
        setUser(null);
        setError(e instanceof Error ? e.message : String(e));
      })
      .finally(() => setLoading(false));
  }, [token, app]);

  const logout = useCallback(() => {
    const accessToken = getAccessToken();
    const refreshToken = getRefreshToken();
    if (accessToken) {
      fetch("/api/v1/auth/logout", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ refreshToken }),
      }).catch(() => {});
    }
    clearTokens();
    setUser(null);
    window.location.replace(loginUrl(APP_HREFS[app]));
  }, [app]);

  return { isAuthenticated: !!token && !!user, user, loading, error, logout };
}
