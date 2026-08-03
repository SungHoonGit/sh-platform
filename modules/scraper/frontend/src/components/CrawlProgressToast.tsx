import { useState, useCallback, useRef } from "react";
import { connectCrawlProgress } from "../api/scraper";

interface SiteProgress {
  siteName: string;
  status: "running" | "done" | "error";
  jobCount?: number;
  error?: string;
}

interface CrawlProgress {
  configId: number;
  configName: string;
  totalSites: number;
  sites: SiteProgress[];
  phase: "starting" | "running" | "complete" | "error";
  newJobs?: number;
  dupJobs?: number;
}

let progressId = 0;

export function useCrawlProgress() {
  const [progress, setProgress] = useState<CrawlProgress | null>(null);
  const esRef = useRef<EventSource | null>(null);

  const startProgress = useCallback((configId: number, configName: string) => {
    progressId++;
    const currentId = progressId;

    setProgress({
      configId,
      configName,
      totalSites: 0,
      sites: [],
      phase: "starting",
    });

    const es = connectCrawlProgress(configId, {
      onStart: (data) => {
        if (currentId !== progressId) return;
        setProgress((prev) => prev ? {
          ...prev,
          totalSites: data.totalSites,
          phase: "running",
          sites: Array.from({ length: data.totalSites }, () => ({
            siteName: "",
            status: "running" as const,
          })),
        } : null);
      },
      onSiteStart: (data) => {
        if (currentId !== progressId) return;
        setProgress((prev) => prev ? {
          ...prev,
          sites: prev.sites.map((s, i) =>
            i === data.index - 1 ? { ...s, siteName: data.siteName, status: "running" as const } : s
          ),
        } : null);
      },
      onSiteComplete: (data) => {
        if (currentId !== progressId) return;
        setProgress((prev) => prev ? {
          ...prev,
          sites: prev.sites.map((s) =>
            s.siteName === data.siteName ? {
              ...s,
              status: data.success ? "done" as const : "error" as const,
              jobCount: data.jobCount,
              error: data.error,
            } : s
          ),
        } : null);
      },
      onComplete: (data) => {
        if (currentId !== progressId) return;
        setProgress((prev) => prev ? {
          ...prev,
          phase: "complete",
          newJobs: data.newJobs,
          dupJobs: data.dupJobs,
        } : null);
        setTimeout(() => {
          if (currentId === progressId) setProgress(null);
        }, 5000);
      },
      onError: () => {
        if (currentId !== progressId) return;
        setProgress((prev) => prev ? { ...prev, phase: "error" } : null);
        setTimeout(() => {
          if (currentId === progressId) setProgress(null);
        }, 3000);
      },
    });

    esRef.current = es;
  }, []);

  const dismiss = useCallback(() => {
    progressId++;
    esRef.current?.close();
    setProgress(null);
  }, []);

  return { progress, startProgress, dismiss };
}

const SITE_ICONS: Record<string, string> = {
  saramin: "🔍",
  jobkorea: "💼",
  wanted: "🚀",
  remember: "📋",
};

export default function CrawlProgressToast({
  progress,
  onDismiss,
}: {
  progress: CrawlProgress;
  onDismiss: () => void;
}) {
  if (!progress) return null;

  const completedSites = progress.sites.filter((s) => s.status === "done" || s.status === "error");
  const progressPercent = progress.totalSites > 0
    ? Math.round((completedSites.length / progress.totalSites) * 100)
    : 0;

  return (
    <div className="fixed top-16 right-4 z-50 w-80 bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden animate-slide-in">
      <div className="bg-slate-800 text-white px-4 py-2.5 flex items-center justify-between">
        <div className="flex items-center gap-2">
          {progress.phase === "running" ? (
            <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
          ) : progress.phase === "complete" ? (
            <span className="text-green-400">✓</span>
          ) : (
            <span className="text-yellow-400">⏳</span>
          )}
          <span className="text-sm font-medium">
            {progress.phase === "starting" && "크롤링 준비 중..."}
            {progress.phase === "running" && `${completedSites.length}/${progress.totalSites} 사이트 완료`}
            {progress.phase === "complete" && "크롤링 완료"}
            {progress.phase === "error" && "크롤링 실패"}
          </span>
        </div>
        <button onClick={onDismiss} className="text-slate-400 hover:text-white text-xs">✕</button>
      </div>

      {progress.phase === "running" && (
        <div className="h-1 bg-slate-100">
          <div
            className="h-full bg-blue-500 transition-all duration-500"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      )}

      <div className="px-4 py-3 max-h-48 overflow-y-auto">
        <div className="text-xs text-slate-500 mb-2">{progress.configName}</div>
        <div className="space-y-1.5">
          {progress.sites.map((site, i) => (
            <div key={i} className="flex items-center gap-2 text-xs">
              <span className="w-4 text-center">
                {site.status === "running" && (
                  <div className="w-3 h-3 border border-blue-400 border-t-transparent rounded-full animate-spin" />
                )}
                {site.status === "done" && <span className="text-green-500">✓</span>}
                {site.status === "error" && <span className="text-red-500">✕</span>}
              </span>
              <span className="text-slate-700">
                {SITE_ICONS[site.siteName] || "🌐"} {site.siteName || "..."}
              </span>
              {site.status === "done" && site.jobCount !== undefined && (
                <span className="ml-auto text-slate-400">{site.jobCount}건</span>
              )}
              {site.status === "error" && (
                <span className="ml-auto text-red-400 truncate" title={site.error}>실패</span>
              )}
            </div>
          ))}
        </div>
      </div>

      {progress.phase === "complete" && (
        <div className="px-4 py-2.5 bg-slate-50 border-t border-slate-100 text-xs text-slate-600">
          신규 <span className="font-semibold text-blue-600">{progress.newJobs}</span>건
          {progress.dupJobs ? (
            <span className="ml-2 text-slate-400">(중복 {progress.dupJobs}건 제외)</span>
          ) : null}
        </div>
      )}
    </div>
  );
}
