import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowUp, ArrowDown } from "lucide-react";
import { logout } from "../api/client";

interface JobPostingSummary {
  id: number;
  siteName: string;
  company: string;
  position: string;
  career: string;
  tech: string;
  location: string;
  deadline: string;
  url: string;
}

interface RecentPostings {
  items: JobPostingSummary[];
  total: number;
  page: number;
  size: number;
}

interface RawScrapItem extends JobPostingSummary {
  scrappedAt: string;
  postingId: number;
}

interface GridRow {
  postingId: number;
  siteName: string;
  company: string;
  position: string;
  career: string;
  tech: string;
  location: string;
  deadline: string;
  url: string;
  savedAt?: string;
}

const SITES = [
  { id: "", name: "전체" },
  { id: "saramin", name: "사람인" },
  { id: "jobkorea", name: "잡코리아" },
];

const SITE_BADGES: Record<string, string> = {
  saramin: "bg-blue-100 text-blue-700",
  jobkorea: "bg-green-100 text-green-700",
};

type SortKey = "position" | "company" | "career" | "location" | "deadline";

async function fetchScraper<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("accessToken");
  const res = await fetch(`/scraper${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${token ?? ""}` },
  });
  if (res.status === 401) {
    logout();
    throw new Error("인증 만료");
  }
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

function deadlineRank(deadline: string | null): number {
  if (!deadline) return Number.MAX_SAFE_INTEGER;
  if (/오늘|금일|즉시/.test(deadline)) return -1;
  if (/상시|채용시까지|채울\s*때까지|마감\s*없음/.test(deadline)) return Number.MAX_SAFE_INTEGER - 1;
  const m = deadline.match(/(\d{2,4})[.\-/]\s*(\d{1,2})[.\-/]\s*(\d{1,2})/);
  if (!m) return Number.MAX_SAFE_INTEGER;
  const y = m[1].length === 2 ? 2000 + Number(m[1]) : Number(m[1]);
  const t = new Date(y, Number(m[2]) - 1, Number(m[3])).getTime();
  return Number.isNaN(t) ? Number.MAX_SAFE_INTEGER : t;
}

function SortIcon({ state }: { state: "asc" | "desc" | null }) {
  if (state === "asc") return <ArrowUp size={11} className="inline ml-0.5 text-slate-700" />;
  if (state === "desc") return <ArrowDown size={11} className="inline ml-0.5 text-slate-700" />;
  return null;
}

export default function PostingsBrowsePage() {
  const [view, setView] = useState<"all" | "scraps">("all");
  const [data, setData] = useState<RecentPostings | null>(null);
  const [scraps, setScraps] = useState<RawScrapItem[]>([]);
  const [keyword, setKeyword] = useState("");
  const [site, setSite] = useState("");
  const [page, setPage] = useState(0);
  const [sortKey, setSortKey] = useState<SortKey | null>(null);
  const [sortDir, setSortDir] = useState<"asc" | "desc" | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [scrappedIds, setScrappedIds] = useState<Set<number>>(new Set());

  const SIZE = 20;

  const loadScraps = useCallback(
    () =>
      fetchScraper<{ scraps: RawScrapItem[] }>("/job-scrap")
        .then((json) => {
          setScraps(json.scraps);
          setScrappedIds(new Set(json.scraps.map((s) => s.postingId)));
        })
        .catch(() => undefined),
    []
  );

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ page: String(page), size: String(SIZE) });
    if (keyword.trim()) params.set("keyword", keyword.trim());
    if (site) params.set("siteName", site);
    fetchScraper<RecentPostings>(`/job-postings/recent?${params}`)
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : "공고를 불러올 수 없습니다"))
      .finally(() => setLoading(false));
  }, [page, keyword, site]);

  useEffect(() => { load(); }, [load]);
  useEffect(() => { void loadScraps(); }, [loadScraps]);

  const toggleScrap = async (postingId: number) => {
    const wasScrapped = scrappedIds.has(postingId);
    const nextIds = new Set(scrappedIds);
    if (wasScrapped) nextIds.delete(postingId);
    else nextIds.add(postingId);
    setScrappedIds(nextIds);

    if (wasScrapped) {
      setScraps((prev) => prev.filter((s) => s.postingId !== postingId));
    } else {
      const posting = data?.items.find((i) => i.id === postingId);
      if (posting) {
        setScraps((prev) => [
          { ...posting, postingId: posting.id, scrappedAt: new Date().toISOString() },
          ...prev,
        ]);
      }
    }

    try {
      if (wasScrapped) {
        await fetchScraper(`/job-scrap/${postingId}`, { method: "DELETE" });
      } else {
        await fetchScraper(`/job-scrap/${postingId}`, { method: "POST" });
      }
      await loadScraps();
    } catch {
      setScrappedIds(new Set(scrappedIds));
      void loadScraps();
    }
  };

  const toggleSort = (key: SortKey) => {
    if (sortKey === key) {
      if (sortDir === "asc") { setSortKey(key); setSortDir("desc"); }
      else if (sortDir === "desc") { setSortKey(null); setSortDir(null); }
      else { setSortKey(key); setSortDir("asc"); }
    } else {
      setSortKey(key);
      setSortDir("asc");
    }
  };

  const rows: GridRow[] = useMemo(() => {
    const list: GridRow[] =
      view === "scraps"
        ? scraps.map((s) => ({
            postingId: s.postingId,
            siteName: s.siteName,
            company: s.company,
            position: s.position,
            career: s.career,
            tech: s.tech,
            location: s.location,
            deadline: s.deadline,
            url: s.url,
            savedAt: s.scrappedAt,
          }))
        : (data?.items ?? []).map((i) => ({ ...i, postingId: i.id }));

    if (!sortKey || !sortDir) {
      if (view === "scraps") return list.sort((a, b) => (a.savedAt! < b.savedAt! ? 1 : -1));
      return list;
    }

    const collator = new Intl.Collator("ko");
    return [...list].sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case "company":  cmp = collator.compare(a.company, b.company); break;
        case "position": cmp = collator.compare(a.position, b.position); break;
        case "career":   cmp = (a.career || "").localeCompare(b.career || "", "ko"); break;
        case "location": cmp = (a.location || "").localeCompare(b.location || "", "ko"); break;
        case "deadline": cmp = deadlineRank(a.deadline) - deadlineRank(b.deadline); break;
      }
      return sortDir === "asc" ? cmp : -cmp;
    });
  }, [view, scraps, data, sortKey, sortDir]);

  const applyNow = (p: GridRow) => {
    sessionStorage.setItem(
      "applicationPrefill",
      JSON.stringify({ companyName: p.company, postingTitle: p.position, postingUrl: p.url, postingId: p.postingId })
    );
    window.location.hash = "#/applications";
  };

  const totalPages = view === "all" && data ? Math.max(1, Math.ceil(data.total / SIZE)) : 1;
  const sortState = (key: SortKey): "asc" | "desc" | null => sortKey === key ? sortDir : null;

  return (
    <div className="max-w-6xl mx-auto px-4 py-6">
      <h1 className="text-lg font-bold text-slate-800 mb-1">공고 탐색</h1>
      <p className="text-xs text-slate-400 mb-4">
        스크래퍼가 수집한 공고와 내가 저장한 공고를 한 곳에서 관리합니다. ★로 저장하고 [지원]으로 바로 기록하세요.
      </p>

      <div className="flex items-center gap-2 mb-3 flex-wrap">
        <div className="flex rounded-lg border border-gray-300 overflow-hidden shrink-0">
          <button
            onClick={() => { setView("all"); setSortKey(null); setSortDir(null); setSite(""); setPage(0); }}
            className={`px-3 py-1.5 text-xs font-medium ${view === "all" ? "bg-slate-900 text-white" : "bg-white text-slate-600 hover:bg-gray-50"}`}
          >
            전체 공고
          </button>
          <button
            onClick={() => { setView("scraps"); setSortKey(null); setSortDir(null); setSite(""); setPage(0); }}
            className={`px-3 py-1.5 text-xs font-medium ${view === "scraps" ? "bg-amber-500 text-white" : "bg-white text-slate-600 hover:bg-gray-50"}`}
          >
            ★ 내 스크랩 ({scraps.length})
          </button>
        </div>

        {SITES.map((s) => (
          <button
            key={s.id}
            onClick={() => { setSite(s.id); setPage(0); }}
            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
              site === s.id
                ? "bg-slate-900 text-white"
                : "bg-white border border-slate-200 text-slate-600 hover:border-slate-400"
            }`}
          >
            {s.name}
          </button>
        ))}

        <span className="text-xs text-slate-400 shrink-0">
          총 {view === "all" ? (data?.total ?? 0).toLocaleString() : scraps.length.toLocaleString()}건
        </span>

        <div className="flex items-center gap-1.5 ml-auto shrink-0">
          <input
            value={keyword}
            onChange={(e) => { setKeyword(e.target.value); setPage(0); }}
            onKeyDown={(e) => e.key === "Enter" && load()}
            placeholder="회사명 / 직무 검색"
            className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm w-48 focus:outline-none focus:border-gray-500"
          />
          <button
            onClick={load}
            className="bg-slate-900 text-white rounded-lg px-3 py-1.5 text-sm font-semibold hover:bg-slate-800"
          >
            검색
          </button>
        </div>
      </div>

      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}

      {loading ? (
        <div className="text-center py-16 text-slate-400 text-sm">불러오는 중...</div>
      ) : rows.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-gray-200">
          {view === "scraps" ? (
            <>
              <p className="text-slate-500">스크랩한 공고가 없습니다.</p>
              <button onClick={() => setView("all")} className="mt-2 text-sm text-blue-600 hover:underline">
                전체 공고에서 ☆ 저장해 보세요 →
              </button>
            </>
          ) : (
            <>
              <p className="text-slate-500">조건에 맞는 공고가 없습니다.</p>
              <a href="/scraper/" target="_blank" rel="noopener noreferrer" className="mt-2 inline-block text-sm text-blue-600 hover:underline">
                스크래퍼에서 공고 수집하러 가기 →
              </a>
            </>
          )}
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl border border-gray-200 overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 border-b border-gray-200 text-left">
                <tr>
                  <th className="px-1 py-2 w-[28px]"></th>
                  <th className="px-1 py-2 font-semibold text-slate-600 w-[48px]">사이트</th>
                  <th className="px-2 py-2 font-semibold text-slate-600 w-[32%]">
                    <button onClick={() => toggleSort("position")} className="inline-flex items-center gap-0.5 hover:text-slate-900 transition-colors">
                      직무 / 기술 <SortIcon state={sortState("position")} />
                    </button>
                  </th>
                  <th className="px-1 py-2 font-semibold text-slate-600 w-[48px]">
                    <button onClick={() => toggleSort("career")} className="inline-flex items-center gap-0.5 hover:text-slate-900 transition-colors">
                      경력 <SortIcon state={sortState("career")} />
                    </button>
                  </th>
                  <th className="px-2 py-2 font-semibold text-slate-600 w-[110px]">
                    <button onClick={() => toggleSort("location")} className="inline-flex items-center gap-0.5 hover:text-slate-900 transition-colors">
                      지역 <SortIcon state={sortState("location")} />
                    </button>
                  </th>
                  <th className="px-2 py-2 font-semibold text-slate-600 w-[68px]">
                    <button onClick={() => toggleSort("deadline")} className="inline-flex items-center gap-0.5 hover:text-slate-900 transition-colors">
                      {view === "scraps" ? "저장일" : "마감"} <SortIcon state={sortState("deadline")} />
                    </button>
                  </th>
                  <th className="px-2 py-2 font-semibold text-slate-600 w-[70px]">
                    <button onClick={() => toggleSort("company")} className="inline-flex items-center gap-0.5 hover:text-slate-900 transition-colors">
                      회사 <SortIcon state={sortState("company")} />
                    </button>
                  </th>
                  <th className="px-1 py-2 w-[48px]"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((p) => (
                  <tr
                    key={p.postingId}
                    onClick={() => p.url && window.open(p.url, "_blank")}
                    className={`hover:bg-blue-50/40 cursor-pointer transition-colors ${scrappedIds.has(p.postingId) ? "bg-amber-50/40" : ""}`}
                  >
                    <td className="px-1 py-2 text-center">
                      <button
                        onClick={(e) => { e.stopPropagation(); void toggleScrap(p.postingId); }}
                        title={scrappedIds.has(p.postingId) ? "스크랩 해제" : "스크랩"}
                        className={`text-base leading-none transition-transform hover:scale-125 ${scrappedIds.has(p.postingId) ? "text-amber-500" : "text-slate-300 hover:text-amber-400"}`}
                      >
                        {scrappedIds.has(p.postingId) ? "★" : "☆"}
                      </button>
                    </td>
                    <td className="px-1 py-2">
                      <span className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-medium ${SITE_BADGES[p.siteName] ?? "bg-slate-100 text-slate-600"}`}>
                        {p.siteName}
                      </span>
                    </td>
                    <td className="px-2 py-2">
                      <div className="font-medium text-slate-800 truncate max-w-[360px]">{p.position}</div>
                      {p.tech && <div className="text-[10px] text-blue-600 mt-0.5 truncate max-w-[360px]">{p.tech}</div>}
                    </td>
                    <td className="px-1 py-2 text-slate-600 text-xs whitespace-nowrap">{p.career || "-"}</td>
                    <td className="px-2 py-2 text-slate-600 text-xs">
                      <div className="line-clamp-2 leading-relaxed">{p.location || "-"}</div>
                    </td>
                    <td className="px-2 py-2 text-slate-500 text-xs whitespace-nowrap">
                      {view === "scraps"
                        ? p.savedAt ? new Date(p.savedAt).toLocaleDateString("ko-KR") : "-"
                        : p.deadline || "-"}
                    </td>
                    <td className="px-2 py-2 text-slate-700 text-xs truncate max-w-[80px]">{p.company}</td>
                    <td className="px-1 py-2 text-right">
                      <button
                        onClick={(e) => { e.stopPropagation(); applyNow(p); }}
                        className="text-[11px] font-semibold text-slate-700 border border-gray-300 rounded px-1.5 py-0.5 hover:bg-slate-900 hover:text-white hover:border-slate-900 transition-colors whitespace-nowrap"
                      >
                        지원
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {view === "all" && totalPages > 1 && (
            <div className="flex justify-center items-center gap-2 mt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 text-xs rounded border border-gray-300 disabled:opacity-30 hover:bg-gray-50"
              >
                이전
              </button>
              <span className="text-xs text-slate-500">{page + 1} / {totalPages}</span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-xs rounded border border-gray-300 disabled:opacity-30 hover:bg-gray-50"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
