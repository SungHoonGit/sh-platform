import { useState, useEffect } from "react";
import { useQuery } from "@tanstack/react-query";
import { useSearchParams } from "react-router-dom";
import { fetchCrawlers } from "../api/scraper";
import FileTree from "../components/FileTree";

const SITE_TAB_COLORS: Record<string, string> = {
  "사람인": "bg-blue-600 text-white",
  "잡코리아": "bg-green-600 text-white",
  "원티드": "bg-red-600 text-white",
  "리멤버": "bg-purple-600 text-white",
};

export default function Viewer() {
  const [searchParams] = useSearchParams();
  const crawlerId = searchParams.get("crawler");
  
  const [selectedCrawlerId, setSelectedCrawlerId] = useState<number | null>(
    crawlerId ? parseInt(crawlerId) : null
  );
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [selectedSite, setSelectedSite] = useState<string>("");
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
    queryKey: ["jobs", selectedCrawlerId, selectedSite, filePath, page],
    queryFn: async () => {
      if (!selectedCrawler || !selectedSite) return { jobs: [], total: 0 };
      const siteNames: Record<string, string> = {
        saramin: "사람인", jobkorea: "잡코리아", wanted: "원티드", remember: "리멤버",
      };
      const pathParts = filePath.split("/");
      const fileName = pathParts[pathParts.length - 1];
      const dirPath = pathParts.length > 1 ? pathParts.slice(0, -1).join("/") : "";
      
      const params = new URLSearchParams({
        rootPath: selectedCrawler.localPath,
        path: dirPath ? `${dirPath}/${fileName}` : fileName,
        site: siteNames[selectedSite] || selectedSite,
        page: String(page),
        size: String(SIZE),
      });
      const res = await fetch(`/scraper/docs/jobs?${params}`);
      return res.json();
    },
    enabled: !!selectedCrawler && !!selectedSite,
  });

  const jobs = jobsData?.jobs || [];
  const total = jobsData?.total || 0;
  const totalPages = Math.ceil(total / SIZE);

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
                onSelectFile={setSelectedFile}
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
              <div className="mb-3 text-sm text-slate-500">
                📄 {filePath}
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-50 border-b border-slate-200">
                    <th className="text-left p-3 font-medium text-slate-600">회사명</th>
                    <th className="text-left p-3 font-medium text-slate-600">포지션</th>
                    <th className="text-left p-3 font-medium text-slate-600">경력</th>
                    <th className="text-left p-3 font-medium text-slate-600">기술</th>
                    <th className="text-left p-3 font-medium text-slate-600">지역</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job: any, i: number) => (
                    <tr key={i} className="border-b border-slate-100 hover:bg-slate-50">
                      <td className="p-3 font-medium">{job.company}</td>
                      <td className="p-3">
                        <a href={job.url} target="_blank" rel="noopener noreferrer"
                          className="text-blue-600 hover:underline">{job.position}</a>
                      </td>
                      <td className="p-3 text-slate-600">{job.career || "-"}</td>
                      <td className="p-3 text-slate-600">{job.tech || "-"}</td>
                      <td className="p-3 text-slate-600">{job.location || "-"}</td>
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
