import { useState, useCallback, useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { fetchCrawlers } from "../api/scraper";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700" },
  { id: "wanted", name: "원티드", color: "bg-red-100 text-red-700" },
  { id: "remember", name: "리멤버", color: "bg-purple-100 text-purple-700" },
];

const CAREERS = ["전체", "경력무관", "1~3년", "3~5년", "5~10년", "10년 이상"];
const LOCATIONS = ["전체", "서울", "경기", "인천", "부산", "대구", "기타"];
const PAGE_SIZE = 20;
const LOOKBACK_DAYS = 7;

function getDateStr(offset: number): string {
  const d = new Date();
  d.setDate(d.getDate() - offset);
  return d.toISOString().split("T")[0];
}

const siteNames: Record<string, string> = {
  saramin: "사람인", jobkorea: "잡코리아", wanted: "원티드", remember: "리멤버",
};

async function fetchJobsForConfig(
  localPath: string,
  siteName: string
): Promise<any[]> {
  // Try dates from today backwards
  for (let offset = 0; offset < LOOKBACK_DAYS; offset++) {
    const dateStr = getDateStr(offset);
    const params = new URLSearchParams({
      rootPath: localPath,
      path: `${dateStr}.md`,
      site: siteName,
      page: "0",
      size: "500",
    });
    try {
      const res = await fetch(`/scraper/docs/jobs?${params}`);
      if (!res.ok) continue;
      const json = await res.json();
      return json.jobs || [];
    } catch {
      continue;
    }
  }
  return [];
}

export default function Search() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");
  const [career, setCareer] = useState("전체");
  const [location, setLocation] = useState("전체");
  const [selectedSites, setSelectedSites] = useState<string[]>(["saramin", "jobkorea", "wanted", "remember"]);
  const [searchTrigger, setSearchTrigger] = useState(0);
  const [page, setPage] = useState(0);

  const { data: crawlers, error: crawlersError } = useQuery({
    queryKey: ["crawlers"],
    queryFn: fetchCrawlers,
  });

  const handleSearch = useCallback(() => {
    if (!keyword.trim()) return;
    setSearchTrigger((t) => t + 1);
    setPage(0);
  }, [keyword]);

  const configPaths = useMemo(() => {
    if (!crawlers || crawlers.length === 0) return [];
    return crawlers.map((c: any) => c.localPath).filter(Boolean);
  }, [crawlers]);

  const { data, isLoading, error } = useQuery({
    queryKey: ["search-v2", searchTrigger, ...configPaths, ...selectedSites],
    queryFn: async () => {
      if (configPaths.length === 0) throw new Error("크롤러 설정이 없습니다");
      if (!keyword.trim()) throw new Error("키워드를 입력하세요");
      if (selectedSites.length === 0) throw new Error("검색할 사이트를 선택하세요");

      const results = await Promise.all(
        configPaths.flatMap((localPath: string) =>
          selectedSites.map((siteId) => {
            const siteName = siteNames[siteId];
            if (!siteName) return Promise.resolve([]);
            return fetchJobsForConfig(localPath, siteName);
          })
        )
      );

      const allJobs = results
        .flat()
        .filter((j: any) => {
          const matchKeyword = !keyword ||
            [j.company, j.position, j.tech, j.location]
              .filter(Boolean)
              .some((v: string) => v.toLowerCase().includes(keyword.toLowerCase()));
          const matchCareer = career === "전체" || !j.career || j.career.includes(career);
          const matchLocation = location === "전체" || !j.location || j.location.includes(location);
          return matchKeyword && matchCareer && matchLocation;
        });

      return allJobs;
    },
    enabled: searchTrigger > 0,
  });

  const totalPages = data ? Math.ceil(data.length / PAGE_SIZE) : 0;
  const pagedData = data ? data.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE) : [];
  const totalCount = data ? data.length : 0;

  const goToSchedule = () => {
    navigate("/schedule", { state: { keyword, career, location, sites: selectedSites } });
  };

  return (
    <div className="flex h-full">
      <div className="w-72 bg-white border-r border-slate-200 p-5 shrink-0 overflow-auto">
        <h3 className="text-sm font-bold text-slate-800 mb-4 uppercase tracking-wide">검색 조건</h3>

        <div className="mb-5">
          <label className="block text-xs font-medium text-slate-600 mb-1.5">키워드</label>
          <input
            type="text"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="React, Java, Spring..."
            className="w-full px-3 py-2.5 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            onKeyDown={(e) => e.key === "Enter" && handleSearch()}
          />
        </div>

        <div className="mb-5">
          <label className="block text-xs font-medium text-slate-600 mb-2">경력</label>
          <div className="space-y-1.5">
            {CAREERS.map((c) => (
              <label key={c} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="career"
                  value={c}
                  checked={career === c}
                  onChange={(e) => setCareer(e.target.value)}
                  className="w-4 h-4 text-blue-600"
                />
                <span className="text-sm text-slate-700">{c}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="mb-5">
          <label className="block text-xs font-medium text-slate-600 mb-2">지역</label>
          <div className="space-y-1.5">
            {LOCATIONS.map((l) => (
              <label key={l} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="location"
                  value={l}
                  checked={location === l}
                  onChange={(e) => setLocation(e.target.value)}
                  className="w-4 h-4 text-blue-600"
                />
                <span className="text-sm text-slate-700">{l}</span>
              </label>
            ))}
          </div>
        </div>

        <div className="mb-5">
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-medium text-slate-600">사이트</label>
            <button
              onClick={() =>
                setSelectedSites((prev) =>
                  prev.length === SITES.length ? [] : SITES.map((s) => s.id)
                )
              }
              className="text-xs text-blue-600 hover:text-blue-800"
            >
              {selectedSites.length === SITES.length ? "전체해제" : "전체선택"}
            </button>
          </div>
          <div className="space-y-2">
            {SITES.map((site) => (
              <label
                key={site.id}
                className={`flex items-center gap-2.5 p-2.5 rounded-lg cursor-pointer border transition-colors ${
                  selectedSites.includes(site.id)
                    ? "border-blue-300 bg-blue-50"
                    : "border-slate-200 hover:border-slate-300"
                }`}
              >
                <input
                  type="checkbox"
                  checked={selectedSites.includes(site.id)}
                  onChange={() =>
                    setSelectedSites((prev) =>
                      prev.includes(site.id)
                        ? prev.filter((s) => s !== site.id)
                        : [...prev, site.id]
                    )
                  }
                  className="w-4 h-4 rounded text-blue-600"
                />
                <span className={`px-2 py-0.5 rounded text-xs font-medium ${site.color}`}>
                  {site.name}
                </span>
              </label>
            ))}
          </div>
        </div>

        <div className="space-y-2">
          <button
            onClick={handleSearch}
            disabled={!keyword.trim() || isLoading}
            className="w-full py-2.5 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? "검색 중..." : "🔍 검색"}
          </button>
          <button
            onClick={goToSchedule}
            className="w-full py-2.5 bg-slate-100 text-slate-700 rounded-lg text-sm font-medium hover:bg-slate-200 transition-colors"
          >
            📅 이 조건으로 스케줄 등록
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        {searchTrigger === 0 ? (
          <div className="flex flex-col items-center justify-center h-full text-slate-400">
            <div className="text-6xl mb-4">🔍</div>
            <div className="text-lg">키워드를 입력하고 검색하세요</div>
            <div className="text-sm mt-2">예: React, Java, Python, Spring</div>
          </div>
        ) : isLoading ? (
          <div className="flex flex-col items-center justify-center h-full">
            <div className="text-slate-500 text-lg mb-2">검색 중...</div>
            <div className="text-sm text-slate-400">
              {configPaths.length}개 스케줄 × {selectedSites.length}개 사이트 검사 중
            </div>
          </div>
        ) : error || crawlersError ? (
          <div className="flex flex-col items-center justify-center h-full">
            <div className="text-6xl mb-4">⚠️</div>
            <div className="text-lg text-red-600 mb-2">검색 중 오류 발생</div>
            <div className="text-sm text-slate-500">
              {(error as any)?.message || (crawlersError as any)?.message || "알 수 없는 오류"}
            </div>
            <button
              onClick={() => setSearchTrigger((t) => t + 1)}
              className="mt-4 px-4 py-2 bg-blue-600 text-white rounded-lg text-sm hover:bg-blue-700"
            >
              다시 시도
            </button>
          </div>
        ) : (
          <div className="p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-bold">
                검색 결과
                <span className="ml-2 text-sm font-normal text-slate-500">
                  {totalCount}건
                </span>
              </h2>
              <div className="text-sm text-slate-500">
                {keyword && `키워드: ${keyword}`}
                {career !== "전체" && ` | 경력: ${career}`}
                {location !== "전체" && ` | 지역: ${location}`}
              </div>
            </div>

            {totalCount === 0 ? (
              <div className="text-center py-16 text-slate-400">
                <div className="text-5xl mb-3">📭</div>
                <div className="text-lg mb-2">검색 결과가 없습니다</div>
                <div className="text-sm">
                  오늘({getDateStr(0)}) 기준 최근 {LOOKBACK_DAYS}일간 데이터를 검색했습니다.
                  {configPaths.length > 0 && (
                    <> 스케줄 실행 후 다시 시도해 주세요.</>
                  )}
                </div>
              </div>
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="bg-slate-50 border-b border-slate-200">
                        <th className="text-left p-3 font-medium text-slate-600">사이트</th>
                        <th className="text-left p-3 font-medium text-slate-600">회사명</th>
                        <th className="text-left p-3 font-medium text-slate-600">포지션</th>
                        <th className="text-left p-3 font-medium text-slate-600">경력</th>
                        <th className="text-left p-3 font-medium text-slate-600">기술</th>
                        <th className="text-left p-3 font-medium text-slate-600">지역</th>
                        <th className="text-left p-3 font-medium text-slate-600">마감</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pagedData.map((job: any, i: number) => {
                        const siteInfo = SITES.find((s) => s.name === job.site);
                        return (
                          <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
                            <td className="p-3">
                              <span className={`px-2 py-0.5 rounded text-xs font-medium ${siteInfo?.color || "bg-slate-100 text-slate-600"}`}>
                                {job.site}
                              </span>
                            </td>
                            <td className="p-3 font-medium">{job.company}</td>
                            <td className="p-3">
                              <a
                                href={job.url}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-blue-600 hover:underline"
                              >
                                {job.position}
                              </a>
                            </td>
                            <td className="p-3 text-slate-600">{job.career || "-"}</td>
                            <td className="p-3 text-slate-600">{job.tech || "-"}</td>
                            <td className="p-3 text-slate-600">{job.location || "-"}</td>
                            <td className="p-3 text-slate-600">{job.deadline || "-"}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                {totalPages > 1 && (
                  <div className="flex items-center justify-between mt-4">
                    <div className="text-sm text-slate-500">
                      {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, totalCount)} / {totalCount}
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => setPage((p) => Math.max(0, p - 1))}
                        disabled={page === 0}
                        className="px-3 py-1.5 text-sm border border-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-50"
                      >
                        ◀
                      </button>
                      {Array.from({ length: Math.min(totalPages, 10) }, (_, i) => {
                        const start = Math.max(0, Math.min(page - 4, totalPages - 10));
                        const pageNum = start + i;
                        if (pageNum >= totalPages) return null;
                        return (
                          <button
                            key={pageNum}
                            onClick={() => setPage(pageNum)}
                            className={`w-8 h-8 text-sm rounded-lg ${
                              pageNum === page
                                ? "bg-blue-600 text-white"
                                : "text-slate-600 hover:bg-slate-100 border border-slate-300"
                            }`}
                          >
                            {pageNum + 1}
                          </button>
                        );
                      })}
                      <button
                        onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                        disabled={page >= totalPages - 1}
                        className="px-3 py-1.5 text-sm border border-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-50"
                      >
                        ▶
                      </button>
                    </div>
                  </div>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
