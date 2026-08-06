import { useState, useEffect, useMemo } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { fetchCrawlers, executeCrawler, fetchJobPostings, downloadJobPostingsExcel, fetchCrawlLogsGrouped, type JobPostingItem } from "../api/scraper";
import { useCrawlProgress } from "../contexts/CrawlProgressContext";

const SITES = [
  { id: "saramin", name: "사람인", color: "bg-blue-100 text-blue-700 border-blue-200" },
  { id: "jobkorea", name: "잡코리아", color: "bg-green-100 text-green-700 border-green-200" },
  { id: "wanted", name: "원티드", color: "bg-red-100 text-red-700 border-red-200" },
  { id: "remember", name: "리멤버", color: "bg-purple-100 text-purple-700 border-purple-200" },
];

const SITE_TAB_COLORS: Record<string, string> = {
  "사람인": "bg-blue-600 text-white",
  "잡코리아": "bg-green-600 text-white",
  "원티드": "bg-red-600 text-white",
  "리멤버": "bg-purple-600 text-white",
};

const COLUMNS: { key: string; label: string; w: string }[] = [
  { key: "site", label: "사이트", w: "w-[80px]" },
  { key: "position", label: "포지션", w: "w-auto" },
  { key: "company", label: "회사명", w: "w-[120px]" },
  { key: "career", label: "경력", w: "w-[70px]" },
  { key: "location", label: "지역", w: "w-[70px]" },
  { key: "tech", label: "기술", w: "w-[180px]" },
  { key: "deadline", label: "마감", w: "w-[70px]" },
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
  const SIZE = 20;

  const executeMutation = useMutation({
    mutationFn: async (configId: number) => {
      const crawler = crawlers?.find((c) => c.id === configId);
      startProgress(configId, crawler?.name || "공고 수집");
      await new Promise(resolve => setTimeout(resolve, 100));
      return executeCrawler(configId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
      queryClient.invalidateQueries({ queryKey: ["jobDates"] });
      queryClient.invalidateQueries({ queryKey: ["crawlLogsGrouped"] });
    },
    onError: (e: Error) => alert(`실행 실패: ${e.message}`),
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
                          return (
                            <button
                              key={run.logId}
                              onClick={() => {
                                setSelectedDate(group.date);
                                setSelectedRunIds(run.logIds);
                                setPage(0);
                              }}
                              className={`w-full text-left px-2 py-0.5 rounded text-[11px] transition-colors flex items-center justify-between ${
                                selectedRunIds && run.logIds.some(id => selectedRunIds.includes(id))
                                  ? "bg-blue-100 text-blue-700 font-medium"
                                  : "hover:bg-slate-50 text-slate-500"
                              }`}
                            >
                              <span className="flex items-center gap-1">
                                <span className={run.status === "SUCCESS" ? "text-green-500" : run.status === "FAILED" ? "text-red-500" : "text-yellow-500"}>
                                  {statusIcon}
                                </span>
                                <span>{time} ({run.siteCount}개 사이트)</span>
                              </span>
                              <span className="text-[10px] text-slate-400">{run.newCount}건</span>
                            </button>
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
        <div className="bg-white border-b border-slate-200 px-4 py-2 flex items-center gap-2">
          <div className="flex items-center gap-2 mr-4">
            <span className="text-[13px] font-semibold text-slate-800">
              {selectedCrawler?.name || "전체"}
            </span>
            {selectedDate && (
              <span className="text-[11px] text-slate-400">| {selectedDate}</span>
            )}
          </div>

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
          {SITES.map((site) => (
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
            <span className="text-[12px] text-slate-500">{total}건</span>
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
          ) : jobs.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-slate-400">
              <div className="text-4xl mb-2">📋</div>
              <div className="text-[13px]">데이터가 없습니다</div>
              <div className="text-[11px] mt-1">수동 수집을 실행해 보세요</div>
            </div>
          ) : (
            <div className="p-3">
              <table className="w-full text-[12px] table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 sticky top-0 z-10">
                  <tr>
                    <th className="px-2 py-1.5 text-left font-semibold text-slate-500 w-[32px]">#</th>
                    {COLUMNS.map((c) => (
                      <th
                        key={c.key}
                        onClick={() => toggleSort(c.key)}
                        className={`px-2 py-1.5 text-left font-semibold text-slate-500 cursor-pointer hover:text-blue-600 select-none ${c.w}`}
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
                  {jobs.map((job: JobPostingItem, i: number) => {
                    const no = page * SIZE + i + 1;
                    const siteDef = SITES.find((s) => s.id === job.site || s.name === job.site);
                    return (
                      <tr
                        key={job.id}
                        onClick={() => job.url && window.open(job.url, "_blank")}
                        className="hover:bg-blue-50/50 cursor-pointer transition-colors"
                      >
                        <td className="px-2 py-1 text-slate-400">{no}</td>
                        <td className="px-2 py-1">
                          <span className={`px-1 py-0.5 rounded text-[10px] font-medium ${siteDef?.color || "bg-slate-100 text-slate-600"}`}>
                            {siteDef?.name || job.site}
                          </span>
                        </td>
                        <td className="px-2 py-1 font-medium text-slate-800 truncate">
                          {job.position || "-"}
                        </td>
                        <td className="px-2 py-1 text-slate-600 truncate">{job.company || "-"}</td>
                        <td className="px-2 py-1 text-slate-500 truncate">{job.career || "-"}</td>
                        <td className="px-2 py-1 text-slate-500 truncate">{job.location || "-"}</td>
                        <td className="px-2 py-1">
                          {job.tech ? (
                            <span className="text-[10px] text-blue-600 bg-blue-50 px-1 py-0.5 rounded truncate block">{job.tech}</span>
                          ) : <span className="text-slate-300">-</span>}
                        </td>
                        <td className="px-2 py-1 text-slate-400 truncate">{job.deadline || "-"}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {totalPages > 1 && (
                <div className="py-2 flex items-center justify-center gap-0.5 mt-3">
                  <button onClick={() => setPage(0)} disabled={page === 0}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
                  <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
                  {Array.from({ length: Math.min(7, totalPages) }, (_, i) => {
                    let pageNum: number;
                    if (totalPages <= 7) {
                      pageNum = i;
                    } else if (page <= 3) {
                      pageNum = i;
                    } else if (page >= totalPages - 4) {
                      pageNum = totalPages - 7 + i;
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
                  <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
                  <button onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}
                    className="px-1.5 py-0.5 text-[11px] rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
                  <span className="ml-2 text-[10px] text-slate-400">{page + 1}/{totalPages}</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
