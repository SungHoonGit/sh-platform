import { useEffect } from "react";
import type { ReactNode } from "react";
import { useAuth } from "../hooks/useAuth";

export default function AuthGuard({ children }: { children: ReactNode }) {
  const { isAuthenticated, loading } = useAuth();

  useEffect(() => {
    if (!loading && !isAuthenticated) {
      window.location.replace("/?redirect=" + encodeURIComponent("/scraper/"));
    }
  }, [loading, isAuthenticated]);

  if (loading) {
    return (
      <div className="h-screen flex items-center justify-center bg-slate-900">
        <div className="text-slate-400 text-sm">로딩 중...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <>{children}</>;
}
