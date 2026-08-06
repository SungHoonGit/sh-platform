import { createContext, useContext, useState, useCallback, useRef } from "react";
import type { ReactNode } from "react";
import { connectCrawlProgress } from "../api/scraper";
import { useQueryClient } from "@tanstack/react-query";

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
  error?: string;
}

interface CrawlProgressContextType {
  progressList: CrawlProgress[];
  startProgress: (configId: number, configName: string) => void;
  dismiss: (id: number) => void;
  dismissAll: () => void;
}

const CrawlProgressContext = createContext<CrawlProgressContextType | null>(null);

let progressId = 0;

export function CrawlProgressProvider({ children }: { children: ReactNode }) {
  const [progressList, setProgressList] = useState<CrawlProgress[]>([]);
  const progressListRef = useRef<CrawlProgress[]>([]);
  const esMapRef = useRef<Map<number, EventSource>>(new Map());
  const queryClient = useQueryClient();

  // ref를 최신 상태로 동기화
  progressListRef.current = progressList;

  const startProgress = useCallback((configId: number, configName: string) => {
    // 같은 configId의 진행 중인 알림이 있으면 무시
    const existingProgress = progressListRef.current.find(
      (p) => p.configId === configId && (p.phase === "starting" || p.phase === "running")
    );
    if (existingProgress) {
      console.log("[CrawlProgress] skip duplicate, existing id:", existingProgress.id);
      return;
    }

    progressId++;
    const currentId = progressId;
    console.log("[CrawlProgress] startProgress called, new id:", currentId, "configId:", configId, "configName:", configName);

    const newProgress: CrawlProgress = {
      id: currentId,
      configId,
      configName,
      totalSites: 0,
      sites: [],
      phase: "starting",
    };

    setProgressList((prev) => {
      console.log("[CrawlProgress] adding notification, prev length:", prev.length);
      return [...prev, newProgress];
    });

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
        console.log("[CrawlProgress] onComplete for id:", currentId, "newJobs:", data.newJobs);
        setProgressList((prev) =>
          prev.map((p) =>
            p.id === currentId
              ? { ...p, phase: "complete", newJobs: data.newJobs, dupJobs: data.dupJobs }
              : p
          )
        );
        queryClient.invalidateQueries({ queryKey: ["jobs"] });
        queryClient.invalidateQueries({ queryKey: ["jobDates"] });
        queryClient.invalidateQueries({ queryKey: ["crawlLogsGrouped"] });
        queryClient.invalidateQueries({ queryKey: ["crawlers"] });
        // 완료 알림은 30초간 유지 (사용자가 확인할 수 있도록)
        setTimeout(() => {
          console.log("[CrawlProgress] onComplete timeout, removing id:", currentId);
          setProgressList((prev) => prev.filter((p) => p.id !== currentId));
        }, 30000);
        esMapRef.current.delete(currentId);
      },
      onError: () => {
        console.log("[CrawlProgress] onError for id:", currentId);
        setProgressList((prev) =>
          prev.map((p) => {
            if (p.id !== currentId) return p;
            // 이미 완료된 상태라면 무시
            if (p.phase === "complete") return p;
            // 시작 단계에서 에러 발생 시 연결 실패로 표시
            if (p.phase === "starting") {
              return { ...p, phase: "error", error: "서버 연결 실패" };
            }
            return { ...p, phase: "disconnected" };
          })
        );
        // 5초 후 자동 제거
        setTimeout(() => {
          console.log("[CrawlProgress] onError timeout, removing id:", currentId);
          setProgressList((prev) => prev.filter((p) => p.id !== currentId));
        }, 5000);
        esMapRef.current.delete(currentId);
      },
    });

    esMapRef.current.set(currentId, es);
  }, [queryClient]);

  const dismiss = useCallback((id: number) => {
    console.log("[CrawlProgress] dismiss called with id:", id, "current list length:", progressListRef.current.length, "items:", progressListRef.current.map(p => p.id));
    const es = esMapRef.current.get(id);
    if (es) {
      es.close();
      esMapRef.current.delete(id);
    }
    setProgressList((prev) => {
      const filtered = prev.filter((p) => p.id !== id);
      console.log("[CrawlProgress] after dismiss, remaining:", filtered.length);
      return filtered;
    });
  }, []);

  const dismissAll = useCallback(() => {
    esMapRef.current.forEach((es) => es.close());
    esMapRef.current.clear();
    setProgressList([]);
  }, []);

  return (
    <CrawlProgressContext.Provider value={{ progressList, startProgress, dismiss, dismissAll }}>
      {children}
    </CrawlProgressContext.Provider>
  );
}

export function useCrawlProgress() {
  const ctx = useContext(CrawlProgressContext);
  if (!ctx) throw new Error("useCrawlProgress must be used within CrawlProgressProvider");
  return ctx;
}
