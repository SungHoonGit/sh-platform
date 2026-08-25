import { useState } from "react";
import { X } from "lucide-react";
import { useCrawlProgress } from "../contexts/CrawlProgressContext";
import type { Props as GlobalHeaderProps } from "../shell/GlobalHeader";

/**
 * 크롤 수집 진행 알림(SSE)을 셸의 notifications prop 형태로 변환한다.
 * SHELL_VERSION: 1
 */
export function useCrawlNotifications(): NonNullable<GlobalHeaderProps["notifications"]> {
  const { progressList, dismiss, dismissAll } = useCrawlProgress();
  const [open, setOpen] = useState(false);

  const activeCount = progressList.filter(
    (p) => p.phase === "starting" || p.phase === "running" || p.phase === "disconnected"
  ).length;
  const completedCount = progressList.filter((p) => p.phase === "complete").length;

  return {
    count: activeCount,
    done: completedCount > 0,
    panelOpen: open,
    onToggle: () => setOpen((v) => !v),
    panel: (
      <div className="absolute right-0 top-full mt-2 w-80 bg-white rounded-xl shadow-2xl border border-slate-200 z-50 overflow-hidden">
        <div className="bg-slate-800 text-white px-4 py-2.5 flex items-center justify-between">
          <span className="text-sm font-medium">수집 현황</span>
          <div className="flex items-center gap-2">
            {progressList.length > 1 && (
              <button onClick={dismissAll} className="text-[10px] text-slate-400 hover:text-white">
                모두 닫기
              </button>
            )}
            <button onClick={() => setOpen(false)} className="text-slate-400 hover:text-white">
              <X size={14} />
            </button>
          </div>
        </div>
        <div className="max-h-80 overflow-y-auto">
          {progressList.length === 0 ? (
            <div className="px-4 py-6 text-center text-sm text-slate-400">진행 중인 수집이 없습니다</div>
          ) : (
            <div className="divide-y divide-slate-100">
              {progressList.map((p) => (
                <div key={p.id} className="px-4 py-3">
                  <div className="flex items-center justify-between mb-1">
                    <div className="flex items-center gap-2">
                      {(p.phase === "running" || p.phase === "starting" || p.phase === "disconnected") && (
                        <div
                          className={`w-2 h-2 rounded-full animate-pulse ${
                            p.phase === "running" ? "bg-blue-500" : "bg-yellow-500"
                          }`}
                        />
                      )}
                      {p.phase === "complete" && <div className="w-2 h-2 bg-green-500 rounded-full" />}
                      {p.phase === "error" && <div className="w-2 h-2 bg-red-500 rounded-full" />}
                      <span className="text-xs font-medium text-slate-700">{p.configName}</span>
                    </div>
                    <button onClick={() => dismiss(p.id)} className="text-slate-400 hover:text-slate-600">
                      <X size={12} />
                    </button>
                  </div>
                  <div className="text-[11px] text-slate-500">
                    {p.phase === "starting" && "공고 수집 준비 중..."}
                    {p.phase === "running" &&
                      `공고 수집 중 ${p.sites.filter((s) => s.status === "done" || s.status === "error").length}/${p.totalSites}`}
                    {p.phase === "disconnected" && (
                      <span className="text-yellow-600">연결 끊김 (진행 중일 수 있음)</span>
                    )}
                    {p.phase === "complete" && (
                      <span className="text-green-600">
                        완료 - 신규 {p.newJobs}건{p.dupJobs ? ` (중복 ${p.dupJobs}건 제외)` : ""}
                      </span>
                    )}
                    {p.phase === "error" && <span className="text-red-600">수집 오류</span>}
                  </div>
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
    ),
  };
}
