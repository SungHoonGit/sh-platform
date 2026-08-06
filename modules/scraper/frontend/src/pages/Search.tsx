import { useState, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import * as XLSX from "xlsx";
import { realTimeSearch, type SearchRequest, type SearchResponse } from "../api/scraper";
import {
  REGIONS,
  DEFAULT_LOCATIONS,
  CAREER_TOTAL,
  isCareerActive,
  CareerRangeSlider,
  LocationMultiSelect,
} from "../components/SearchFilters";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700 border-blue-200" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700 border-green-200" },
  { id: "wanted", name: "원티드", color: "bg-red-100 text-red-700 border-red-200" },
  { id: "remember", name: "리멤버", color: "bg-purple-100 text-purple-700 border-purple-200" },
];

const PAGE_SIZE = 20;

type SortKey = "site" | "company" | "position" | "career" | "location" | "tech" | "deadline";

const COLUMNS: { key: SortKey; label: string; w: string }[] = [
  { key: "site", label: "사이트", w: "w-[70px]" },
  { key: "position", label: "포지션", w: "w-auto" },
  { key: "company", label: "회사명", w: "w-[140px]" },
  { key: "career", label: "경력", w: "w-[80px]" },
  { key: "location", label: "지역", w: "w-[80px]" },
  { key: "tech", label: "기술", w: "w-[160px]" },
  { key: "deadline", label: "마감", w: "w-[80px]" },
];

export default function Search() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");
  const [careerMin, setCareerMin] = useState(0);
  const [careerMax, setCareerMax] = useState(CAREER_TOTAL);
  const [locations, setLocations] = useState<string[]>(DEFAULT_LOCATIONS);
  const [selectedSites, setSelectedSites] = useState<string[]>(["saramin", "jobkorea", "wanted", "remember"]);
  const [data, setData] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeSite, setActiveSite] = useState<string>("all");
  const [sortBy, setSortBy] = useState<SortKey | null>(null);
  const [sortDir, setSortDir] = useState<"asc" | "desc">("asc");
  const [page, setPage] = useState(1);
  const [searched, setSearched] = useState(false);

  const allJobs = useMemo(() => data?.jobs ?? [], [data]);

  const filteredJobs = useMemo(() => {
    const base = activeSite === "all" ? allJobs : allJobs.filter((j) => j.site === activeSite);
    if (!sortBy) return base;
    return [...base].sort((a, b) => {
      const getVal = (j: Record<string, string>) => {
        if (sortBy === "site") return j.site || "";
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
    if (sortBy === key && sortDir === "desc") {
      setSortBy(null);
    } else if (sortBy === key) {
      setSortDir("desc");
    } else {
      setSortBy(key);
      setSortDir("asc");
    }
    setPage(1);
  };

  const totalPages = Math.max(1, Math.ceil(filteredJobs.length / PAGE_SIZE));
  const pagedJobs = filteredJobs.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const siteEntries = useMemo(() => {
    if (!data?.siteCounts) return [];
    return Object.entries(data.siteCounts).map(([siteId, count]) => {
      const siteDef = SITES.find((s) => s.id === siteId);
      return { siteId, name: siteDef?.name || siteId, count, color: siteDef?.color || "bg-slate-100 text-slate-600" };
    });
  }, [data]);

  const handleSearch = async () => {
    if (!keyword.trim() || selectedSites.length === 0) return;
    setSearched(true);
    setLoading(true);
    setError(null);
    setData(null);
    setActiveSite("all");
    setPage(1);

    const payload: SearchRequest = {
      keyword: keyword.trim(),
      sites: selectedSites,
    };
    if (isCareerActive(careerMin, careerMax)) {
      if (careerMin > 0) payload.careerMin = careerMin;
      if (careerMax < CAREER_TOTAL) payload.careerMax = careerMax;
    }
    if (locations.length > 0 && locations.length < REGIONS.length) {
      payload.locations = locations;
    }

    try {
      const result = await realTimeSearch(payload);
      setData(result);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  };

  const toggleSite = (siteId: string) => {
    setSelectedSites((prev) =>
      prev.includes(siteId) ? prev.filter((s) => s !== siteId) : [...prev, siteId]
    );
  };

  const toggleLocation = (loc: string) => {
    setLocations((prev) =>
      prev.includes(loc) ? prev.filter((l) => l !== loc) : [...prev, loc]
    );
  };

  const goToSchedule = () => {
    navigate("/schedule", {
      state: { keyword, careerMin, careerMax, locations, sites: selectedSites },
    });
  };

  return (
    <div className="flex h-full">
      {/* 왼쪽: 필터 */}
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
          <CareerRangeSlider
            min={careerMin}
            max={careerMax}
            onMinChange={setCareerMin}
            onMaxChange={setCareerMax}
          />
          <div className="mt-1.5 text-[10px] text-slate-400">
            {isCareerActive(careerMin, careerMax) ? (
              <span className="text-blue-600">선택됨: {careerMin > 0 ? `${careerMin}년` : "신입"} ~ {careerMax >= CAREER_TOTAL ? "15년+" : `${careerMax}년`}</span>
            ) : (
              <span>전체 경력</span>
            )}
          </div>
        </div>

        <div className="mb-4">
          <LocationMultiSelect
            selected={locations}
            onToggle={toggleLocation}
            onSelectAll={() => setLocations([...REGIONS])}
            onClear={() => setLocations([])}
          />
          {locations.length === 0 && (
            <div className="mt-1 text-[10px] text-slate-400">전체 지역 검색</div>
          )}
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
                  onChange={() => toggleSite(site.id)}
                  className="w-3.5 h-3.5 rounded text-blue-600" />
                <span className={`px-1.5 py-0.5 rounded text-[10px] font-medium ${site.color}`}>{site.name}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="space-y-2">
          <button
            onClick={handleSearch}
            disabled={!keyword.trim() || selectedSites.length === 0 || loading}
            className="w-full py-2 bg-blue-600 text-white rounded-lg text-xs font-semibold hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {loading ? "검색 중..." : "🔍 검색"}
          </button>
          <button
            onClick={goToSchedule}
            className="w-full py-2 bg-slate-100 text-slate-700 rounded-lg text-xs font-medium hover:bg-slate-200 transition-colors"
          >
            📅 이 조건으로 스케줄 등록
          </button>
        </div>
      </div>

      {/* 오른쪽: 검색 결과 */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {!searched ? (
          <div className="flex-1 flex flex-col items-center justify-center text-slate-400">
            <div className="text-6xl mb-4">🔍</div>
            <div className="text-lg">키워드를 입력하고 검색하세요</div>
            <div className="text-sm mt-2">예: React, Java, Python, Spring</div>
          </div>
        ) : loading ? (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="animate-spin rounded-full h-10 w-10 border-4 border-blue-200 border-t-blue-600 mb-4" />
            <div className="text-slate-500 text-lg mb-2">공고 수집 중...</div>
            <div className="text-sm text-slate-400">{selectedSites.length}개 사이트 실시간 수집 중</div>
          </div>
        ) : error ? (
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="text-6xl mb-4">⚠️</div>
            <div className="text-lg text-red-600 mb-2">검색 중 오류 발생</div>
            <div className="text-sm text-slate-500">{error}</div>
            <button onClick={handleSearch} className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700">다시 시도</button>
          </div>
        ) : (
          <>
            {/* 사이트별 탭 */}
            <div className="px-5 pt-4 pb-3 bg-white border-b border-slate-200 flex items-center gap-2 flex-wrap shrink-0">
              <button onClick={() => { setActiveSite("all"); setPage(1); }}
                className={`px-3 py-1.5 rounded-md text-sm font-medium border transition-colors ${
                  activeSite === "all" ? "bg-slate-800 text-white border-slate-800" : "bg-white text-slate-700 border-slate-200 hover:border-slate-400"
                }`}>
                전체 <span className="ml-1 opacity-70">{allJobs.length}건</span>
              </button>
              {siteEntries.map((sc) => (
                <button key={sc.siteId} onClick={() => { setActiveSite(sc.siteId); setPage(1); }}
                  className={`px-3 py-1.5 rounded-md text-sm font-medium border transition-colors ${
                    activeSite === sc.siteId
                      ? `${sc.color} border-current`
                      : "bg-white text-slate-700 border-slate-200 hover:border-slate-400"
                  }`}>
                  {sc.name} <span className="ml-1 opacity-70">{sc.count}건</span>
                </button>
              ))}
            </div>

            {/* 툴바 */}
            <div className="px-5 py-2 bg-white border-b border-slate-200 flex items-center justify-between text-xs shrink-0">
              <div className="text-slate-500">
                총 <span className="font-semibold text-slate-800">{filteredJobs.length}</span>건
                {keyword && <span className="ml-2">키워드: <span className="font-medium text-slate-700">{keyword}</span></span>}
              </div>
              <div className="flex items-center gap-3">
                <span className="text-slate-400">
                  {data?.searchTime ? `${(data.searchTime / 1000).toFixed(1)}초 소요` : ""}
                </span>
                <button
                  onClick={() => {
                    if (filteredJobs.length === 0) return;

                    const SITE_NAME_MAP: Record<string, string> = {
                      saramin: "사람인",
                      jobkorea: "잡코리아",
                      wanted: "원티드",
                      remember: "리멤버",
                    };

                    const headers = ["사이트", "회사명", "포지션", "경력", "지역", "기술", "마감", "URL"];
                    const toRow = (j: Record<string, string>) => [
                      SITE_NAME_MAP[j.site] || j.site || "",
                      j.company || "",
                      j.position || j.title || "",
                      j.career || "",
                      j.location || "",
                      j.tech || "",
                      j.deadline || "",
                      j.url || "",
                    ];

                    const wb = XLSX.utils.book_new();

                    // 사이트별 시트 분리
                    const bySite = new Map<string, Record<string, string>[]>();
                    for (const job of filteredJobs) {
                      const site = job.site || "기타";
                      if (!bySite.has(site)) bySite.set(site, []);
                      bySite.get(site)!.push(job);
                    }

                    // 전체 시트
                    const allData = [headers, ...filteredJobs.map(toRow)];
                    const allSheet = XLSX.utils.aoa_to_sheet(allData);
                    XLSX.utils.book_append_sheet(wb, allSheet, "전체");

                    // 사이트별 시트
                    for (const [site, jobs] of bySite) {
                      const sheetName = SITE_NAME_MAP[site] || site;
                      const siteData = [headers, ...jobs.map(toRow)];
                      const sheet = XLSX.utils.aoa_to_sheet(siteData);
                      XLSX.utils.book_append_sheet(wb, sheet, sheetName);
                    }

                    const today = new Date().toISOString().slice(0, 10);
                    XLSX.writeFile(wb, `검색결과_${keyword}_${today}.xlsx`);
                  }}
                  disabled={filteredJobs.length === 0}
                  className="px-3 py-1.5 bg-green-600 text-white rounded-lg text-xs font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  📥 Excel
                </button>
              </div>
            </div>

            {/* 실패한 사이트 경고 */}
            {data?.failedSites && data.failedSites.length > 0 && (
              <div className="mx-5 mt-3 p-2 bg-yellow-50 border border-yellow-200 rounded-lg text-xs text-yellow-700 shrink-0">
                일부 사이트에서 오류 발생: {data.failedSites.join(", ")}
              </div>
            )}

            {/* 테이블 */}
            <div className="flex-1 overflow-auto">
              {pagedJobs.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-slate-400">
                  <div className="text-5xl mb-3">📩</div>
                  <div className="text-lg mb-2">검색 결과가 없습니다</div>
                  <div className="text-sm">다른 키워드나 조건으로 다시 검색해 보세요</div>
                </div>
              ) : (
                <table className="w-full text-xs table-fixed">
                  <thead className="bg-slate-50 border-b border-slate-200 sticky top-0 z-10">
                    <tr>
                      <th className="px-2 py-2 text-left font-semibold text-slate-500 w-[40px]">#</th>
                      {COLUMNS.map((col) => (
                        <th key={col.key}
                          onClick={() => handleSort(col.key)}
                          className={`px-2 py-2 text-left font-semibold text-slate-500 cursor-pointer hover:text-blue-600 select-none ${col.w}`}>
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
                      const siteId = job.site || "";
                      const siteDef = SITES.find((s) => s.id === siteId || s.name === siteId);
                      return (
                        <tr key={i}
                          onClick={() => job.url && window.open(job.url, "_blank")}
                          className="hover:bg-blue-50/50 cursor-pointer transition-colors">
                          <td className="px-2 py-1.5 text-slate-400">{no}</td>
                          <td className="px-2 py-1.5">
                            <span className={`px-1 py-0.5 rounded text-[10px] font-medium ${siteDef?.color || "bg-slate-100 text-slate-600"}`}>
                              {siteDef?.name || siteId}
                            </span>
                          </td>
                          <td className="px-2 py-1.5 font-medium text-slate-800 truncate">
                            {job.position || job.title || "-"}
                          </td>
                          <td className="px-2 py-1.5 text-slate-600 truncate">{job.company || "-"}</td>
                          <td className="px-2 py-1.5 text-slate-500 truncate">{job.career || "-"}</td>
                          <td className="px-2 py-1.5 text-slate-500 truncate">{job.location || "-"}</td>
                          <td className="px-2 py-1.5">
                            {job.tech ? (
                              <span className="text-[10px] text-blue-600 bg-blue-50 px-1 py-0.5 rounded truncate block">{job.tech}</span>
                            ) : <span className="text-slate-300">-</span>}
                          </td>
                          <td className="px-2 py-1.5 text-slate-400 truncate">{job.deadline || "-"}</td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              )}
            </div>

            {/* 페이지네이션 */}
            {totalPages > 1 && (
              <div className="px-5 py-2 bg-white border-t border-slate-200 flex items-center justify-center gap-0.5 shrink-0">
                <button onClick={() => setPage(1)} disabled={page === 1}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
                <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
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
                        page === pageNum ? "bg-blue-600 text-white font-medium" : "text-slate-600 hover:bg-slate-100"
                      }`}>{pageNum}</button>
                  );
                })}
                <button onClick={() => setPage((p) => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
                <button onClick={() => setPage(totalPages)} disabled={page === totalPages}
                  className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
                <span className="ml-2 text-[10px] text-slate-400">{page}/{totalPages}</span>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
