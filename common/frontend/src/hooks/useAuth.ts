import { useState, useEffect, useCallback } from "react";
import { fetchProfile, logout as logoutApi, type UserProfile } from "../api/auth";

export interface AuthState {
  isAuthenticated: boolean;
  user: UserProfile | null;
  loading: boolean;
  error: string | null;
  logout: () => void;
  refresh: () => void;
}

export function useAuth(): AuthState {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [retry, setRetry] = useState(0);

  const token = localStorage.getItem("accessToken");

  useEffect(() => {
    if (!token) {
      setUser(null);
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    fetchProfile()
      .then(setUser)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [token, retry]);

  const logout = useCallback(() => {
    logoutApi();
    setUser(null);
    window.location.href = "/";
  }, []);

  const refresh = useCallback(() => setRetry((r) => r + 1), []);

  return {
    isAuthenticated: !!token && !!user,
    user,
    loading,
    error,
    logout,
    refresh,
  };
}
