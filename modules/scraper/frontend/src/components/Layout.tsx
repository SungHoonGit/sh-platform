import { Outlet, useLocation, Link } from "react-router-dom";
import CommonHeader from "./CommonHeader";

export default function Layout() {
  const location = useLocation();
  const isActive = (path: string) =>
    location.pathname === path || (path === "/" && location.pathname === "/search");

  return (
    <div className="h-screen flex flex-col">
      <CommonHeader />
      <div className="bg-white border-b border-slate-200 px-5 flex items-center h-10 gap-4 shrink-0">
        <Link
          to="/"
          className={`text-sm font-medium ${isActive("/") ? "text-blue-600 border-b-2 border-blue-600" : "text-slate-500 hover:text-slate-700"}`}
        >
          🔍 통합검색
        </Link>
        <Link
          to="/schedule"
          className={`text-sm font-medium ${isActive("/schedule") ? "text-blue-600 border-b-2 border-blue-600" : "text-slate-500 hover:text-slate-700"}`}
        >
          📅 스케줄등록
        </Link>
        <Link
          to="/viewer"
          className={`text-sm font-medium ${isActive("/viewer") ? "text-blue-600 border-b-2 border-blue-600" : "text-slate-500 hover:text-slate-700"}`}
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
