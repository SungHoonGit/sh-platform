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
  deadline: string | null;
  scrappedAt: string;
}

export async function fetchMyScraps(): Promise<ScrapItemDto[]> {
  const json = await getRaw<{ scraps: ScrapItemDto[]; total: number }>("/scraper/job-scrap");
  return json.scraps ?? [];
}

// ── 블랙리스트 (scraper) ────────────────────────────────────────

export interface BlockStatCategory {
  id: number | null;
  name: string;
  category: string;
  count: number;
}

export interface BlockStats {
  total: number;
  uncategorized: number;
  categories: BlockStatCategory[];
}

export const fetchBlockStats = () =>
  getRaw<{ data: BlockStats }>("/scraper/company-blacklist/stats").then((j) => j.data);

/**
 * 마감일 문자열을 파싱해 남은 일수를 반환한다.
 * 지원 형식: "2026-08-30", "~08/30", "08/30(월)", "08월30일" 등. 상시/채용시까지는 null.
 */
export function daysUntilDeadline(deadline: string | null | undefined): number | null {
  if (!deadline) return null;
  const d = deadline.replace(/[^0-9~/월일\s-]/g, "");
  let m: RegExpMatchArray | null;
  if ((m = d.match(/(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})/))) {
    return diffDays(+m[1], +m[2], +m[3]);
  }
  if ((m = d.match(/(\d{1,2})\s*[~/]\s*(\d{1,2})/))) {
    const now = new Date();
    return diffDays(now.getFullYear(), +m[1], +m[2]);
  }
  if (/상시|채용시|마감없음|수시/.test(deadline)) return null;
  return null;
}

function diffDays(y: number, mo: number, day: number): number | null {
  const target = new Date(y, mo - 1, day);
  if (Number.isNaN(target.getTime())) return null;
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((target.getTime() - today.getTime()) / 86400000);
}

/** 마감 배지용 라벨. 임박(D-3~D-0)만 반환, 그 외 null */
export function deadlineBadge(deadline: string | null | undefined): string | null {
  const n = daysUntilDeadline(deadline);
  if (n == null || n > 3) return null;
  if (n < 0) return "마감";
  return `D-${n}`;
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
