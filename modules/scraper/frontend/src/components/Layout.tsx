import { Outlet, useLocation, Link } from "react-router-dom";
import CommonHeader from "./CommonHeader";
import CrawlProgressToast from "./CrawlProgressToast";
import { useCrawlProgress } from "../contexts/CrawlProgressContext";

export default function Layout() {
  const location = useLocation();
  const { progress, dismiss } = useCrawlProgress();
  const isActive = (path: string) =>
    location.pathname === path || (path === "/" && location.pathname === "/search");

  return (
    <div className="h-screen flex flex-col">
      <CommonHeader />
      {progress && <CrawlProgressToast progress={progress} onDismiss={dismiss} />}
      <div className="bg-slate-800 border-b border-slate-700 px-5 flex items-center h-10 gap-4 shrink-0">
        <Link
          to="/"
          className={`text-sm font-medium ${isActive("/") ? "text-blue-400" : "text-slate-300 hover:text-white"}`}
        >
          🔍 통합검색
        </Link>
        <Link
          to="/schedule"
          className={`text-sm font-medium ${isActive("/schedule") ? "text-blue-400" : "text-slate-300 hover:text-white"}`}
        >
          📅 스케줄등록
        </Link>
        <Link
          to="/viewer"
          className={`text-sm font-medium ${isActive("/viewer") ? "text-blue-400" : "text-slate-300 hover:text-white"}`}
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
