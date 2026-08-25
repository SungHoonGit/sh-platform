/**
 * 대시보드 데이터 소스. 기존 모듈 API를 조합한다 (신규 백엔드 없음).
 */
const token = () => localStorage.getItem("accessToken") ?? "";

async function getRaw<T>(url: string): Promise<T> {
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token()}` } });
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  return res.json();
}

async function getResume<T>(path: string): Promise<T> {
  const json = await getRaw<{ code: string; message: string; data: T }>(`/resume/api/v1${path}`);
  return json.data;
}

// ── 스크래퍼 (raw 응답) ──────────────────────────────────────────

interface CrawlerDto {
  id: number;
  name: string;
  schedule: string;
}

export interface CrawlStats {
  crawlers: number;
  activeSchedules: number;
  totalPostings: number;
  todayPostings: number;
  lastCrawledAt: string | null;
  details: CrawlerDetail[];
}

export interface CrawlerDetail {
  name: string;
  schedule: string;
  total: number;
  todayCount: number;
}

export async function fetchCrawlStats(): Promise<CrawlStats> {
  const crawlers = await getRaw<CrawlerDto[]>("/scraper/docs/crawlers");
  const withSchedule = crawlers.filter((c) => c.schedule && c.schedule.trim().length > 0);
  const stats = await Promise.all(
    crawlers.map((c) =>
      getRaw<{ total: number; todayCount: number; lastCrawledAt: string | null }>(
        `/scraper/job-postings/stats?configId=${c.id}`
      ).catch(() => ({ total: 0, todayCount: 0, lastCrawledAt: null }))
    )
  );
  const latest = stats
    .map((s) => s.lastCrawledAt)
    .filter((v): v is string => !!v)
    .sort()
    .at(-1) ?? null;
  return {
    crawlers: crawlers.length,
    activeSchedules: withSchedule.length,
    totalPostings: stats.reduce((a, s) => a + s.total, 0),
    todayPostings: stats.reduce((a, s) => a + s.todayCount, 0),
    lastCrawledAt: latest,
    details: crawlers.map((c, i) => ({
      name: c.name,
      schedule: c.schedule,
      total: stats[i].total,
      todayCount: stats[i].todayCount,
    })),
  };
}

export interface ScrapItemDto {
  id: number;
  postingId: number;
  company: string;
  position: string;
  siteName: string | null;
  scrappedAt: string;
}

export async function fetchMyScraps(): Promise<ScrapItemDto[]> {
  const json = await getRaw<{ scraps: ScrapItemDto[]; total: number }>("/scraper/job-scrap");
  return json.scraps ?? [];
}

// ── 이력서 (ApiResponse 래퍼) ───────────────────────────────────

export interface ResumeDocDto {
  id: number;
  title: string;
  templateCode?: string;
  updatedAt?: string;
}

export const fetchMyResumes = () => getResume<ResumeDocDto[]>("/documents");

export interface ApplicationDto {
  id: number;
  companyName: string;
  postingTitle: string;
  status: string;
  appliedAt: string | null;
}

export const fetchMyApplications = () => getResume<ApplicationDto[]>("/applications");
