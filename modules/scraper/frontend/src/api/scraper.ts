import type { Crawler, JobsResponse } from "../types";

const BASE = "/scraper";

export interface SearchRequest {
  keyword?: string;
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
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request),
  });
  if (!res.ok) throw new Error("실시간 검색 실패");
  const json = await res.json();
  return json.data;
}

export async function fetchCrawlers(): Promise<Crawler[]> {
  const res = await fetch(`${BASE}/docs/crawlers`);
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
  const res = await fetch(`${BASE}/docs/jobs?${params}`);
  if (!res.ok) throw new Error("채용공고 조회 실패");
  return res.json();
}

export async function executeCrawler(
  configId: number
): Promise<{ status: string }> {
  const res = await fetch(`${BASE}/crawl-config/${configId}/execute`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("크롤러 실행 실패");
  return res.json();
}

export async function fetchCrawlLogs(configId: number): Promise<any[]> {
  const res = await fetch(`${BASE}/crawl-logs/config/${configId}/recent`);
  if (!res.ok) return [];
  return res.json();
}
