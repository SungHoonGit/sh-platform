import { useEffect, useRef, useState, type ReactNode } from "react";
import { Menu, Bell } from "lucide-react";
import { APP_HREFS, APP_LABELS, T, type ShellApp } from "./tokens";

/**
 * 글로벌 헤더. 로고 + 앱 전환 주메뉴 + (선택) 알림 벨 + 아바타 드롭다운.
 * 사이드드로어 열기는 onToggleDrawer로 위임한다.
 * SHELL_VERSION: 1
 */
export type Props = {
  currentApp: ShellApp;
  user: { name: string; email?: string } | null;
  authLoading?: boolean;
  isAdmin?: boolean;
  /** 알림 벨: 배지 + 패널 열림 제어는 앱에서 위임 */
  notifications?: {
    count: number;
    done?: boolean;
    panelOpen: boolean;
    onToggle: () => void;
    panel?: ReactNode;
  };
  onLogout: () => void;
  onToggleDrawer: () => void;
};

export default function GlobalHeader({
  currentApp,
  user,
  authLoading,
  isAdmin,
  notifications,
  onLogout,
  onToggleDrawer,
}: Props) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!menuOpen) return;
    const onClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) setMenuOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [menuOpen]);

  const apps = Object.keys(APP_LABELS) as ShellApp[];

  return (
    <header className={`${T.headerBg} ${T.headerH} flex items-center justify-between shrink-0 relative z-40`}>
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleDrawer}
          aria-label="사이드 메뉴"
          className="p-2 rounded-lg text-slate-300 hover:text-white hover:bg-slate-800 transition-colors"
        >
          <Menu size={18} />
        </button>
        <a href={APP_HREFS.platform} className={T.brandText}>
          SH Platform
        </a>
        <nav className="hidden sm:flex items-center gap-1 ml-2">
          {apps.map((app) => (
            <a
              key={app}
              href={APP_HREFS[app]}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                app === currentApp ? T.navItemActive : T.navItemIdle
              }`}
            >
              {APP_LABELS[app]}
            </a>
          ))}
        </nav>
      </div>

      <div className="flex items-center gap-2">
        {notifications && (
          <div className="relative">
            <button
              onClick={notifications.onToggle}
              aria-label="알림"
              className="p-2 text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
            >
              <Bell size={18} />
              {notifications.count > 0 && (
                <span className="absolute -top-0.5 right-0.5 w-4 h-4 bg-blue-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center animate-pulse pointer-events-none">
                  {notifications.count}
                </span>
              )}
              {notifications.count === 0 && notifications.done && (
                <span className="absolute -top-0.5 right-0.5 w-4 h-4 bg-green-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center pointer-events-none">
                  ✓
                </span>
              )}
            </button>
            {notifications.panelOpen && notifications.panel}
          </div>
        )}

        {authLoading ? (
          <div className="text-sm text-slate-400 px-2">...</div>
        ) : user ? (
          <div className="relative" ref={menuRef}>
            <button
              onClick={() => setMenuOpen((v) => !v)}
              className="flex items-center gap-2 pl-1 pr-2 py-1 rounded-full hover:bg-slate-800 transition-colors"
            >
              <span className="w-7 h-7 rounded-full bg-blue-600 flex items-center justify-center text-xs font-bold text-white">
                {(user.name || "?").slice(0, 1)}
              </span>
              <span className="hidden md:inline text-sm text-slate-200">{user.name}</span>
            </button>
            {menuOpen && (
              <div className="absolute right-0 top-full mt-2 w-56 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                <div className="px-4 py-3 border-b border-slate-100">
                  <p className="text-sm font-semibold text-slate-800">{user.name}</p>
                  {user.email && <p className="text-xs text-slate-500 truncate">{user.email}</p>}
                </div>
                <div className="py-1">
                  <a href="/platform/account" className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50">
                    계정 설정
                  </a>
                  {isAdmin && (
                    <a href="/platform/admin" className="block px-4 py-2 text-sm text-slate-700 hover:bg-slate-50">
                      관리자
                    </a>
                  )}
                </div>
                <div className="border-t border-slate-100 py-1">
                  <button
                    onClick={() => {
                      setMenuOpen(false);
                      onLogout();
                    }}
                    className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                  >
                    로그아웃
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : (
          <a
            href={`/?redirect=${encodeURIComponent(window.location.pathname)}`}
            className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-sm font-medium rounded-lg transition-colors"
          >
            로그인
          </a>
        )}
      </div>
    </header>
  );
}
