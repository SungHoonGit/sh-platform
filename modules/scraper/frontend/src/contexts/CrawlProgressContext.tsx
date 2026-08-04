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
  const esMapRef = useRef<Map<number, EventSource>>(new Map());
  const queryClient = useQueryClient();

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
        queryClient.invalidateQueries({ queryKey: ["jobs"] });
        queryClient.invalidateQueries({ queryKey: ["jobDates"] });
        queryClient.invalidateQueries({ queryKey: ["crawlers"] });
        setTimeout(() => {
          setProgressList((prev) => prev.filter((p) => p.id !== currentId));
        }, 8000);
        esMapRef.current.delete(currentId);
      },
      onError: () => {
        // 네트워크 오류 또는 서버 연결 끊김 시
        // "disconnected" 상태로 설정 (에러보다 관대한 표시)
        setProgressList((prev) =>
          prev.map((p) => {
            if (p.id !== currentId) return p;
            // 이미 완료된 상태라면 무시
            if (p.phase === "complete") return p;
            // 시작 단계라면 아직 진행 중일 수 있음
            if (p.phase === "starting") return p;
            return { ...p, phase: "disconnected" };
          })
        );
        // 10초 후 자동 제거 (완료되지 않은 경우)
        setTimeout(() => {
          setProgressList((prev) => {
            const target = prev.find((p) => p.id === currentId);
            if (target && target.phase !== "complete") {
              return prev.filter((p) => p.id !== currentId);
            }
            return prev;
          });
        }, 10000);
        esMapRef.current.delete(currentId);
      },
    });

    esMapRef.current.set(currentId, es);
  }, [queryClient]);

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
