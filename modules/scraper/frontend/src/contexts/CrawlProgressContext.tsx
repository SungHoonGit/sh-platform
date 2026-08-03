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

interface CrawlProgress {
  configId: number;
  configName: string;
  totalSites: number;
  sites: SiteProgress[];
  phase: "starting" | "running" | "complete" | "error";
  newJobs?: number;
  dupJobs?: number;
}

interface CrawlProgressContextType {
  progress: CrawlProgress | null;
  startProgress: (configId: number, configName: string) => void;
  dismiss: () => void;
}

const CrawlProgressContext = createContext<CrawlProgressContextType | null>(null);

let progressId = 0;

export function CrawlProgressProvider({ children }: { children: ReactNode }) {
  const [progress, setProgress] = useState<CrawlProgress | null>(null);
  const esRef = useRef<EventSource | null>(null);
  const queryClient = useQueryClient();

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
        queryClient.invalidateQueries({ queryKey: ["jobs"] });
        queryClient.invalidateQueries({ queryKey: ["crawlers"] });
        queryClient.invalidateQueries({ queryKey: ["crawlLogs"] });
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
  }, [queryClient]);

  const dismiss = useCallback(() => {
    progressId++;
    esRef.current?.close();
    setProgress(null);
  }, []);

  return (
    <CrawlProgressContext.Provider value={{ progress, startProgress, dismiss }}>
      {children}
    </CrawlProgressContext.Provider>
  );
}

export function useCrawlProgress() {
  const ctx = useContext(CrawlProgressContext);
  if (!ctx) throw new Error("useCrawlProgress must be used within CrawlProgressProvider");
  return ctx;
}
