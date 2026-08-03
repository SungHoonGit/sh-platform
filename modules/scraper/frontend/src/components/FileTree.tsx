import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { fetchFiles, type FileNode } from "../api/scraper";

interface FileTreeProps {
  rootPath: string;
  onSelectFile: (node: FileNode) => void;
  selectedFile: string | null;
}

export default function FileTree({ rootPath, onSelectFile, selectedFile }: FileTreeProps) {
  const [dir, setDir] = useState("");

  const { data: files, isLoading, isError, error } = useQuery({
    queryKey: ["files", rootPath, dir],
    queryFn: () => fetchFiles(rootPath, dir || undefined),
  });

  if (isLoading) {
    return <div className="p-4 text-sm text-slate-500">로딩 중...</div>;
  }

  if (isError) {
    return <div className="p-4 text-sm text-red-500">파일 목록 오류: {(error as Error).message}</div>;
  }

  const list = files ?? [];

  const goParent = () => {
    const idx = dir.lastIndexOf("/");
    setDir(idx > 0 ? dir.slice(0, idx) : "");
  };

  return (
    <div className="font-mono text-sm">
      {dir !== "" && (
        <div
          onClick={goParent}
          className="flex items-center gap-1.5 py-1 px-2 cursor-pointer hover:bg-slate-100 rounded text-slate-500"
        >
          <span className="text-base">📁</span>
          <span>..</span>
        </div>
      )}
      {list.length === 0 && (
        <div className="p-4 text-sm text-slate-400">파일이 없습니다</div>
      )}
      {list.map((item) => {
        const isFile = item.type === "file";
        const isSelected = selectedFile === item.path;
        return (
          <div
            key={item.path}
            onClick={() => (isFile ? onSelectFile(item) : setDir(item.path))}
            className={`flex items-center gap-1.5 py-1 px-2 cursor-pointer hover:bg-slate-100 rounded ${
              isSelected ? "bg-blue-50 text-blue-700" : ""
            }`}
          >
            <span className="text-base">{isFile ? "📄" : "📁"}</span>
            <span className="truncate">{item.name}</span>
            {isFile && item.modifiedAt && (
              <span className="ml-auto text-[10px] text-slate-400 shrink-0">
                {formatDate(item.modifiedAt)}
              </span>
            )}
            {!isFile && item.childCount != null && item.childCount > 0 && (
              <span className="ml-auto text-xs text-slate-400">{item.childCount}</span>
            )}
          </div>
        );
      })}
    </div>
  );
}

function formatDate(iso: string): string {
  return iso.slice(0, 10);
}
