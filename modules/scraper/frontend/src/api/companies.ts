/**
 * 회사 메모 (company-notes) API 클라이언트.
 * ApiResponse 래핑이므로 data 필드를 언랩한다 (blacklistReq 패턴과 동일).
 */
const BASE = "/scraper/company-notes";

function token(): string {
  return localStorage.getItem("accessToken") ?? "";
}

async function companyNoteReq<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token() ? { Authorization: `Bearer ${token()}` } : {}),
      ...options?.headers,
    },
  });
  if (!res.ok) throw new Error(`COMPANY_NOTE_${res.status}`);
  const json = await res.json();
  return (json.data ?? json) as T;
}

export interface NoteCategory {
  id: number;
  name: string;
}

export interface CompanyNoteItem {
  id: number | null;
  companyNameDisplay: string;
  companyNameNormalized: string;
  myStars: number | null;
  bookmarked: boolean;
  blocked: boolean;
  hasNote: boolean;
  averageScore: number | null;
  jobplanetScore: number | null;
  jobkoreaScore: number | null;
  saraminScore: number | null;
  updatedAt: string | null;
  hiddenCount: number | null;
}

export interface CompanyNoteDetail extends Omit<CompanyNoteItem, "id" | "hasNote"> {
  id: number;
  companyNameNormalized: string;
  noteMd: string | null;
  categories: NoteCategory[];
}

export interface CompanyNotePage {
  content: CompanyNoteItem[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type CompanyTab = "all" | "bookmarked" | "blocked";

export interface CompanyNoteInput {
  companyName?: string;
  myStars?: number | null;
  isBookmarked?: boolean | null;
  noteMd?: string | null;
  reasonIds?: number[] | null;
  categoryNames?: string[] | null;
}

export type CompanySort = "updated" | "display" | "stars";
export type SortDir = "asc" | "desc";

export interface CompanyPosting {
  position: string;
  siteName: string;
  company: string;
  crawledAt: string;
  url: string;
}

export interface CompanySuggest {
  companyName: string;
  normalized: string;
  hasNote: boolean;
  noteId: number | null;
  blocked: boolean;
}

export const companyNoteApi = {
  list: (tab: CompanyTab, q?: string, page = 0, size = 50, sort: CompanySort = "updated", dir: SortDir = "desc") => {
    const params = new URLSearchParams({ tab, page: String(page), size: String(size), sort, dir });
    if (q?.trim()) params.set("q", q.trim());
    return companyNoteReq<CompanyNotePage>(`?${params}`);
  },
  get: (id: number) => companyNoteReq<CompanyNoteDetail>(`/${id}`),
  upsert: (input: CompanyNoteInput) =>
    companyNoteReq<CompanyNoteDetail>("", { method: "POST", body: JSON.stringify(input) }),
  update: (id: number, input: CompanyNoteInput) =>
    companyNoteReq<CompanyNoteDetail>(`/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id: number) =>
    companyNoteReq<void>(`/${id}`, { method: "DELETE" }),
  postings: (id: number, size = 10) =>
    companyNoteReq<CompanyPosting[]>(`/${id}/postings?size=${size}`),
  suggest: (q: string) =>
    companyNoteReq<CompanySuggest[]>(`/company-suggest?q=${encodeURIComponent(q)}`),
  exportUrl: (id: number) => `${BASE}/${id}/export`,
};
