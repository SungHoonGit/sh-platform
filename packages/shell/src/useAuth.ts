import { useCallback } from "react";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  loginUrl,
} from "@sh-platform/core";
import { APP_HREFS, type ShellApp } from "./config";
import { invalidateProfileCache, useProfile, type UserProfile } from "./useProfile";

export type { UserProfile };

export interface AuthState {
  isAuthenticated: boolean;
  user: UserProfile | null;
  loading: boolean;
  error: string | null;
  logout: () => void;
}

/**
 * (명령형) 공통 인증 훅 — 인증 상태와 로그아웃 (설계 037).
 * /me 조회는 useProfile에 위임하고 전역 캐시로 공유한다 (3b에서는 useAuth에 결합됨).
 * 3개 앱의 사본 useAuth를 대체한다 (설계 035). 앱별 설정은 app 파라미터로 전달한다.
 *
 * @param app 현재 앱 — logout 이동 경로(APP_HREFS)와 401 redirect hashRoute 결정
 * @return AuthState (isAuthenticated·user·loading·error·logout)
 */
export function useAuth(app: ShellApp): AuthState {
  const token = getAccessToken();
  const { user, loading, error } = useProfile(app);

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
    invalidateProfileCache();
    window.location.replace(loginUrl(APP_HREFS[app]));
  }, [app]);

  return { isAuthenticated: !!token && !!user, user, loading, error, logout };
}
