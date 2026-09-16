import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Building2, Bookmark, Ban, FileText, Plus, X } from "lucide-react";
import { companyNoteApi, type CompanyTab, type CompanySort, type SortDir } from "../api/companies";
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
  const [sortKey, setSortKey] = useState<CompanySort>("updated");
  const [sortDir, setSortDir] = useState<SortDir>("desc");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [createName, setCreateName] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [addName, setAddName] = useState("");

  useEffect(() => {
    const t = setTimeout(() => {
      setDebouncedQ(q.trim());
      setPage(0);
    }, 400);
    return () => clearTimeout(t);
  }, [q]);

  const listQuery = useQuery({
    queryKey: ["company-notes", tab, debouncedQ, page, sortKey, sortDir],
    queryFn: () => companyNoteApi.list(tab, debouncedQ || undefined, page, PAGE_SIZE, sortKey, sortDir),
  });

  const toggleSort = (key: CompanySort) => {
    if (sortKey === key) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir(key === "display" ? "asc" : "desc");
    }
    setPage(0);
  };

  const sortMark = (key: CompanySort) => (sortKey === key ? (sortDir === "asc" ? " ▲" : " ▼") : "");

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

  const confirmAdd = () => {
    const name = addName.trim();
    if (!name) return;
    setAddOpen(false);
    setAddName("");
    setSelectedId(null);
    setCreateName(name);
  };

  return (
    <div className="flex h-full flex-col p-4">
      <div className="mb-3 flex flex-wrap items-center gap-2">
        <h1 className="mr-2 inline-flex items-center gap-1.5 text-lg font-bold text-slate-800">
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
              className={`rounded-full px-3 py-1.5 ${tab === t.key ? "bg-blue-600 font-semibold text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}
            >
              {t.label}
            </button>
          ))}
        </div>
        <input
          className="ml-auto w-52 rounded border border-slate-200 px-2.5 py-1.5 text-sm"
          placeholder="회사명 검색…"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />
        <button
          className="inline-flex items-center gap-1 rounded bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-blue-700"
          onClick={() => {
            setAddName("");
            setAddOpen(true);
          }}
        >
          <Plus size={15} /> 회사 추가
        </button>
      </div>

      {listQuery.isLoading && <p className="text-sm text-slate-500">불러오는 중…</p>}
      {listQuery.isError && <p className="text-sm text-red-600">목록 조회 실패.</p>}

      {!listQuery.isLoading && items.length === 0 && (
        <p className="py-10 text-center text-sm text-slate-400">표시할 회사가 없습니다.</p>
      )}

      {items.length > 0 && (
        <div className="flex-1 overflow-auto rounded border border-slate-200">
          <table className="w-full min-w-[820px] text-sm">
            <thead className="sticky top-0 bg-slate-50">
              <tr className="text-left text-xs text-slate-500">
                <th className="px-2 py-1">
                  <button className="hover:text-slate-800" onClick={() => toggleSort("display")}>
                    회사명{sortMark("display")}
                  </button>
                </th>
                <th className="px-2 py-1">크롤링 평균</th>
                <th className="px-2 py-1">
                  <button className="hover:text-slate-800" onClick={() => toggleSort("stars")}>
                    내 별점{sortMark("stars")}
                  </button>
                </th>
                <th className="px-2 py-1">북마크</th>
                <th className="px-2 py-1">차단</th>
                <th className="px-2 py-1">메모</th>
                <th className="px-2 py-1">
                  <button className="hover:text-slate-800" onClick={() => toggleSort("updated")}>
                    업데이트{sortMark("updated")}
                  </button>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((c) => (
                <tr
                  key={`${c.id ?? "b"}-${c.companyNameDisplay}`}
                  className="cursor-pointer transition-colors hover:bg-blue-50/50"
                  onClick={() => openRow(c.id, c.companyNameDisplay)}
                >
                  <td className="px-2 py-1 font-medium text-slate-800">{c.companyNameDisplay}</td>
                  <td className="px-2 py-1 text-slate-600">{c.averageScore ?? <span className="text-slate-300">-</span>}</td>
                  <td className="px-2 py-1">
                    <Stars value={c.myStars} />
                  </td>
                  <td className="px-2 py-1">
                    {c.blocked ? (
                      <span className="text-slate-300" title="차단된 회사는 북마크 불가">-</span>
                    ) : c.bookmarked ? (
                      <Bookmark size={15} className="fill-amber-400 text-amber-400" />
                    ) : (
                      <span className="text-slate-300">-</span>
                    )}
                  </td>
                  <td className="px-2 py-1">
                    {c.blocked ? (
                      <span className="inline-flex flex-col" title={`차단 키워드: ${c.companyNameNormalized}`}>
                        <span className="inline-flex items-center gap-1 font-semibold text-red-600">
                          <Ban size={15} /> 차단
                        </span>
                        <span className="text-[11px] text-slate-500">
                          {c.companyNameNormalized}
                          {c.hiddenCount != null && ` · ${c.hiddenCount}건 숨김`}
                        </span>
                      </span>
                    ) : (
                      <span className="text-slate-300">-</span>
                    )}
                  </td>
                  <td className="px-2 py-1">
                    {c.hasNote ? <FileText size={15} className="text-blue-500" /> : <span className="text-slate-300">-</span>}
                  </td>
                  <td className="px-2 py-1 text-xs text-slate-500">
                    {c.updatedAt ? new Date(c.updatedAt).toLocaleDateString() : "-"}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {totalPages > 1 && (
        <div className="mt-2 flex items-center gap-2 text-sm text-slate-600">
          <button
            className="rounded border border-slate-200 px-2.5 py-1 disabled:opacity-40"
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            이전
          </button>
          <span>
            {page + 1} / {totalPages} (전체 {total})
          </span>
          <button
            className="rounded border border-slate-200 px-2.5 py-1 disabled:opacity-40"
            disabled={page + 1 >= totalPages}
            onClick={() => setPage((p) => p + 1)}
          >
            다음
          </button>
        </div>
      )}

      {addOpen && (
        <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
          <div className="absolute inset-0 bg-black/40" onClick={() => setAddOpen(false)} />
          <div className="absolute left-1/2 top-1/3 w-full max-w-sm -translate-x-1/2 rounded-lg bg-white p-4 shadow-xl">
            <div className="mb-2 flex items-center">
              <h2 className="flex-1 text-base font-semibold text-slate-800">회사 추가</h2>
              <button className="rounded p-1 text-slate-500 hover:bg-slate-100" onClick={() => setAddOpen(false)}>
                <X size={16} />
              </button>
            </div>
            <p className="mb-2 text-xs text-slate-500">
              뷰어·스케줄 공고의 회사명과 같은 이름으로 입력하면 기존 메모·차단과 연결됩니다.
            </p>
            <input
              className="mb-3 w-full rounded border border-slate-200 px-2.5 py-1.5 text-sm"
              placeholder="예: 삼성전자(주)"
              value={addName}
              autoFocus
              onChange={(e) => setAddName(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && confirmAdd()}
            />
            <div className="flex justify-end gap-1.5">
              <button
                className="rounded border border-slate-200 px-3 py-1.5 text-sm text-slate-600"
                onClick={() => setAddOpen(false)}
              >
                취소
              </button>
              <button
                className="rounded bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
                disabled={!addName.trim()}
                onClick={confirmAdd}
              >
                추가 후 메모 작성
              </button>
            </div>
          </div>
        </div>
      )}

      {(selectedId != null || createName != null) && (
        <CompanySlideOver
          id={selectedId}
          companyName={createName ?? undefined}
          onClose={() => {
            setSelectedId(null);
            setCreateName(null);
          }}
          onSaved={(newId) => {
            setSelectedId(newId);
            setCreateName(null);
          }}
        />
      )}
    </div>
  );
}
