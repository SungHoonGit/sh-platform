import { Outlet, useLocation } from "react-router-dom";

export default function Layout() {
  const location = useLocation();
  const isActive = (path: string) =>
    location.pathname === path || (path === "/" && location.pathname === "/search");

  return (
    <div className="h-screen flex flex-col">
      <header className="bg-slate-800 text-white px-5 py-3 flex items-center gap-6 shrink-0">
        <span className="text-lg font-semibold">🤖 SH Platform</span>
        <nav className="flex gap-4 text-sm">
          <a
            href="/"
            className={`px-3 py-1 rounded ${isActive("/") ? "bg-slate-600" : "text-slate-300 hover:text-white"}`}
          >
            🔍 통합검색
          </a>
          <a
            href="/schedule"
            className={`px-3 py-1 rounded ${isActive("/schedule") ? "bg-slate-600" : "text-slate-300 hover:text-white"}`}
          >
            📅 스케줄등록
          </a>
          <a
            href="/viewer"
            className={`px-3 py-1 rounded ${isActive("/viewer") ? "bg-slate-600" : "text-slate-300 hover:text-white"}`}
          >
            📄 뷰어
          </a>
        </nav>
      </header>
      <main className="flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  );
}
