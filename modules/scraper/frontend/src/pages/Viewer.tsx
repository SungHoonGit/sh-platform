import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { fetchCrawlers, fetchJobs, type FileNode } from "../api/scraper";
import FileTree from "../components/FileTree";

const SITE_TAB_COLORS: Record<string, string> = {
  "사람인": "bg-blue-600 text-white",
  "잡코리아": "bg-green-600 text-white",
  "원티드": "bg-red-600 text-white",
  "리멤버": "bg-purple-600 text-white",
};

const COLUMNS: { key: string; label: string }[] = [
  { key: "company", label: "회사명" },
  { key: "position", label: "포지션" },
  { key: "career", label: "경력" },
  { key: "tech", label: "기술" },
  { key: "location", label: "지역" },
  { key: "deadline", label: "마감" },
  { key: "site", label: "사이트" },
];

export default function Viewer() {
  const [searchParams] = useSearchParams();
  const crawlerId = searchParams.get("crawler");
  
  const [selectedCrawlerId, setSelectedCrawlerId] = useState<number | null>(
    crawlerId ? parseInt(crawlerId) : null
  );
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [selectedFileInfo, setSelectedFileInfo] = useState<FileNode | null>(null);
  const [selectedSite, setSelectedSite] = useState<string>("");
  const [sort, setSort] = useState<{ key: string; order: "asc" | "desc" } | null>(null);
  const [page, setPage] = useState(0);
  const SIZE = 20;

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
            <button
              key={c.id}
              onClick={() => {
                setSelectedCrawlerId(c.id);
                setSelectedFile(null);
                setSelectedFileInfo(null);
                setPage(0);
              }}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm mb-1 transition-colors ${
                selectedCrawlerId === c.id
                  ? "bg-blue-50 text-blue-700 font-medium"
                  : "hover:bg-slate-50 text-slate-600"
              }`}
            >
              🤖 {c.name}
            </button>
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
                <colgroup>
                  <col className={selectedSite === "all" ? "w-[14%]" : "w-[16%]"} />
                  <col className={selectedSite === "all" ? "w-[24%]" : "w-[26%]"} />
                  <col className={selectedSite === "all" ? "w-[10%]" : "w-[12%]"} />
                  <col className={selectedSite === "all" ? "w-[24%]" : "w-[26%]"} />
                  <col className={selectedSite === "all" ? "w-[12%]" : "w-[12%]"} />
                  <col className={selectedSite === "all" ? "w-[8%]" : "w-[8%]"} />
                  {selectedSite === "all" && <col className="w-[8%]" />}
                </colgroup>
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200">
                    {COLUMNS.filter(
                      (c) => c.key !== "site" || selectedSite === "all"
                    ).map((c) => (
                      <th
                        key={c.key}
                        onClick={() => toggleSort(c.key)}
                        className="text-left p-2 font-medium text-slate-600 cursor-pointer select-none hover:bg-slate-100 whitespace-nowrap overflow-hidden"
                      >
                        {c.label}
                        {sort?.key === c.key && (
                          <span className="ml-1 text-blue-600">
                            {sort.order === "asc" ? "▲" : "▼"}
                          </span>
                        )}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job: any, i: number) => (
                    <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
                      <td className="p-2 font-medium">
                        <div className="truncate" title={job.company}>{job.company}</div>
                      </td>
                      <td className="p-2">
                        <a href={job.url} target="_blank" rel="noopener noreferrer"
                          className="text-blue-600 hover:underline block truncate" title={job.position}>
                          {job.position}
                        </a>
                      </td>
                      <td className="p-2 text-slate-600">
                        <div className="truncate" title={job.career || ""}>{job.career || "-"}</div>
                      </td>
                      <td className="p-2 text-slate-600">
                        <div className="truncate" title={job.tech || ""}>{job.tech || "-"}</div>
                      </td>
                      <td className="p-2 text-slate-600">
                        <div className="truncate" title={job.location || ""}>{job.location || "-"}</div>
                      </td>
                      <td className="p-2 text-slate-600">
                        <div className="truncate" title={job.deadline || ""}>{job.deadline || "-"}</div>
                      </td>
                      {selectedSite === "all" && (
                        <td className="p-2 text-slate-600">
                          <div className="truncate" title={job.site || ""}>{job.site || "-"}</div>
                        </td>
                      )}
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* 페이지네이션 */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="px-3 py-1.5 rounded border text-sm disabled:opacity-40 hover:bg-slate-50"
                  >
                    이전
                  </button>
                  {Array.from({ length: totalPages }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => setPage(i)}
                      className={`w-9 h-9 rounded text-sm ${
                        page === i ? "bg-blue-600 text-white" : "hover:bg-slate-100"
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="px-3 py-1.5 rounded border text-sm disabled:opacity-40 hover:bg-slate-50"
                  >
                    다음
                  </button>
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
