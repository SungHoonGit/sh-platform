import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { fetchCrawlers, fetchJobs, executeCrawler, type FileNode } from "../api/scraper";
import FileTree from "../components/FileTree";
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
  { key: "site", label: "사이트", w: "w-[70px]" },
  { key: "position", label: "포지션", w: "w-auto" },
  { key: "company", label: "회사명", w: "w-[140px]" },
  { key: "career", label: "경력", w: "w-[80px]" },
  { key: "location", label: "지역", w: "w-[80px]" },
  { key: "tech", label: "기술", w: "w-[160px]" },
  { key: "deadline", label: "마감", w: "w-[80px]" },
];

export default function Viewer() {
  const [searchParams] = useSearchParams();
  const crawlerId = searchParams.get("crawler");
  const queryClient = useQueryClient();
  const { startProgress } = useCrawlProgress();
  
  const [selectedCrawlerId, setSelectedCrawlerId] = useState<number | null>(
    crawlerId ? parseInt(crawlerId) : null
  );
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [selectedFileInfo, setSelectedFileInfo] = useState<FileNode | null>(null);
  const [selectedSite, setSelectedSite] = useState<string>("");
  const [sort, setSort] = useState<{ key: string; order: "asc" | "desc" } | null>(null);
  const [page, setPage] = useState(0);
  const SIZE = 20;

  const executeMutation = useMutation({
    mutationFn: executeCrawler,
    onSuccess: (_, configId) => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
      const crawler = crawlers?.find((c) => c.id === configId);
      startProgress(configId, crawler?.name || "크롤링");
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

  const selectedCrawler = crawlers?.find((c) => c.id === selectedCrawlerId);

  useEffect(() => {
    if (selectedCrawler?.siteConfigs && selectedCrawler.siteConfigs.length > 0) {
      setSelectedSite(selectedCrawler!.siteConfigs![0].siteName);
    }
  }, [selectedCrawler]);

  const today = new Date().toISOString().split("T")[0];
  const filePath = selectedFile || `${today}.md`;

  const { data: jobsData, isLoading } = useQuery({
    queryKey: ["jobs", selectedCrawlerId, selectedSite, filePath, page, sort],
    queryFn: async () => {
      if (!selectedCrawler || !selectedSite) return { jobs: [], total: 0 };
      const siteNames: Record<string, string> = {
        saramin: "사람인", jobkorea: "잡코리아", wanted: "원티드", remember: "리멤버",
      };
      const pathParts = filePath.split("/");
      const fileName = pathParts[pathParts.length - 1];
      const dirPath = pathParts.length > 1 ? pathParts.slice(0, -1).join("/") : "";
      const relPath = dirPath ? `${dirPath}/${fileName}` : fileName;
      try {
        return await fetchJobs(
          selectedCrawler.localPath,
          relPath,
          selectedSite === "all" ? "all" : siteNames[selectedSite] || selectedSite,
          page,
          SIZE,
          sort?.key,
          sort?.order
        );
      } catch {
        return { jobs: [], total: 0 };
      }
    },
    enabled: !!selectedCrawler && !!selectedSite,
  });

  const jobs = jobsData?.jobs || [];
  const total = jobsData?.total || 0;
  const totalPages = Math.ceil(total / SIZE);

  const toggleSort = (key: string) => {
    setPage(0);
    setSort((prev) =>
      prev?.key === key
        ? { key, order: prev.order === "asc" ? "desc" : "asc" }
        : { key, order: "asc" }
    );
  };

  const selectFile = (node: FileNode) => {
    setSelectedFile(node.path);
    setSelectedFileInfo(node);
    setPage(0);
  };

  return (
    <div className="flex h-full">
      {/* 왼쪽 사이드바 - 스케줄 목록 + 파일 트리 */}
      <div className="w-72 bg-white border-r border-slate-200 shrink-0 overflow-auto flex flex-col">
        <div className="p-4 border-b border-slate-200">
          <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wide">스케줄</h3>
        </div>
        
        <div className="p-2">
          {crawlers?.map((c) => (
            <div key={c.id} className="flex items-center gap-1 mb-1">
              <button
                onClick={() => {
                  setSelectedCrawlerId(c.id);
                  setSelectedFile(null);
                  setSelectedFileInfo(null);
                  setPage(0);
                }}
                className={`flex-1 text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                  selectedCrawlerId === c.id
                    ? "bg-blue-50 text-blue-700 font-medium"
                    : "hover:bg-slate-50 text-slate-600"
                }`}
              >
                🤖 {c.name}
              </button>
              <button
                onClick={() => executeMutation.mutate(c.id)}
                disabled={executeMutation.isPending}
                className="px-2 py-1.5 text-xs rounded-md bg-blue-50 text-blue-600 hover:bg-blue-100 transition-colors disabled:opacity-50"
                title="수동 실행"
              >
                ▶
              </button>
            </div>
          ))}
        </div>

        {selectedCrawler && (
          <>
            <div className="p-4 border-t border-slate-200">
              <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wide">파일</h3>
            </div>
            <div className="flex-1 overflow-auto p-2">
              <FileTree
                rootPath={selectedCrawler.localPath}
                onSelectFile={selectFile}
                selectedFile={selectedFile}
              />
            </div>
          </>
        )}
      </div>

      {/* 메인 영역 */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* 사이트 탭 */}
        {selectedCrawler && (
          <div className="bg-white border-b border-slate-200 px-4 py-2 flex items-center gap-2">
            <button
              onClick={() => { setSelectedSite("all"); setPage(0); }}
              className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                selectedSite === "all"
                  ? "bg-slate-800 text-white"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              전체
            </button>
            {selectedCrawler.siteConfigs?.map((sc: any) => (
              <button
                key={sc.siteName}
                onClick={() => { setSelectedSite(sc.siteName); setPage(0); }}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  selectedSite === sc.siteName
                    ? SITE_TAB_COLORS[sc.displayName] || "bg-blue-600 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {sc.displayName}
              </button>
            ))}
            <div className="ml-auto text-sm text-slate-500">
              {total}건
            </div>
          </div>
        )}

        {/* 결과 테이블 */}
        <div className="flex-1 overflow-auto">
          {isLoading ? (
            <div className="flex items-center justify-center h-full text-slate-500">로딩 중...</div>
          ) : jobs.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-slate-400">
              <div className="text-5xl mb-3">📋</div>
              <div>데이터가 없습니다</div>
              <div className="text-sm mt-1">파일을 선택하거나 수동 수집을 실행해 보세요</div>
            </div>
          ) : (
            <div className="p-4">
              <div className="mb-3 text-sm text-slate-500 flex items-center gap-2">
                <span>📄 {filePath}</span>
                {selectedFileInfo?.modifiedAt && (
                  <span className="text-xs bg-slate-100 rounded px-2 py-0.5">
                    수집일시 {formatDateTime(selectedFileInfo.modifiedAt)}
                  </span>
                )}
              </div>
              <table className="w-full text-xs table-fixed">
                <thead className="bg-slate-50 border-b border-slate-200 sticky top-0 z-10">
                  <tr>
                    <th className="px-2 py-2 text-left font-semibold text-slate-500 w-[40px]">#</th>
                    {COLUMNS.filter(
                      (c) => c.key !== "site" || selectedSite === "all"
                    ).map((c) => (
                      <th
                        key={c.key}
                        onClick={() => toggleSort(c.key)}
                        className={`px-2 py-2 text-left font-semibold text-slate-500 cursor-pointer hover:text-blue-600 select-none ${c.w}`}
                      >
                        <span className="inline-flex items-center gap-1">
                          {c.label}
                          {sort?.key === c.key && (
                            <span className="text-blue-600">
                              {sort.order === "asc" ? "▲" : "▼"}
                            </span>
                          )}
                        </span>
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {jobs.map((job: any, i: number) => {
                    const no = page * SIZE + i + 1;
                    const siteId = job.site || "";
                    const siteDef = SITES.find((s) => s.id === siteId || s.name === siteId);
                    return (
                      <tr
                        key={i}
                        onClick={() => job.url && window.open(job.url, "_blank")}
                        className="hover:bg-blue-50/50 cursor-pointer transition-colors"
                      >
                        <td className="px-2 py-1.5 text-slate-400">{no}</td>
                        <td className="px-2 py-1.5">
                          <span className={`px-1 py-0.5 rounded text-[10px] font-medium ${siteDef?.color || "bg-slate-100 text-slate-600"}`}>
                            {siteDef?.name || siteId}
                          </span>
                        </td>
                        <td className="px-2 py-1.5 font-medium text-slate-800 truncate">
                          {job.position || "-"}
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
                        {selectedSite === "all" && (
                          <td className="px-2 py-1.5 text-slate-600">
                            <div className="truncate" title={job.site || ""}>{job.site || "-"}</div>
                          </td>
                        )}
                      </tr>
                    );
                  })}
                </tbody>
              </table>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="px-5 py-2 bg-white border-t border-slate-200 flex items-center justify-center gap-0.5 mt-4">
                  <button onClick={() => setPage(0)} disabled={page === 0}
                    className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
                  <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                    className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
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
                        className={`w-6 h-6 text-xs rounded ${
                          page === pageNum ? "bg-blue-600 text-white font-medium" : "text-slate-600 hover:bg-slate-100"
                        }`}>{pageNum + 1}</button>
                    );
                  })}
                  <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                    className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
                  <button onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}
                    className="px-1.5 py-1 text-xs rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
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

function formatDateTime(iso: string): string {
  return iso.slice(0, 10) + " " + iso.slice(11, 16);
}
