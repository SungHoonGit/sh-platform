import { useState, useCallback, useRef } from "react";
import { connectCrawlProgress } from "../api/scraper";

interface SiteProgress {
  siteName: string;
  status: "running" | "done" | "error";
  jobCount?: number;
  error?: string;
}

export interface CrawlProgress {
  id: number;
  configId: number;
  configName: string;
  totalSites: number;
  sites: SiteProgress[];
  phase: "starting" | "running" | "complete" | "error" | "disconnected";
  newJobs?: number;
  dupJobs?: number;
}

let progressId = 0;

export function useCrawlProgress() {
  const [progressList, setProgressList] = useState<CrawlProgress[]>([]);
  const esMapRef = useRef<Map<number, EventSource>>(new Map());

  const startProgress = useCallback((configId: number, configName: string) => {
    progressId++;
    const currentId = progressId;

    const newProgress: CrawlProgress = {
      id: currentId,
      configId,
      configName,
      totalSites: 0,
      sites: [],
      phase: "starting",
    };

    setProgressList((prev) => [...prev, newProgress]);

    const es = connectCrawlProgress(configId, {
      onStart: (data) => {
        setProgressList((prev) =>
          prev.map((p) =>
            p.id === currentId
              ? {
                  ...p,
                  totalSites: data.totalSites,
                  phase: "running",
                  sites: Array.from({ length: data.totalSites }, () => ({
                    siteName: "",
                    status: "running" as const,
                  })),
                }
              : p
          )
        );
      },
      onSiteStart: (data) => {
        setProgressList((prev) =>
          prev.map((p) =>
            p.id === currentId
              ? {
                  ...p,
                  sites: p.sites.map((s, i) =>
                    i === data.index - 1 ? { ...s, siteName: data.siteName, status: "running" as const } : s
                  ),
                }
              : p
          )
        );
      },
      onSiteComplete: (data) => {
        setProgressList((prev) =>
          prev.map((p) =>
            p.id === currentId
              ? {
                  ...p,
                  sites: p.sites.map((s) =>
                    s.siteName === data.siteName
                      ? { ...s, status: data.success ? "done" as const : "error" as const, jobCount: data.jobCount, error: data.error }
                      : s
                  ),
                }
              : p
          )
        );
      },
      onComplete: (data) => {
        setProgressList((prev) =>
          prev.map((p) =>
            p.id === currentId
              ? { ...p, phase: "complete", newJobs: data.newJobs, dupJobs: data.dupJobs }
              : p
          )
        );
        setTimeout(() => {
          setProgressList((prev) => prev.filter((p) => p.id !== currentId));
        }, 8000);
        esMapRef.current.delete(currentId);
      },
      onError: () => {
        setProgressList((prev) =>
          prev.map((p) => (p.id === currentId ? { ...p, phase: "error" } : p))
        );
        setTimeout(() => {
          setProgressList((prev) => prev.filter((p) => p.id !== currentId));
        }, 5000);
        esMapRef.current.delete(currentId);
      },
    });

    esMapRef.current.set(currentId, es);
  }, []);

  const dismiss = useCallback((id: number) => {
    const es = esMapRef.current.get(id);
    es?.close();
    esMapRef.current.delete(id);
    setProgressList((prev) => prev.filter((p) => p.id !== id));
  }, []);

  const dismissAll = useCallback(() => {
    esMapRef.current.forEach((es) => es.close());
    esMapRef.current.clear();
    setProgressList([]);
  }, []);

  return { progressList, startProgress, dismiss, dismissAll };
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
  const progressPercent =
    progress.totalSites > 0 ? Math.round((completedSites.length / progress.totalSites) * 100) : 0;

  return (
    <div className="w-80 bg-white rounded-xl shadow-2xl border border-slate-200 overflow-hidden">
      <div className="bg-slate-800 text-white px-4 py-2.5 flex items-center justify-between">
        <div className="flex items-center gap-2">
          {progress.phase === "running" ? (
            <div className="w-4 h-4 border-2 border-blue-400 border-t-transparent rounded-full animate-spin" />
          ) : progress.phase === "complete" ? (
            <span className="text-green-400 text-lg">✓</span>
          ) : progress.phase === "error" ? (
            <span className="text-red-400 text-lg">✕</span>
          ) : (
            <span className="text-yellow-400 text-lg">⏳</span>
          )}
          <span className="text-sm font-medium">
            {progress.phase === "starting" && "공고 수집 준비 중..."}
            {progress.phase === "running" && `공고 수집 중 ${completedSites.length}/${progress.totalSites}`}
            {progress.phase === "complete" && "공고 수집 완료"}
            {progress.phase === "disconnected" && "연결 끊김 (수집 계속 진행 중)"}
            {progress.phase === "error" && (progress.error || "공고 수집 실패")}
          </span>
        </div>
        <button onClick={onDismiss} className="text-slate-400 hover:text-white text-xs">
          ✕
        </button>
      </div>

      {progress.phase === "running" && (
        <div className="h-1.5 bg-slate-100">
          <div
            className="h-full bg-blue-500 transition-all duration-500"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
      )}

      <div className="px-4 py-3 max-h-48 overflow-y-auto">
        <div className="text-xs text-slate-500 mb-2 font-medium">{progress.configName}</div>
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
                <span className="ml-auto text-red-400 truncate" title={site.error}>
                  실패
                </span>
              )}
            </div>
          ))}
        </div>
      </div>

      {progress.phase === "complete" && (
        <div className="px-4 py-2.5 bg-green-50 border-t border-green-100 text-xs text-green-700">
          신규 <span className="font-semibold">{progress.newJobs}</span>건 수집
          {progress.dupJobs ? (
            <span className="ml-2 text-green-500">(중복 {progress.dupJobs}건 제외)</span>
          ) : null}
        </div>
      )}

      {progress.phase === "disconnected" && (
        <div className="px-4 py-2.5 bg-yellow-50 border-t border-yellow-100 text-xs text-yellow-700">
          연결이 끊어졌습니다. 서버에서 수집이 계속 진행 중일 수 있습니다.
        </div>
      )}

      {progress.phase === "error" && (
        <div className="px-4 py-2.5 bg-red-50 border-t border-red-100 text-xs text-red-700">
          수집 중 오류가 발생했습니다
        </div>
      )}
    </div>
  );
}
