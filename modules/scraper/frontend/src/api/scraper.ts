import type { Crawler, JobsResponse } from "../types";

const BASE = "/scraper";

function authHeaders(): HeadersInit {
  const token = localStorage.getItem("accessToken");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export function redirectToLogin() {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  window.location.replace("/?redirect=" + encodeURIComponent("/scraper/"));
}

export interface SearchRequest {
  keyword?: string;
  careerMin?: number;
  careerMax?: number;
  locations?: string[];
  career?: string;
  location?: string;
  sites?: string[];
}

export interface SearchResponse {
  total: number;
  jobs: Record<string, string>[];
  siteCounts: Record<string, number>;
  searchTime: number;
  failedSites: string[];
}

export async function realTimeSearch(search: SearchRequest): Promise<SearchResponse> {
  const json = await request<{ data: SearchResponse }>("/search", {
    method: "POST",
    body: JSON.stringify(search),
  });
  return json.data;
}

export async function fetchCrawlers(): Promise<Crawler[]> {
  return request<Crawler[]>("/docs/crawlers");
}

export async function fetchJobs(
  rootPath: string,
  path: string,
  site: string,
  page: number,
  size: number = 20,
  sort?: string,
  order?: "asc" | "desc"
): Promise<JobsResponse> {
  const params = new URLSearchParams({
    rootPath,
    path,
    page: String(page),
    size: String(size),
  });
  if (site && site !== "all") {
    params.set("site", site);
  }
  if (sort) {
    params.set("sort", sort);
    params.set("order", order ?? "asc");
  }
  return request<JobsResponse>(`/docs/jobs?${params}`);
}

export interface FileNode {
  name: string;
  path: string;
  type: "file" | "directory";
  size?: number;
  childCount?: number;
  modifiedAt?: string;
}

export async function fetchFiles(rootPath: string, dir?: string): Promise<FileNode[]> {
  const params = new URLSearchParams({ rootPath });
  if (dir) params.set("path", dir);
  return request<FileNode[]>(`/docs/tree?${params}`);
}

export async function executeCrawler(
  configId: number
): Promise<{ status: string }> {
  return request<{ status: string }>(`/crawl-config/${configId}/execute`, {
    method: "POST",
  });
}

export function connectCrawlProgress(
  configId: number,
  handlers: {
    onStart?: (data: { configId: number; configName: string; totalSites: number }) => void;
    onSiteStart?: (data: { siteName: string; index: number; total: number }) => void;
    onSiteComplete?: (data: { siteName: string; jobCount: number; success: boolean; error?: string }) => void;
    onComplete?: (data: { totalSites: number; successSites: number; totalJobs: number; newJobs: number; dupJobs: number }) => void;
    onError?: (error: Event) => void;
  }
): EventSource {
  const token = localStorage.getItem("accessToken");
  const url = `/scraper/crawl-config/${configId}/progress?token=${token}`;
  const es = new EventSource(url);

  es.addEventListener("crawl-start", ((e: MessageEvent) => {
    handlers.onStart?.(JSON.parse(e.data));
  }) as EventListener);

  es.addEventListener("site-start", ((e: MessageEvent) => {
    handlers.onSiteStart?.(JSON.parse(e.data));
  }) as EventListener);

  es.addEventListener("site-complete", ((e: MessageEvent) => {
    handlers.onSiteComplete?.(JSON.parse(e.data));
  }) as EventListener);

  es.addEventListener("crawl-complete", ((e: MessageEvent) => {
    handlers.onComplete?.(JSON.parse(e.data));
    es.close();
  }) as EventListener);

  es.onerror = (e) => {
    handlers.onError?.(e);
    es.close();
  };

  return es;
}

export async function fetchCrawlLogs(configId: number): Promise<any[]> {
  return request<any[]>(`/crawl-logs/config/${configId}/recent`);
}

export interface SiteDefinitionInfo {
  id: number;
  siteName: string;
  displayName: string;
  baseUrl: string;
  isEnabled: boolean;
}

async function errorMessage(res: Response): Promise<string> {
  try {
    const json = await res.json();
    return json?.message || `요청 실패 (${res.status})`;
  } catch {
    return `요청 실패 (${res.status})`;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers: HeadersInit = {
    ...authHeaders(),
    ...(typeof options.body === "string" ? { "Content-Type": "application/json" } : {}),
    ...options.headers,
  };
  const res = await fetch(`${BASE}${path}`, { ...options, headers });
  if (res.status === 401) {
    redirectToLogin();
    throw new Error("인증이 만료되었습니다. 다시 로그인해 주세요");
  }
  if (!res.ok) throw new Error(await errorMessage(res));
  if (res.status === 204) return undefined as T;
  return res.json();
}

export async function fetchSites(): Promise<SiteDefinitionInfo[]> {
  return request<SiteDefinitionInfo[]>("/sites");
}

export interface CrawlerSaveBody {
  name: string;
  description?: string;
  schedule: string;
  isActive?: boolean;
  retentionDays?: number;
}

export async function saveCrawler(body: CrawlerSaveBody): Promise<Crawler> {
  return request<Crawler>("/crawl-config", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function updateCrawler(id: number, body: CrawlerSaveBody): Promise<Crawler> {
  return request<Crawler>(`/crawl-config/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export async function deleteCrawler(id: number): Promise<void> {
  return request<void>(`/crawl-config/${id}`, {
    method: "DELETE",
  });
}

export async function saveSiteConfig(
  configId: number,
  siteDefinitionId: number,
  body: { paramValues: string; isEnabled: boolean }
): Promise<unknown> {
  return request<unknown>(`/crawl-config/${configId}/site-configs/${siteDefinitionId}`, {
    method: "POST",
    body: JSON.stringify(body),
  });
}
