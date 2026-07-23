import { Outlet, useLocation, Link } from "react-router-dom";
import CommonHeader from "./CommonHeader";

export default function Layout() {
  const location = useLocation();
  const isActive = (path: string) =>
    location.pathname === path || (path === "/" && location.pathname === "/search");

  return (
    <div className="h-screen flex flex-col bg-slate-100">
      <CommonHeader />
      <div className="bg-slate-800 border-b border-slate-700 px-5 flex items-center h-10 gap-4 shrink-0">
        <Link
          to="/"
          className={`text-sm font-medium ${isActive("/") ? "text-white" : "text-slate-400 hover:text-white"}`}
        >
          🔍 통합검색
        </Link>
        <Link
          to="/schedule"
          className={`text-sm font-medium ${isActive("/schedule") ? "text-white" : "text-slate-400 hover:text-white"}`}
        >
          📅 스케줄등록
        </Link>
        <Link
          to="/viewer"
          className={`text-sm font-medium ${isActive("/viewer") ? "text-white" : "text-slate-400 hover:text-white"}`}
        >
          📄 뷰어
        </Link>
      </div>
      <main className="flex-1 overflow-hidden">
        <Outlet />
      </main>
    </div>
  );
}
