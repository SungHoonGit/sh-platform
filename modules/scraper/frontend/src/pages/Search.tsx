import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { realTimeSearch, type SearchResponse } from "../api/scraper";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700" },
  { id: "wanted", name: "원티드", color: "bg-red-100 text-red-700" },
  { id: "remember", name: "리멤버", color: "bg-purple-100 text-purple-700" },
];

const CAREERS = ["전체", "경력무관", "1~3년", "3~5년", "5~10년", "10년 이상"];
const LOCATIONS = ["전체", "서울", "경기", "인천", "부산", "대구", "기타"];

export default function Search() {
  const navigate = useNavigate();
  const [keyword, setKeyword] = useState("");
  const [career, setCareer] = useState("전체");
  const [location, setLocation] = useState("전체");
  const [selectedSites, setSelectedSites] = useState<string[]>(["saramin", "jobkorea", "wanted", "remember"]);
  const [searched, setSearched] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [data, setData] = useState<SearchResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSearch = async () => {
    setSearched(true);
    setIsLoading(true);
    setError(null);

    try {
      const result = await realTimeSearch({
        keyword: keyword || undefined,
        career: career !== "전체" ? career : undefined,
        location: location !== "전체" ? location : undefined,
        sites: selectedSites,
      });
      setData(result);
    } catch (e) {
      console.error("Search error:", e);
      setError("검색 중 오류가 발생했습니다");
    } finally {
      setIsLoading(false);
    }
  };

  const toggleSite = (siteId: string) => {
    setSelectedSites((prev) =>
      prev.includes(siteId)
        ? prev.filter((s) => s !== siteId)
        : [...prev, siteId]
    );
  };

  const toggleAllSites = () => {
    if (selectedSites.length === SITES.length) {
      setSelectedSites([]);
    } else {
      setSelectedSites(SITES.map((s) => s.id));
    }
  };

  const goToSchedule = () => {
    navigate("/schedule", { state: { keyword, career, location, sites: selectedSites } });
  };

  return (
    <div className="flex h-full">
      {/* 왼쪽: 필터 */}
      <div className="w-72 bg-white border-r border-slate-200 p-5 shrink-0 overflow-auto">
        <h3 className="text-sm font-bold text-slate-800 mb-4 uppercase tracking-wide">검색 조건</h3>

        {/* 키워드 */}
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

        {/* 경력 */}
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

        {/* 지역 */}
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

        {/* 사이트 */}
        <div className="mb-5">
          <div className="flex items-center justify-between mb-2">
            <label className="text-xs font-medium text-slate-600">사이트</label>
            <button
              onClick={toggleAllSites}
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
                  onChange={() => toggleSite(site.id)}
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
            disabled={isLoading}
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

      {/* 오른쪽: 검색 결과 */}
      <div className="flex-1 overflow-auto">
        {!searched ? (
          <div className="flex flex-col items-center justify-center h-full text-slate-400">
            <div className="text-6xl mb-4">🔍</div>
            <div className="text-lg">키워드를 입력하고 검색하세요</div>
            <div className="text-sm mt-2">예: React, Java, Python, Spring</div>
          </div>
        ) : isLoading ? (
          <div className="flex flex-col items-center justify-center h-full">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mb-4"></div>
            <div className="text-slate-600 font-medium">크롤링 중...</div>
            <div className="text-sm text-slate-400 mt-2">사이트에서 채용공고를 수집하고 있습니다</div>
          </div>
        ) : error ? (
          <div className="flex flex-col items-center justify-center h-full text-red-500">
            <div className="text-lg">{error}</div>
            <button
              onClick={handleSearch}
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
                  {data?.total || 0}건
                </span>
              </h2>
              <div className="text-sm text-slate-500">
                {data?.searchTime && `${(data.searchTime / 1000).toFixed(1)}초 소요`}
                {keyword && ` | 키워드: ${keyword}`}
                {career !== "전체" && ` | 경력: ${career}`}
                {location !== "전체" && ` | 지역: ${location}`}
              </div>
            </div>

            {/* 사이트별 결과 수 */}
            {data?.siteCounts && Object.keys(data.siteCounts).length > 0 && (
              <div className="flex gap-2 mb-4">
                {Object.entries(data.siteCounts).map(([site, count]) => {
                  const siteInfo = SITES.find((s) => s.id === site);
                  return (
                    <span
                      key={site}
                      className={`px-3 py-1 rounded-full text-xs font-medium ${siteInfo?.color || "bg-slate-100 text-slate-600"}`}
                    >
                      {siteInfo?.name || site}: {count}건
                    </span>
                  );
                })}
              </div>
            )}

            {/* 실패한 사이트 */}
            {data?.failedSites && data.failedSites.length > 0 && (
              <div className="mb-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-sm text-yellow-700">
                일부 사이트에서 오류가 발생했습니다: {data.failedSites.join(", ")}
              </div>
            )}

            {!data || !data.jobs || data.jobs.length === 0 ? (
              <div className="text-center py-16 text-slate-400">
                검색 결과가 없습니다
              </div>
            ) : (
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
                    {data.jobs.map((job: Record<string, string>, i: number) => {
                      const siteName = job.site || "";
                      const siteInfo = SITES.find((s) => s.id === siteName || s.name === siteName);
                      return (
                        <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
                          <td className="p-3">
                            <span className={`px-2 py-0.5 rounded text-xs font-medium ${siteInfo?.color || "bg-slate-100 text-slate-600"}`}>
                              {siteInfo?.name || siteName}
                            </span>
                          </td>
                          <td className="p-3 font-medium">{job.company || "-"}</td>
                          <td className="p-3">
                            <a
                              href={job.url}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-blue-600 hover:underline"
                            >
                              {job.position || job.title || "-"}
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
            )}
          </div>
        )}
      </div>
    </div>
  );
}
