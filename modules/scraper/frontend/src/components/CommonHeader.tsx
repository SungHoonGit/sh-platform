import { useAuth } from "../hooks/useAuth";
import { LogOut, User } from "lucide-react";

export default function CommonHeader() {
  const { user, isAuthenticated, loading, logout } = useAuth();

  return (
    <header className="bg-slate-900 text-white h-14 flex items-center justify-between px-5 shrink-0 border-b border-slate-700">
      <a href="/platform" className="text-lg font-bold tracking-tight">
        SH Platform
      </a>

      <div className="flex items-center gap-3">
        {loading ? (
          <div className="text-sm text-slate-400">로딩 중...</div>
        ) : isAuthenticated && user ? (
          <>
            <div className="flex items-center gap-2 text-sm text-slate-300">
              <div className="w-7 h-7 rounded-full bg-blue-600 flex items-center justify-center">
                <User size={14} />
              </div>
              <span>{user.name}</span>
            </div>
            <button
              onClick={logout}
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
            >
              <LogOut size={14} />
              로그아웃
            </button>
          </>
        ) : (
          <a
            href="/"
            className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-sm font-medium rounded-lg transition-colors"
          >
            로그인
          </a>
        )}
      </div>
    </header>
  );
}
