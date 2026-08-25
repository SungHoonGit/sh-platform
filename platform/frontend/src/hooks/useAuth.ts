import { useCallback, useEffect, useState } from "react";

export interface UserProfile {
  id: number;
  email: string;
  name: string;
  role: string;
}

interface AuthState {
  user: UserProfile | null;
  loading: boolean;
  logout: () => void;
}

/**
 * 플랫폼용 인증 훅. /api/v1/auth/me 로 프로필 조회.
 * scraper 앱의 useAuth와 동일 패턴 (복제-동기화).
 */
export function useAuth(): AuthState {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  const token = localStorage.getItem("accessToken");

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }
    fetch("/api/v1/auth/me", { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => (res.ok ? res.json() : Promise.reject(res.status)))
      .then((json) => setUser(json.data))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, [token]);

  const logout = useCallback(() => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    window.location.replace("/");
  }, []);

  return { user, loading, logout };
}
