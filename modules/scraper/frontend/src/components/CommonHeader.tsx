import { useState } from "react";
import { useAuth } from "../hooks/useAuth";
import { useCrawlProgress } from "../contexts/CrawlProgressContext";
import { LogOut, User, Bell, X } from "lucide-react";

export default function CommonHeader() {
  const { user, isAuthenticated, loading, logout } = useAuth();
  const { progressList, dismiss, dismissAll } = useCrawlProgress();
  const [showNoti, setShowNoti] = useState(false);

  const activeCount = progressList.filter((p) => p.phase === "starting" || p.phase === "running" || p.phase === "disconnected").length;
  const completedCount = progressList.filter((p) => p.phase === "complete").length;

  return (
    <header className="bg-slate-900 text-white h-14 flex items-center justify-between px-5 shrink-0 border-b border-slate-700 relative">
      <a href="/platform" className="text-lg font-bold tracking-tight">
        SH Platform
      </a>

      <div className="flex items-center gap-3">
        {/* 알림 버튼 */}
        {progressList.length > 0 && (
          <div className="relative">
            <button
              onClick={() => setShowNoti(!showNoti)}
              className="relative p-2 text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
            >
              <Bell size={18} />
              {activeCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-blue-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center animate-pulse">
                  {activeCount}
                </span>
              )}
              {activeCount === 0 && completedCount > 0 && (
                <span className="absolute -top-0.5 -right-0.5 w-4 h-4 bg-green-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center">
                  {completedCount}
                </span>
              )}
            </button>

            {/* 알림 드롭다운 */}
            {showNoti && (
              <div className="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
                <div className="bg-slate-800 text-white px-4 py-2.5 flex items-center justify-between">
                  <span className="text-sm font-medium">수집 현황</span>
                  <div className="flex items-center gap-2">
                    {progressList.length > 1 && (
                      <button onClick={dismissAll} className="text-[10px] text-slate-400 hover:text-white">
                        모두 닫기
                      </button>
                    )}
                    <button onClick={() => setShowNoti(false)} className="text-slate-400 hover:text-white">
                      <X size={14} />
                    </button>
                  </div>
                </div>
                <div className="max-h-80 overflow-y-auto">
                  {progressList.length === 0 ? (
                    <div className="px-4 py-6 text-center text-sm text-slate-400">
                      진행 중인 수집이 없습니다
                    </div>
                  ) : (
                    <div className="divide-y divide-slate-100">
                      {progressList.map((p) => (
                        <div key={p.id} className="px-4 py-3">
                          <div className="flex items-center justify-between mb-1">
                            <div className="flex items-center gap-2">
                              {p.phase === "running" && (
                                <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse" />
                              )}
                              {p.phase === "complete" && (
                                <div className="w-2 h-2 bg-green-500 rounded-full" />
                              )}
                              {p.phase === "disconnected" && (
                                <div className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse" />
                              )}
                              {p.phase === "error" && (
                                <div className="w-2 h-2 bg-red-500 rounded-full" />
                              )}
                              {p.phase === "starting" && (
                                <div className="w-2 h-2 bg-yellow-500 rounded-full animate-pulse" />
                              )}
                              <span className="text-xs font-medium text-slate-700">{p.configName}</span>
                            </div>
                            <button
                              onClick={() => dismiss(p.id)}
                              className="text-slate-400 hover:text-slate-600"
                            >
                              <X size={12} />
                            </button>
                          </div>
                          <div className="text-[11px] text-slate-500">
                            {p.phase === "starting" && "공고 수집 준비 중..."}
                            {p.phase === "running" && `공고 수집 중 ${p.sites.filter((s) => s.status === "done" || s.status === "error").length}/${p.totalSites}`}
                            {p.phase === "disconnected" && (
                              <span className="text-yellow-600">
                                연결 끊김 (진행 중일 수 있음)
                              </span>
                            )}
                            {p.phase === "complete" && (
                              <span className="text-green-600">
                                완료 - 신규 {p.newJobs}건
                                {p.dupJobs ? ` (중복 ${p.dupJobs}건 제외)` : ""}
                              </span>
                            )}
                            {p.phase === "error" && <span className="text-red-600">수집 오류</span>}
                          </div>
                          {/* 사이트별 진행상황 */}
                          {p.phase === "running" && p.sites.length > 0 && (
                            <div className="mt-2 space-y-0.5">
                              {p.sites.map((site, i) => (
                                <div key={i} className="flex items-center gap-1.5 text-[10px]">
                                  <span className="w-1.5 h-1.5 rounded-full bg-slate-300" />
                                  <span className="text-slate-600">{site.siteName || "..."}</span>
                                  {site.status === "done" && <span className="text-green-500">✓</span>}
                                  {site.status === "error" && <span className="text-red-500">✕</span>}
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>
        )}

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
