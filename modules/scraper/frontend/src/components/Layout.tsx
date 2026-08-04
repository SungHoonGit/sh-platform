import { Outlet, useLocation, Link } from "react-router-dom";
import CommonHeader from "./CommonHeader";
import CrawlProgressToast from "./CrawlProgressToast";
import { useCrawlProgress } from "../contexts/CrawlProgressContext";

export default function Layout() {
  const location = useLocation();
  const { progressList, dismiss, dismissAll } = useCrawlProgress();
  const isActive = (path: string) =>
    location.pathname === path || (path === "/" && location.pathname === "/search");

  return (
    <div className="h-screen flex flex-col">
      <CommonHeader />
      
      {/* 크롤링 진행 알림 - 오른쪽 상단에 쌓이도록 */}
      {progressList.length > 0 && (
        <div className="fixed top-16 right-4 z-50 flex flex-col gap-2">
          {progressList.map((progress) => (
            <CrawlProgressToast
              key={progress.id}
              progress={progress}
              onDismiss={() => dismiss(progress.id)}
            />
          ))}
          {progressList.length > 1 && (
            <button
              onClick={dismissAll}
              className="self-end text-xs text-slate-400 hover:text-slate-600 px-2 py-1"
            >
              모두 닫기
            </button>
          )}
        </div>
      )}

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
