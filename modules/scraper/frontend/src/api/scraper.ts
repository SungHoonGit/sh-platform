import type { Crawler, JobsResponse } from "../types";

const BASE = "/scraper";

function authHeaders(): HeadersInit {
  const token = localStorage.getItem("accessToken");
  return token ? { Authorization: `Bearer ${token}` } : {};
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

export async function realTimeSearch(request: SearchRequest): Promise<SearchResponse> {
  const res = await fetch(`${BASE}/search`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(request),
  });
  if (!res.ok) throw new Error("실시간 검색 실패");
  const json = await res.json();
  return json.data;
}

export async function fetchCrawlers(): Promise<Crawler[]> {
  const res = await fetch(`${BASE}/docs/crawlers`, { headers: authHeaders() });
  if (!res.ok) throw new Error("크롤러 목록 조회 실패");
  return res.json();
}

export async function fetchJobs(
  rootPath: string,
  path: string,
  site: string,
  page: number,
  size: number = 20
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
  const res = await fetch(`${BASE}/docs/jobs?${params}`, { headers: authHeaders() });
  if (!res.ok) throw new Error("채용공고 조회 실패");
  return res.json();
}

export async function executeCrawler(
  configId: number
): Promise<{ status: string }> {
  const res = await fetch(`${BASE}/crawl-config/${configId}/execute`, {
    method: "POST",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error("크롤러 실행 실패");
  return res.json();
}

export async function fetchCrawlLogs(configId: number): Promise<any[]> {
  const res = await fetch(`${BASE}/crawl-logs/config/${configId}/recent`, {
    headers: authHeaders(),
  });
  if (!res.ok) return [];
  return res.json();
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

export async function fetchSites(): Promise<SiteDefinitionInfo[]> {
  const res = await fetch(`${BASE}/sites`, { headers: authHeaders() });
  if (!res.ok) throw new Error(await errorMessage(res));
  return res.json();
}

export interface CrawlerSaveBody {
  name: string;
  description?: string;
  schedule: string;
  isActive?: boolean;
  retentionDays?: number;
}

export async function saveCrawler(body: CrawlerSaveBody): Promise<Crawler> {
  const res = await fetch(`${BASE}/crawl-config`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await errorMessage(res));
  return res.json();
}

export async function updateCrawler(id: number, body: CrawlerSaveBody): Promise<Crawler> {
  const res = await fetch(`${BASE}/crawl-config/${id}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await errorMessage(res));
  return res.json();
}

export async function deleteCrawler(id: number): Promise<void> {
  const res = await fetch(`${BASE}/crawl-config/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(await errorMessage(res));
}

export async function saveSiteConfig(
  configId: number,
  siteDefinitionId: number,
  body: { paramValues: string; isEnabled: boolean }
): Promise<unknown> {
  const res = await fetch(`${BASE}/crawl-config/${configId}/site-configs/${siteDefinitionId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...authHeaders() },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(await errorMessage(res));
  return res.json();
}
