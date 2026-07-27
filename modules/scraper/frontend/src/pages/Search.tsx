import { useState, useCallback, useMemo } from "react";
import { searchJobsRealtime, type SearchSiteResult } from "../api/scraper";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700 border-blue-200" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700 border-green-200" },
  { id: "wanted", name: "원티드", color: "bg-red-100 text-red-700 border-red-200" },
  { id: "remember", name: "리멤버", color: "bg-purple-100 text-purple-700 border-purple-200" },
];

const CAREERS = ["전체", "경력무관", "1~3년", "3~5년", "5~10년", "10년 이상"];
const LOCATIONS = ["전체", "서울", "경기", "인천", "부산", "대구", "기타"];
const PAGE_SIZE = 20;

type SortKey = "site" | "company" | "position" | "career" | "location" | "tech" | "deadline";

const COLUMNS: { key: SortKey; label: string; width?: string }[] = [
  { key: "site", label: "사이트", width: "w-16" },
  { key: "position", label: "포지션" },
  { key: "company", label: "회사명", width: "w-32" },
  { key: "career", label: "경력", width: "w-20" },
  { key: "location", label: "지역", width: "w-20" },
  { key: "tech", label: "기술", width: "w-36" },
  { key: "deadline", label: "마감", width: "w-20" },
];

export default function Search() {
  const [keyword, setKeyword] = useState("");
  const [career, setCareer] = useState("전체");
  const [location, setLocation] = useState("전체");
  const [selectedSites, setSelectedSites] = useState<string[]>(["saramin", "jobkorea", "wanted", "remember"]);
  const [results, setResults] = useState<SearchSiteResult[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeSite, setActiveSite] = useState<string>("all");
  const [sortBy, setSortBy] = useState<SortKey>("site");
  const [sortDir, setSortDir] = useState<"asc" | "desc">("asc");
  const [page, setPage] = useState(1);

  const allJobs = useMemo(() =>
    results?.flatMap((r) =>
      (r.jobs || []).map((j) => ({ ...j, site: r.site, siteId: r.siteId }))
    ) ?? [],
    [results]
  );

  const filteredJobs = useMemo(() => {
    const base = activeSite === "all" ? allJobs : allJobs.filter((j) => j.siteId === activeSite);
    return [...base].sort((a, b) => {
      const getVal = (j: typeof a) => {
        if (sortBy === "site") return j.site;
        if (sortBy === "company") return j.company || "";
        if (sortBy === "position") return j.position || j.title || "";
        if (sortBy === "career") return j.career || "";
        if (sortBy === "location") return j.location || "";
        if (sortBy === "tech") return j.tech || "";
        return j.deadline || "";
      };
      const cmp = getVal(a).localeCompare(getVal(b), "ko");
      return sortDir === "asc" ? cmp : -cmp;
    });
  }, [allJobs, activeSite, sortBy, sortDir]);

  const handleSort = (key: SortKey) => {
    if (sortBy === key) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(key);
      setSortDir("asc");
    }
    setPage(1);
  };

  const totalPages = Math.max(1, Math.ceil(filteredJobs.length / PAGE_SIZE));
  const pagedJobs = filteredJobs.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const siteCounts = results?.map((r) => ({
    siteId: r.siteId,
    site: r.site,
    count: r.jobs?.length ?? 0,
    error: r.error,
  })) ?? [];

  const handleSearch = useCallback(async () => {
    if (!keyword.trim() || selectedSites.length === 0) return;
    setLoading(true);
    setError(null);
    setResults(null);
    setActiveSite("all");
    setPage(1);
    try {
      const data = await searchJobsRealtime({
        keyword: keyword.trim(),
        career,
        location,
        siteIds: selectedSites,
      });
      setResults(data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }, [keyword, career, location, selectedSites]);

  const handleSiteTab = (siteId: string) => {
    setActiveSite(siteId);
    setPage(1);
  };

  return (
    <div className="flex h-full">
      {/* Left sidebar */}
      <div className="w-64 bg-white border-r border-slate-200 p-4 shrink-0 overflow-auto">
        <h3 className="text-xs font-bold text-slate-800 mb-3 uppercase tracking-wide">검색 조건</h3>

        <div className="mb-4">
          <label className="block text-[11px] font-medium text-slate-600 mb-1">키워드</label>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="React, Java, Spring..."
            className="w-full px-2.5 py-2 border border-slate-300 rounded-lg text-xs focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          />
        </div>

        <div className="mb-4">
          <label className="block text-[11px] font-medium text-slate-600 mb-1.5">경력</label>
          <div className="space-y-1">
            {CAREERS.map((c) => (
              <label key={c} className="flex items-center gap-1.5 cursor-pointer">
                <input type="radio" name="career" value={c} checked={career === c}
                  onChange={(e) => setCareer(e.target.value)} className="w-3.5 h-3.5 text-blue-600" />
                <span className="text-xs text-slate-700">{c}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="mb-4">
          <label className="block text-[11px] font-medium text-slate-600 mb-1.5">지역</label>
          <div className="space-y-1">
            {LOCATIONS.map((l) => (
              <label key={l} className="flex items-center gap-1.5 cursor-pointer">
                <input type="radio" name="location" value={l} checked={location === l}
                  onChange={(e) => setLocation(e.target.value)} className="w-3.5 h-3.5 text-blue-600" />
                <span className="text-xs text-slate-700">{l}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="mb-4">
          <div className="flex items-center justify-between mb-1.5">
            <label className="text-[11px] font-medium text-slate-600">사이트</label>
            <button
              onClick={() => setSelectedSites((prev) => prev.length === SITES.length ? [] : SITES.map((s) => s.id))}
              className="text-[10px] text-blue-600 hover:text-blue-800"
            >
              {selectedSites.length === SITES.length ? "전체해제" : "전체선택"}
            </button>
          </div>
          <div className="space-y-1.5">
            {SITES.map((site) => (
              <label key={site.id}
                className={`flex items-center gap-2 p-2 rounded cursor-pointer border transition-colors ${
                  selectedSites.includes(site.id) ? "border-blue-300 bg-blue-50" : "border-slate-200 hover:border-slate-300"
                }`}>
                <input type="checkbox" checked={selectedSites.includes(site.id)}
                  onChange={() => setSelectedSites((prev) => prev.includes(site.id) ? prev.filter((s) => s !== site.id) : [...prev, site.id])}
                  className="w-3.5 h-3.5 rounded text-blue-600" />
                <span className={`px-1.5 py-0.5 rounded text-[10px] font-medium ${site.color}`}>{site.name}</span>
              </label>
            ))}
          </div>
        </div>

        <button
          onClick={handleSearch}
          disabled={!keyword.trim() || selectedSites.length === 0 || loading}
          className="w-full py-2 bg-blue-600 text-white rounded-lg text-xs font-semibold hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {loading ? "검색 중..." : "검색"}
        </button>
      </div>

      {/* Right content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {results === null && !loading && !error ? (
          <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
            <div className="text-6xl mb-4">&#128269;</div>
            <div className="text-lg">키워드를 입력하고 검색하세요</div>
            <div className="text-sm mt-2">예: React, Java, Python, Spring</div>
          </div>
        ) : loading ? (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-blue-200 border-t-blue-600 mb-4" />
            <div className="text-slate-500 text-lg mb-2">검색 중...</div>
            <div className="text-sm text-slate-400">{selectedSites.length}개 사이트 실시간 수집 중</div>
          </div>
        ) : error ? (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="text-6xl mb-4">&#9888;&#65039;</div>
            <div className="text-lg text-red-600 mb-2">검색 중 오류 발생</div>
            <div className="text-sm text-slate-500">{error}</div>
            <button onClick={handleSearch} className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">다시 시도</button>
          </div>
        ) : (
          <>
            {/* Site tabs */}
            <div className="px-5 pt-4 pb-3 bg-white border-b border-slate-200 flex items-center gap-2 flex-wrap shrink-0">
              <button onClick={() => handleSiteTab("all")}
                className={`px-3 py-1.5 rounded-md text-sm font-medium border transition-colors ${
                  activeSite === "all" ? "bg-slate-800 text-white border-slate-800" : "bg-white text-slate-700 border-slate-200 hover:border-slate-400"
                }`}>
                전체 <span className="ml-1 opacity-70">{allJobs.length}건</span>
              </button>
              {siteCounts.map((sc) => {
                const siteDef = SITES.find((s) => s.id === sc.siteId);
                return (
                  <button key={sc.siteId} onClick={() => handleSiteTab(sc.siteId)}
                    className={`px-3 py-1.5 rounded-md text-sm font-medium border transition-colors ${
                      activeSite === sc.siteId
                        ? `${siteDef?.color || "bg-slate-100 text-slate-700"} border-current`
                        : "bg-white text-slate-700 border-slate-200 hover:border-slate-400"
                    }`}>
                    {sc.site}
                    <span className="ml-1 opacity-70">{sc.count}건</span>
                    {sc.error && <span className="ml-1 text-red-500">&#10007;</span>}
                  </button>
                );
              })}
            </div>

            {/* Toolbar */}
            <div className="px-5 py-2 bg-white border-b border-slate-200 flex items-center text-xs shrink-0">
              <div className="text-slate-500">
                총 <span className="font-semibold text-slate-800">{filteredJobs.length}</span>건
                {keyword && <span className="ml-2">키워드: <span className="font-medium text-slate-700">{keyword}</span></span>}
              </div>
            </div>

            {/* Table */}
            <div className="flex-1 overflow-auto">
              {pagedJobs.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-slate-400">
                  <div className="text-5xl mb-3">&#128233;</div>
                  <div className="text-lg mb-2">검색 결과가 없습니다</div>
                  <div className="text-sm">다른 키워드나 조건으로 다시 검색해 보세요</div>
                </div>
              ) : (
                <table className="w-full text-xs">
                  <thead className="bg-slate-50 border-b border-slate-200 sticky top-0 z-10">
                    <tr>
                      <th className="px-2 py-2 text-left font-semibold text-slate-500 w-10">#</th>
                      {COLUMNS.map((col) => (
                        <th key={col.key}
                          onClick={() => handleSort(col.key)}
                          className={`px-2 py-2 text-left font-semibold text-slate-500 cursor-pointer hover:text-blue-600 select-none ${col.width || ""}`}>
                          <span className="inline-flex items-center gap-1">
                            {col.label}
                            {sortBy === col.key && (
                              <span className="text-blue-600">{sortDir === "asc" ? "▲" : "▼"}</span>
                            )}
                          </span>
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {pagedJobs.map((job, i) => {
                      const no = (page - 1) * PAGE_SIZE + i + 1;
                      const siteDef = SITES.find((s) => s.id === job.siteId);
                      return (
                        <tr key={i}
                          onClick={() => window.open(job.url, "_blank")}
                          className="hover:bg-blue-50/50 cursor-pointer transition-colors">
                          <td className="px-2 py-1.5 text-slate-400">{no}</td>
                          <td className="px-2 py-1.5">
                            <span className={`px-1 py-0.5 rounded text-[10px] font-medium ${siteDef?.color || "bg-slate-100 text-slate-600"}`}>
                              {job.site}
                            </span>
                          </td>
                          <td className="px-2 py-1.5 font-medium text-slate-800 truncate max-w-[280px]">
                            {job.position || job.title}
                          </td>
                          <td className="px-2 py-1.5 text-slate-600 truncate">{job.company}</td>
                          <td className="px-2 py-1.5 text-slate-500 whitespace-nowrap">{job.career || "-"}</td>
                          <td className="px-2 py-1.5 text-slate-500">{job.location || "-"}</td>
                          <td className="px-2 py-1.5">
                            {job.tech ? (
                              <span className="text-[10px] text-blue-600 bg-blue-50 px-1 py-0.5 rounded truncate block max-w-[160px]">{job.tech}</span>
                            ) : <span className="text-slate-300">-</span>}
                          </td>
                          <td className="px-2 py-1.5 text-slate-400 whitespace-nowrap">{job.deadline || "-"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="px-5 py-2 bg-white border-t border-slate-200 flex items-center justify-center gap-0.5 shrink-0">
                <button onClick={() => setPage(1)} disabled={page === 1}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">
                  &laquo;
                </button>
                <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">
                  &lsaquo;
                </button>
                {Array.from({ length: Math.min(7, totalPages) }, (_, i) => {
                  let pageNum: number;
                  if (totalPages <= 7) {
                    pageNum = i + 1;
                  } else if (page <= 4) {
                    pageNum = i + 1;
                  } else if (page >= totalPages - 3) {
                    pageNum = totalPages - 6 + i;
                  } else {
                    pageNum = page - 3 + i;
                  }
                  return (
                    <button key={pageNum} onClick={() => setPage(pageNum)}
                      className={`w-6 h-6 text-xs rounded ${
                        page === pageNum
                          ? "bg-blue-600 text-white font-medium"
                          : "text-slate-600 hover:bg-slate-100"
                      }`}>
                      {pageNum}
                    </button>
                  );
                })}
                <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">
                  &rsaquo;
                </button>
                <button onClick={() => setPage(totalPages)} disabled={page === totalPages}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">
                  &raquo;
                </button>
                <span className="ml-2 text-[10px] text-slate-400">{page}/{totalPages}</span>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
