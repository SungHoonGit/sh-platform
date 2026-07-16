import { useState } from "react";
import { useQuery } from "@tanstack/react-query";


interface FileTreeProps {
  rootPath: string;
  onSelectFile: (path: string) => void;
  selectedFile: string | null;
}

export default function FileTree({ rootPath, onSelectFile, selectedFile }: FileTreeProps) {
  const { data: files, isLoading } = useQuery({
    queryKey: ["files", rootPath],
    queryFn: async () => {
      const params = new URLSearchParams({ rootPath });
      const res = await fetch(`/scraper/docs/list?${params}`);
      if (!res.ok) throw new Error("Failed to fetch files");
      return res.json();
    },
  });

  if (isLoading) {
    return <div className="p-4 text-sm text-slate-500">로딩 중...</div>;
  }

  if (!files || files.length === 0) {
    return <div className="p-4 text-sm text-slate-400">파일이 없습니다</div>;
  }

  return (
    <div className="font-mono text-sm">
      {files.map((item: any) => (
        <TreeItem
          key={item.name}
          item={item}
          rootPath={rootPath}
          onSelectFile={onSelectFile}
          selectedFile={selectedFile}
          depth={0}
        />
      ))}
    </div>
  );
}

function TreeItem({
  item,
  rootPath,
  onSelectFile,
  selectedFile,
  depth,
}: {
  item: any;
  rootPath: string;
  onSelectFile: (path: string) => void;
  selectedFile: string | null;
  depth: number;
}) {
  const [expanded, setExpanded] = useState(depth < 1);
  const isFile = item.type === "file";
  const isSelected = selectedFile === item.path;

  const handleClick = () => {
    if (isFile) {
      onSelectFile(item.path);
    } else {
      setExpanded(!expanded);
    }
  };

  return (
    <div>
      <div
        onClick={handleClick}
        className={`flex items-center gap-1.5 py-1 px-2 cursor-pointer hover:bg-slate-100 rounded ${
          isSelected ? "bg-blue-50 text-blue-700" : ""
        }`}
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
      >
        {!isFile && (
          <span className="text-slate-400 text-xs w-4">
            {expanded ? "▼" : "▶"}
          </span>
        )}
        <span className="text-base">
          {isFile ? "📄" : expanded ? "📂" : "📁"}
        </span>
        <span className="truncate">{item.name}</span>
        {item.count && (
          <span className="ml-auto text-xs text-slate-400">{item.count}건</span>
        )}
      </div>
      {!isFile && expanded && item.children && (
        <div>
          {item.children.map((child: any) => (
            <TreeItem
              key={child.name}
              item={child}
              rootPath={rootPath}
              onSelectFile={onSelectFile}
              selectedFile={selectedFile}
              depth={depth + 1}
            />
          ))}
        </div>
      )}
    </div>
  );
}
