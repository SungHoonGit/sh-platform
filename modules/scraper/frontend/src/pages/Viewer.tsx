import { EyeOff } from "lucide-react";
import { useState, useEffect, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { fetchCrawlers, executeCrawler, fetchJobPostings, downloadJobPostingsExcel, fetchCrawlLogsGrouped, deleteCrawlLog, fetchMyScraps, scrapPosting, unscrapPosting, fetchBlacklist, addBlacklist, removeBlacklist, type JobPostingItem, type BlacklistItem } from "../api/scraper";
import { deadlineBadge, jobPlanetQuery, normCompany } from "../common/jobPlanet";

import { useCrawlProgress } from "../contexts/CrawlProgressContext";
import { BlockConfirmDialog, BlacklistManagerModal } from "@sh-platform/ui";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700 border-blue-200" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700 border-green-200" },
];

const SITE_TAB_COLORS: Record<string, string> = {
  "사람인": "bg-blue-600 text-white",
  "잡코리아": "bg-green-600 text-white",
  "원티드": "bg-red-600 text-white",
  "리멤버": "bg-purple-600 text-white",
};

const COLUMNS: { key: string; label: string; w: string }[] = [
  { key: "scrap", label: "", w: "w-[32px]" },
  { key: "site", label: "사이트", w: "w-[80px]" },
  { key: "position", label: "포지션", w: "w-auto" },
  { key: "company", label: "회사명", w: "w-[120px]" },
  { key: "career", label: "경력", w: "w-[80px]" },
  { key: "location", label: "지역", w: "w-[80px]" },
  { key: "tech", label: "기술", w: "w-[180px]" },
  { key: "deadline", label: "마감", w: "w-[110px]" },
];

export default function Viewer() {
  const [searchParams] = useSearchParams();
  const crawlerId = searchParams.get("crawler");
  const queryClient = useQueryClient();
  const { startProgress } = useCrawlProgress();
  
  const [selectedCrawlerId, setSelectedCrawlerId] = useState<number | null>(
    crawlerId ? parseInt(crawlerId) : null
  );
  const [selectedSite, setSelectedSite] = useState<string>("all");
  const [selectedDate, setSelectedDate] = useState<string>("");
  const [selectedRunIds, setSelectedRunIds] = useState<number[] | null>(null);
  const [expandedDates, setExpandedDates] = useState<Set<string>>(new Set());
  const [sort, setSort] = useState<{ key: string; order: "asc" | "desc" } | null>(null);
  const [page, setPage] = useState(0);
  const [currentSearchCriteria, setCurrentSearchCriteria] = useState<string>("");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [scrappedIds, setScrappedIds] = useState<Set<number>>(new Set());
  const SIZE = 20;

  useEffect(() => {
    fetchMyScraps()
      .then((scraps) => setScrappedIds(new Set(scraps.map((s) => s.postingId))))
      .catch(() => undefined);
  }, []);

  const toggleScrap = async (postingId: number) => {
    if (scrappedIds.has(postingId)) {
      await unscrapPosting(postingId).catch(() => undefined);
    } else {
      await scrapPosting(postingId).catch(() => undefined);
    }
    fetchMyScraps()
      .then((scraps) => setScrappedIds(new Set(scraps.map((s) => s.postingId))))
      .catch(() => undefined);
  };

  const executeMutation = useMutation({
    mutationFn: async (configId: number) => {
      const crawler = crawlers?.find((c) => c.id === configId);
      startProgress(configId, crawler?.name || "공고 수집");
      await new Promise(resolve => setTimeout(resolve, 100));
      return executeCrawler(configId);
    },
    onError: (e: Error) => alert(`실행 실패: ${e.message}`),
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCrawlLog,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["crawlLogsGrouped"] });
      setSelectedRunIds(null);
      setCurrentSearchCriteria("");
    },
    onError: (e: Error) => alert(`삭제 실패: ${e.message}`),
  });

  const { data: crawlers } = useQuery({
    queryKey: ["crawlers"],
    queryFn: fetchCrawlers,
  });

  useEffect(() => {
    if (crawlers && crawlers.length > 0 && !selectedCrawlerId) {
      setSelectedCrawlerId(crawlers[0].id);
    }
  }, [crawlers]);

  const selectedCrawler = useMemo(
    () => crawlers?.find((c) => c.id === selectedCrawlerId) ?? null,
    [crawlers, selectedCrawlerId]
  );

  const { data: groupedLogs } = useQuery({
    queryKey: ["crawlLogsGrouped", selectedCrawlerId],
    queryFn: () => fetchCrawlLogsGrouped(selectedCrawlerId!, 30),
    enabled: !!selectedCrawlerId,
  });

  useEffect(() => {
    if (groupedLogs && groupedLogs.length > 0 && !selectedDate) {
      setSelectedDate(groupedLogs[0].date);
      setExpandedDates(new Set([groupedLogs[0].date]));
    }
  }, [groupedLogs]);

  useEffect(() => {
    if (selectedDate && selectedCrawlerId) {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
    }
  }, [selectedDate, selectedCrawlerId]);

  const { data: jobsData, isLoading } = useQuery({
    queryKey: ["jobs", selectedCrawlerId, selectedSite, selectedDate, selectedRunIds, page, sort],
    queryFn: async () => {
      if (!selectedCrawlerId || !selectedDate) return { jobs: [], total: 0, page: 0, size: SIZE };
      const result = await fetchJobPostings(selectedCrawlerId, {
        siteName: selectedSite === "all" ? undefined : selectedSite,
        crawledAt: selectedDate,
        runIds: selectedRunIds || undefined,
        page,
        size: SIZE,
        sortKey: sort?.key,
        sortOrder: sort?.order,
      });
      return result;
    },
    enabled: !!selectedCrawlerId && !!selectedDate,
    staleTime: 0,
    refetchOnMount: true,
  });

  const jobs = jobsData?.jobs || [];
  const total = jobsData?.total || 0;
  const totalPages = Math.ceil(total / SIZE);

  // 스케줄에 설정된 활성 사이트만 표시
  const availableSites = useMemo(() => {
    if (!selectedCrawler?.siteConfigs) return SITES;
    const enabledSites = new Set(
      selectedCrawler.siteConfigs
        .filter((sc) => sc.isEnabled)
        .map((sc) => sc.siteName)
    );
    return SITES.filter((s) => enabledSites.has(s.id));
  }, [selectedCrawler]);

  const filteredJobs = useMemo(() => {
    const kw = searchKeyword.trim().toLowerCase();
    if (!kw) return jobs;
    return jobs.filter((j: JobPostingItem) =>
      [j.position, j.company, j.tech, j.location, j.career, j.site]
        .filter(Boolean)
        .some((v) => String(v).toLowerCase().includes(kw))
    );
  }, [jobs, searchKeyword]);

  const [blacklisted, setBlacklisted] = useState<Set<string>>(new Set());
  const [showBl, setShowBl] = useState(false);
  const [blItems, setBlItems] = useState<BlacklistItem[]>([]);
  const [blockDialog, setBlockDialog] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const loadBl = () => {
    fetchBlacklist().then((l) => { setBlItems(l); setBlacklisted(new Set(l.map((b) => b.companyNameNormalized))); }).catch(() => {});
    setShowBl(true);
  };
  const unblock = async (item: BlacklistItem) => {
    try {
      await removeBlacklist(item.id);
      setBlItems((prev) => prev.filter((b) => b.id !== item.id));
      setBlacklisted((prev) => { const n = new Set(prev); n.delete(item.companyNameNormalized); return n; });
    } catch { showNotice("해제 실패"); }
  };
  const showNotice = (msg: string) => {
    setNotice(msg);
    window.setTimeout(() => setNotice(null), 3000);
  };
  const blockCompany = (company: string) => {
    if (!company) return;
    setBlockDialog(company);
  };
  const confirmBlock = async (company: string, reason: string, reasonIds: number[]) => {
    try {
      await addBlacklist(company, reasonIds, reason || undefined);
      setBlacklisted((prev) => new Set(prev).add(normCompany(company)));
      showNotice("차단했습니다. 이 회사 공고는 더 이상 표시되지 않습니다.");
    } catch { showNotice("차단에 실패했습니다."); }
  };

  useEffect(() => {
    fetchBlacklist()
      .then((list) => setBlacklisted(new Set(list.map((b) => b.companyNameNormalized))))
      .catch(() => {});
  }, []);

  const displayJobs = (searchKeyword.trim() ? filteredJobs : jobs).filter((j: JobPostingItem) => !blacklisted.has(normCompany(j.company)));
  const displayTotal = searchKeyword.trim() ? filteredJobs.length : total;
  const displayTotalPages = searchKeyword.trim() ? Math.max(1, Math.ceil(filteredJobs.length / SIZE)) : totalPages;

  const toggleSort = (key: string) => {
    setPage(0);
    setSort((prev) => {
      if (!prev || prev.key !== key) return { key, order: "asc" };
      if (prev.order === "asc") return { key, order: "desc" };
      return null;
    });
  };

  const toggleExpand = (date: string) => {
    setExpandedDates((prev) => {
      const next = new Set(prev);
      if (next.has(date)) {
        next.delete(date);
      } else {
        next.add(date);
      }
      return next;
    });
  };

  return (
    <div className="flex h-full text-[13px]">
      {/* 왼쪽 사이드바 */}
      <div className="w-60 bg-white border-r border-slate-200 shrink-0 overflow-auto flex flex-col">
        <div className="px-3 py-2.5 border-b border-slate-200">
          <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wide">스케줄</h3>
        </div>
        
        <div className="p-1.5">
          {crawlers?.map((c) => (
            <div key={c.id} className="flex items-center gap-1 mb-0.5">
              <button
                onClick={() => {
                  setSelectedCrawlerId(c.id);
                  setSelectedSite("all");
                  setSelectedDate("");
                  setExpandedDates(new Set());
                  setPage(0);
                }}
                className={`flex-1 text-left px-2.5 py-1.5 rounded text-[12px] transition-colors ${
                  selectedCrawlerId === c.id
                    ? "bg-blue-50 text-blue-700 font-medium"
                    : "hover:bg-slate-50 text-slate-600"
                }`}
              >
                <span className="mr-1">{c.scheduleIcon || "🤖"}</span>
                {c.name}
              </button>
              <button
                onClick={() => executeMutation.mutate(c.id)}
                disabled={executeMutation.isPending}
                className="px-1.5 py-1 text-[10px] rounded bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors disabled:opacity-50"
                title="수동 실행"
              >
                ▶
              </button>
            </div>
          ))}
        </div>

        {/* 날짜별 수집 이력 트리 */}
        {groupedLogs && groupedLogs.length > 0 && (
          <div className="px-3 py-2.5 border-t border-slate-200">
            <h3 className="text-[11px] font-bold text-slate-500 mb-1.5">수집 이력</h3>
            <div className="space-y-0.5">
              {groupedLogs.map((group) => {
                const isExpanded = expandedDates.has(group.date);
                return (
                  <div key={group.date}>
                    {/* 1depth: 날짜 */}
                    <div className="flex items-center">
                      <button
                        onClick={() => toggleExpand(group.date)}
                        className="w-4 h-4 flex items-center justify-center text-slate-400 hover:text-slate-600"
                      >
                        {isExpanded ? "▼" : "▶"}
                      </button>
                      <button
                        onClick={() => {
                          setSelectedDate(group.date);
                          setSelectedRunIds(null);
                          setCurrentSearchCriteria("");
                          setPage(0);
                        }}
                        className={`flex-1 text-left px-1.5 py-0.5 rounded text-[12px] transition-colors flex items-center justify-between ${
                          selectedDate === group.date && !selectedRunIds
                            ? "bg-blue-50 text-blue-700 font-medium"
                            : "hover:bg-slate-50 text-slate-600"
                        }`}
                      >
                        <span>{group.date}</span>
                        <span className="text-[10px] text-slate-400">
                          {group.totalRunCount}회 신규 {group.totalNewCount}건
                        </span>
                      </button>
                    </div>
                    
                    {/* 2depth: 수집 실행 */}
                    {isExpanded && (
                      <div className="ml-4 mt-0.5 space-y-0.5">
                        {group.runs.map((run) => {
                          const time = run.startedAt.split("T")[1]?.substring(0, 5) || "";
                          const statusIcon = run.status === "SUCCESS" ? "✓" : run.status === "FAILED" ? "✗" : "△";
                          const isAuto = run.source === "SCHEDULE";
                          const runTypeIcon = isAuto ? "🕐" : "👆";
                          const runTypeTitle = isAuto ? "스케줄 실행" : "수동 실행";
                          const isSelected = selectedDate === group.date && selectedRunIds && run.logIds.length === selectedRunIds.length && run.logIds.every(id => selectedRunIds.includes(id));
                          const canDelete = !isAuto;
                          return (
                            <div key={run.logId} className="flex items-center gap-1">
                              <button
                                onClick={() => {
                                  setSelectedDate(group.date);
                                  setSelectedRunIds(run.logIds);
                                  setCurrentSearchCriteria(run.searchCriteria || "");
                                  setPage(0);
                                }}
                                className={`flex-1 text-left px-2 py-0.5 rounded text-[11px] transition-colors flex items-center justify-between ${
                                  isSelected
                                    ? "bg-blue-100 text-blue-700 font-medium"
                                    : "hover:bg-slate-50 text-slate-500"
                                }`}
                              >
                                <span className="flex items-center gap-1">
                                  <span className={run.status === "SUCCESS" ? "text-green-500" : run.status === "FAILED" ? "text-red-500" : "text-yellow-500"}>
                                    {statusIcon}
                                  </span>
                                  <span title={runTypeTitle}>{runTypeIcon}</span>
                                  <span>{time}</span>
                                  {run.newCriteria && (
                                    <span className="px-1 py-0.5 rounded bg-orange-100 text-orange-600 text-[9px] font-bold">new!</span>
                                  )}
                                </span>
                                <span className="text-[10px] text-slate-400">{run.newCount}건</span>
                              </button>
                              {canDelete && (
                                <button
                                  onClick={async (e) => {
                                    e.stopPropagation();
                                    if (!(await confirm(`이 수집 이력을 삭제하시겠습니까?`))) return;
                                    run.logIds.forEach(id => deleteMutation.mutate(id));
                                  }}
                                  className="px-1.5 py-0.5 text-[10px] text-red-400 hover:text-red-600 hover:bg-red-50 rounded"
                                  title="삭제"
                                >
                                  🗑️
                                </button>
                              )}
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* 메인 영역 */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* 헤더 */}
        <div className="bg-white border-b border-slate-200 px-4 py-1.5" style={{ fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif", fontSize: "13px" }}>
          {/* 첫째 줄: 크롤러 + 날짜 */}
          <div className="flex items-center gap-2 mb-0.5">
            <span className="font-semibold text-slate-800">
              {selectedCrawler?.scheduleIcon || "🤖"} {selectedCrawler?.name || "전체"}
            </span>
            {selectedDate && (
              <span className="text-[11px] text-slate-400">| {selectedDate}</span>
            )}
          </div>

          {/* 검색 조건 배지 */}
          {(() => {
            const criteriaStr = currentSearchCriteria || (selectedCrawler?.searchCriteria ? JSON.stringify(selectedCrawler.searchCriteria) : "");
            if (!criteriaStr) return null;
            try {
              const criteria = typeof criteriaStr === "string" ? JSON.parse(criteriaStr) : criteriaStr;
              const parts = [];
              if (criteria.keyword) parts.push({ label: "keyword", value: criteria.keyword, color: "bg-blue-50 text-blue-600" });
              if (criteria.career) parts.push({ label: "career", value: criteria.career, color: "bg-green-50 text-green-600" });
              if (criteria.location) parts.push({ label: "location", value: criteria.location, color: "bg-purple-50 text-purple-600" });
              if (parts.length === 0) return null;
              return (
                <div className="flex items-center gap-1 mb-1.5">
                  {parts.map((p, i) => (
                    <span key={i} className={`px-1.5 py-0.5 ${p.color} rounded text-[10px] font-medium`}>
                      {p.label}: {p.value}
                    </span>
                  ))}
                </div>
              );
            } catch {
              return null;
            }
          })()}

          {/* 둘째 줄: 사이트 탭 + 검색 + 건수 + Excel */}
          <div className="flex items-center gap-2">
            <button
            onClick={() => { setSelectedSite("all"); setPage(0); }}
            className={`px-2.5 py-1 rounded text-[12px] font-medium transition-colors ${
              selectedSite === "all"
                ? "bg-slate-800 text-white"
                : "bg-slate-100 text-slate-600 hover:bg-slate-200"
            }`}
          >
            전체
          </button>
          {availableSites.map((site) => (
            <button
              key={site.id}
              onClick={() => { setSelectedSite(site.id); setPage(0); }}
              className={`px-2.5 py-1 rounded text-[12px] font-medium transition-colors ${
                selectedSite === site.id
                  ? SITE_TAB_COLORS[site.name] || "bg-blue-600 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              {site.name}
            </button>
          ))}

          <div className="ml-auto flex items-center gap-3">
            <input
              type="text"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              placeholder="검색..."
              className="px-2 py-1 rounded border border-slate-300 text-[12px] focus:outline-none focus:ring-2 focus:ring-blue-500 w-40"
            />
            <span className="text-[12px] text-slate-500">{displayTotal}건</span>
            <button
              onClick={() => {
                if (!selectedCrawlerId) return;
                downloadJobPostingsExcel(selectedCrawlerId, {
                  siteName: selectedSite === "all" ? undefined : selectedSite,
                  crawledAt: selectedDate || undefined,
                }).catch((e) => alert(`다운로드 실패: ${(e as Error).message}`));
              }}
              disabled={!selectedCrawlerId || total === 0}
              className="px-2.5 py-1 bg-green-600 text-white rounded text-[12px] font-medium hover:bg-green-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              📥 Excel
            </button>
          </div>
          </div>
        </div>

        {/* 결과 테이블 */}
        <div className="flex-1 overflow-auto">
          {!selectedDate ? (
            <div className="flex flex-col items-center justify-center h-full text-slate-400">
              <div className="text-4xl mb-2">📅</div>
              <div className="text-[13px]">수집 이력을 선택하세요</div>
              <div className="text-[11px] mt-1">왼쪽에서 날짜를 클릭하면 데이터가 표시됩니다</div>
            </div>
          ) : isLoading ? (
            <div className="flex items-center justify-center h-full text-slate-500 text-[13px]">로딩 중...</div>
          ) : displayJobs.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-slate-400">
              <div className="text-4xl mb-2">📋</div>
              <div className="text-[13px]">데이터가 없습니다</div>
              <div className="text-[11px] mt-1">{searchKeyword ? "검색 결과가 없습니다" : "수동 수집을 실행해 보세요"}</div>
            </div>
          ) : (
            <>
            <div className="px-3 pt-3">
              <button
                onClick={loadBl}
                className="px-2.5 py-1 border border-slate-300 rounded-lg text-[11px] bg-white hover:bg-slate-50 text-slate-600"
              >
                차단 회사 관리{blItems.length > 0 ? ` (${blItems.length})` : ""}
              </button>
              {notice && (
                <span className="ml-2 text-[11px] text-slate-600 bg-slate-100 px-2 py-1 rounded">
                  {notice}
                </span>
              )}
            </div>
            <div className="p-3">
              <table className="w-full text-[12px] table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 sticky top-0 z-10">
                  <tr>
                    <th className="px-1 py-1.5 text-left font-bold text-slate-600 w-[30px]"></th>
                    <th className="px-2 py-1.5 text-left font-bold text-slate-600 w-[32px]"></th>
                    <th className="px-2 py-1.5 text-left font-bold text-slate-600 w-[32px]">#</th>
                    {COLUMNS.filter((c) => c.key !== "scrap").map((c) => (
                      <th
                        key={c.key}
                        onClick={() => toggleSort(c.key)}
                        className={`px-2 py-1.5 text-left font-bold text-slate-600 select-none cursor-pointer hover:text-blue-600 ${c.w}`}
                      >
                        <span className="inline-flex items-center gap-0.5">
                          {c.label}
                          {sort?.key === c.key && (
                            <span className="text-blue-600 text-[9px]">
                              {sort.order === "asc" ? "▲" : "▼"}
                            </span>
                          )}
                        </span>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {displayJobs.map((job: JobPostingItem, i: number) => {
                    const no = page * SIZE + i + 1;
                    const siteDef = SITES.find((s) => s.id === job.site || s.name === job.site);
                    return (
                      <tr
                        key={job.id}
                        onClick={() => job.url && window.open(job.url, "_blank")}
                        className={`hover:bg-blue-50/50 cursor-pointer transition-colors ${scrappedIds.has(job.id) ? "bg-amber-50/40" : ""}`}
                      >
                        <td className="px-1 py-1 text-center">
                          <button
                            onClick={(e) => { e.stopPropagation(); void blockCompany(job.company); }}
                            title="이 회사 공고 숨기기"
                            className="text-slate-300 hover:text-slate-600 transition-colors"
                          >
                            <EyeOff size={15} />
                          </button>
                        </td>
                        <td className="px-2 py-1 text-center">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              void toggleScrap(job.id);
                            }}
                            title={scrappedIds.has(job.id) ? "스크랩 해제" : "스크랩"}
                            className={`text-base leading-none transition-transform hover:scale-125 ${scrappedIds.has(job.id) ? "text-amber-500" : "text-slate-300 hover:text-amber-400"}`}
                          >
                            {scrappedIds.has(job.id) ? "★" : "☆"}
                          </button>
                        </td>
                        <td className="px-2 py-1 text-slate-400">{no}</td>
                        <td className="px-2 py-1">
                          <span className={`px-1 py-0.5 rounded text-[10px] font-medium ${siteDef?.color || "bg-slate-100 text-slate-600"}`}>
                            {siteDef?.name || job.site}
                          </span>
                        </td>
                        <td className="px-2 py-1 font-medium text-slate-800 truncate">
                          {job.position || "-"}
                        </td>
                        <td className="px-2 py-1 text-slate-600 truncate">
                          <span className="flex items-center gap-1 group/comp">
                            <span className="truncate">{job.company || "-"}</span>
                            {job.company && (
                              <a
                                href={`https://www.jobplanet.co.kr/search?query=${encodeURIComponent(jobPlanetQuery(job.company))}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                onClick={(e) => e.stopPropagation()}
                                className="shrink-0 text-[9px] text-green-500 hover:text-green-600 opacity-0 group-hover/comp:opacity-100 transition-opacity border border-green-200 rounded px-1 py-0.5 hover:bg-green-50"
                                title="잡플래닛에서 검색"
                              >
                                JP
                              </a>
                            )}
                          </span>
                        </td>
                        <td className="px-2 py-1 text-slate-500 truncate">{job.career || "-"}</td>
                        <td className="px-2 py-1 text-slate-500 truncate">{job.location || "-"}</td>
                        <td className="px-2 py-1">
                          {job.tech ? (
                            <span className="text-[10px] text-blue-600 bg-blue-50 px-1 py-0.5 rounded truncate block">{job.tech}</span>
                          ) : <span className="text-slate-300">-</span>}
                        </td>
                        <td className="px-2 py-1 text-slate-400 truncate">
                          {(() => {
                            const badge = deadlineBadge(job.deadline);
                            if (badge) {
                              return (
                                <span className={`inline-block px-1 py-0.5 rounded text-[10px] font-bold ${
                                  badge === "마감" ? "bg-slate-200 text-slate-500" : "bg-red-100 text-red-600"
                                }`}>
                                  {badge}
                                </span>
                              );
                            }
                            return job.deadline || "-";
                          })()}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {displayTotalPages > 1 && (
                <div className="py-2 flex items-center justify-center gap-0.5 mt-3">
                  <button onClick={() => setPage(0)} disabled={page === 0}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
                  <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
                  {Array.from({ length: Math.min(7, displayTotalPages) }, (_, i) => {
                    let pageNum: number;
                    if (displayTotalPages <= 7) {
                      pageNum = i;
                    } else if (page <= 3) {
                      pageNum = i;
                    } else if (page >= displayTotalPages - 4) {
                      pageNum = displayTotalPages - 7 + i;
                    } else {
                      pageNum = page - 3 + i;
                    }
                    return (
                      <button key={pageNum} onClick={() => setPage(pageNum)}
                        className={`w-5 h-5 text-[11px] rounded ${
                          page === pageNum ? "bg-blue-600 text-white font-medium" : "text-slate-600 hover:bg-slate-100"
                        }`}>{pageNum + 1}</button>
                    );
                  })}
                  <button onClick={() => setPage((p) => Math.min(displayTotalPages - 1, p + 1))} disabled={page >= displayTotalPages - 1}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
                  <button onClick={() => setPage(displayTotalPages - 1)} disabled={page >= displayTotalPages - 1}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
                  <span className="ml-2 text-[10px] text-slate-400">{page + 1}/{displayTotalPages}</span>
                </div>
              )}
            </div>
            </>
          )}
        </div>
      </div>
      <BlacklistManagerModal
        open={showBl}
        items={blItems}
        onClose={() => setShowBl(false)}
        onUnblock={unblock}
      />
      <BlockConfirmDialog
        open={blockDialog !== null}
        company={blockDialog ?? ""}
        onCancel={() => setBlockDialog(null)}
        onConfirm={(reason, reasonIds) => { const c = blockDialog; setBlockDialog(null); if (c) void confirmBlock(c, reason, reasonIds); }}
      />
    </div>
  );
}
