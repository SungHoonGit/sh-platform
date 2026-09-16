import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Building2, Bookmark, Ban, FileText, Plus } from "lucide-react";
import { companyNoteApi, type CompanyTab } from "../api/companies";
import CompanySlideOver from "../components/CompanySlideOver";
import Stars from "../components/Stars";

const TABS: { key: CompanyTab; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "bookmarked", label: "북마크" },
  { key: "blocked", label: "차단" },
];

const PAGE_SIZE = 20;

export default function Companies() {
  const [tab, setTab] = useState<CompanyTab>("all");
  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [createName, setCreateName] = useState<string | null>(null);
  const [newName, setNewName] = useState("");

  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedQ(q.trim());
      setPage(0);
    }, 400);
    return () => clearTimeout(t);
  }, [q]);

  const listQuery = useQuery({
    queryKey: ["company-notes", tab, debouncedQ, page],
    queryFn: () => companyNoteApi.list(tab, debouncedQ || undefined, page, PAGE_SIZE),
  });

  const items = listQuery.data?.content ?? [];
  const total = listQuery.data?.totalElements ?? 0;
  const totalPages = listQuery.data?.totalPages ?? 0;

  const openRow = (id: number | null, name: string) => {
    if (id != null) {
      setSelectedId(id);
      setCreateName(null);
    } else {
      setSelectedId(null);
      setCreateName(name);
    }
  };

  return (
    <div className="flex h-full flex-col p-4">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <h1 className="mr-2 inline-flex items-center gap-1.5 text-lg font-bold">
          <Building2 size={19} /> 회사 관리
        </h1>
        <div className="flex gap-1 text-sm">
          {TABS.map((t) => (
            <button
              key={t.key}
              onClick={() => {
                setTab(t.key);
                setPage(0);
              }}
              className={`rounded-full px-3 py-1.5 ${tab === t.key ? "bg-blue-600 font-semibold text-white" : "bg-gray-100 text-gray-600 hover:bg-gray-200"}`}
            >
              {t.label}
            </button>
          ))}
        </div>
        <input
          className="ml-auto w-52 rounded border px-2.5 py-1.5 text-sm"
          placeholder="회사명 검색…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <div className="flex gap-1">
          <input
            className="w-44 rounded border px-2.5 py-1.5 text-sm"
            placeholder="회사명 입력 후 추가"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && newName.trim()) {
                setSelectedId(null);
                setCreateName(newName.trim());
              }
            }}
          />
          <button
            className="inline-flex items-center gap-1 rounded bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
            disabled={!newName.trim()}
            onClick={() => {
              setSelectedId(null);
              setCreateName(newName.trim());
            }}
          >
            <Plus size={15} /> 추가
          </button>
        </div>
      </div>

      {listQuery.isLoading && <p className="text-sm text-gray-500">불러오는 중…</p>}
      {listQuery.isError && <p className="text-sm text-red-600">목록 조회 실패.</p>}

      {!listQuery.isLoading && items.length === 0 && (
        <p className="py-10 text-center text-sm text-gray-400">표시할 회사가 없습니다.</p>
      )}

      {items.length > 0 && (
        <div className="flex-1 overflow-auto rounded border">
          <table className="w-full min-w-[760px] text-sm">
            <thead className="sticky top-0 bg-gray-50">
              <tr className="text-left text-xs text-gray-500">
                <th className="px-3 py-2">회사명</th>
                <th className="px-3 py-2">크롤링 평균</th>
                <th className="px-3 py-2">내 별점</th>
                <th className="px-3 py-2">북마크</th>
                <th className="px-3 py-2">차단</th>
                <th className="px-3 py-2">메모</th>
                <th className="px-3 py-2">업데이트</th>
              </tr>
            </thead>
            <tbody>
              {items.map((c) => (
                <tr
                  key={`${c.id ?? "b"}-${c.companyNameDisplay}`}
                  className="cursor-pointer border-t hover:bg-blue-50"
                  onClick={() => openRow(c.id, c.companyNameDisplay)}
                >
                  <td className="px-3 py-2 font-medium">{c.companyNameDisplay}</td>
                  <td className="px-3 py-2">{c.averageScore ?? <span className="text-gray-300">-</span>}</td>
                  <td className="px-3 py-2">
                    <Stars value={c.myStars} />
                  </td>
                  <td className="px-3 py-2">
                    {c.bookmarked ? <Bookmark size={15} className="fill-amber-400 text-amber-400" /> : <span className="text-gray-300">-</span>}
                  </td>
                  <td className="px-3 py-2">
                    {c.blocked ? <Ban size={15} className="text-red-500" /> : <span className="text-gray-300">-</span>}
                  </td>
                  <td className="px-3 py-2">
                    {c.hasNote ? <FileText size={15} className="text-blue-500" /> : <span className="text-gray-300">-</span>}
                  </td>
                  <td className="px-3 py-2 text-xs text-gray-500">
                    {c.updatedAt ? new Date(c.updatedAt).toLocaleDateString() : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-2 flex items-center gap-2 text-sm">
          <button
            className="rounded border px-2.5 py-1 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            이전
          </button>
          <span className="text-gray-600">
            {page + 1} / {totalPages} (전체 {total})
          </span>
          <button
            className="rounded border px-2.5 py-1 disabled:opacity-40"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </button>
        </div>
      )}

      {(selectedId != null || createName != null) && (
        <CompanySlideOver
          id={selectedId}
          companyName={createName ?? undefined}
          onClose={() => {
            setSelectedId(null);
            setCreateName(null);
            setNewName("");
          }}
          onSaved={(newId) => {
            setSelectedId(newId);
            setCreateName(null);
            setNewName("");
          }}
        />
      )}
    </div>
  );
}
