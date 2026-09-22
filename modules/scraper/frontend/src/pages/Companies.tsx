import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Building2, Bookmark, BookmarkCheck, Ban, FileText, Plus, X, Trash2 } from "lucide-react";
import { BlockConfirmDialog } from "@sh-platform/ui";
import { companyNoteApi, type CompanyTab, type CompanySort, type SortDir } from "../api/companies";
import { fetchBlacklist, addBlacklist, removeBlacklist } from "../api/scraper";
import CompanySlideOver from "../components/CompanySlideOver";
import Stars from "../components/Stars";

const TABS: { key: CompanyTab; label: string }[] = [
  { key: "all", label: "전체" },
  { key: "bookmarked", label: "북마크" },
  { key: "blocked", label: "차단" },
];

const PAGE_SIZE = 20;

export default function Companies() {
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<CompanyTab>("all");
  const [q, setQ] = useState("");
  const [debouncedQ, setDebouncedQ] = useState("");
  const [page, setPage] = useState(0);
  // 북마크 토글 등으로 updatedAt이 바뀌어도 순서가 튀지 않게 기본 정렬은 회사명
  const [sortKey, setSortKey] = useState<CompanySort>("display");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [createName, setCreateName] = useState<string | null>(null);
  const [createNormalized, setCreateNormalized] = useState<string | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [addName, setAddName] = useState("");
  const [debouncedAdd, setDebouncedAdd] = useState("");
  const [blockTarget, setBlockTarget] = useState<string | null>(null);

  const blacklistQuery = useQuery({ queryKey: ["blacklist"], queryFn: fetchBlacklist });

  const invalidateLists = () => {
    queryClient.invalidateQueries({ queryKey: ["company-notes"] });
    queryClient.invalidateQueries({ queryKey: ["blacklist"] });
  };

  const toggleBookmark = async (id: number, current: boolean) => {
    try {
      // 해제 시 별도 함께 삭제됨 (서버 불변식)
      await companyNoteApi.update(id, { isBookmarked: !current });
      invalidateLists();
    } catch {
      alert("북마크 변경 실패.");
    }
  };

  /** 북마크 아이콘 클릭: 별점 없으면 슬라이드를 열어 별점 지정을 유도, 있으면 즉시 토글. */
  const onBookmarkClick = (item: { id: number | null; companyNameDisplay: string; companyNameNormalized: string; bookmarked: boolean; myStars: number | null }) => {
    if (!item.bookmarked && item.myStars == null) {
      openRow(item.id, item.companyNameDisplay, item.companyNameNormalized);
      return;
    }
    if (item.id != null) void toggleBookmark(item.id, item.bookmarked);
  };

  const unblockByNormalized = async (normalized: string, display: string) => {
    const list = blacklistQuery.data ?? (await fetchBlacklist().catch(() => []));
    const entry = list.find((b) => b.companyNameNormalized === normalized);
    if (!entry) {
      alert("차단 항목을 찾을 수 없습니다. 새로고침 후 다시 시도하세요.");
      return;
    }
    if (!confirm(`'${display}' 차단을 해제할까요? 숨김 처리된 공고가 다시 표시됩니다.`)) return;
    try {
      await removeBlacklist(entry.id);
      invalidateLists();
    } catch {
      alert("차단 해제 실패.");
    }
  };

  const confirmBlock = async (keyword: string, reason: string, reasonIds: number[], categoryNames: string[]) => {
    try {
      await addBlacklist(keyword, reasonIds, reason || undefined, categoryNames);
      invalidateLists();
    } catch (e) {
      alert(e instanceof Error ? e.message : "차단 실패.");
    }
  };

  const removeNote = async (id: number, display: string) => {
    if (!confirm(`'${display}' 메모를 삭제할까요? (차단·평점은 유지됩니다)`)) return;
    try {
      await companyNoteApi.remove(id);
      invalidateLists();
    } catch {
      alert("삭제 실패.");
    }
  };

  useEffect(() => {
    const t = setTimeout(() => setDebouncedAdd(addName.trim()), 300);
    return () => clearTimeout(t);
  }, [addName]);

  const suggestQuery = useQuery({
    queryKey: ["company-notes", "all", debouncedAdd, "suggest"],
    queryFn: () => companyNoteApi.list("all", debouncedAdd, 0, 5),
    enabled: addOpen && debouncedAdd.length > 0,
  });
  const suggestions = suggestQuery.data?.content ?? [];

  const crawlSuggestQuery = useQuery({
    queryKey: ["company-suggest", debouncedAdd],
    queryFn: () => companyNoteApi.suggest(debouncedAdd),
    enabled: addOpen && debouncedAdd.length > 0,
  });
  // 내 기록에 이미 있는 회사는 수집 섹션에서 제외 (중복 방지)
  const crawlSuggestions = (crawlSuggestQuery.data ?? []).filter(
    (s) => !suggestions.some((m) => m.companyNameDisplay === s.companyName)
  );

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

  const openRow = (id: number | null, name: string, normalized?: string) => {
    if (id != null) {
      setSelectedId(id);
      setCreateName(null);
      setCreateNormalized(null);
    } else {
      setSelectedId(null);
      setCreateName(name);
      setCreateNormalized(normalized ?? null);
    }
  };

  const confirmAdd = () => {
    const name = addName.trim();
    if (!name) return;
    setAddOpen(false);
    setAddName("");
    setSelectedId(null);
    setCreateName(name);
    setCreateNormalized(null);
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
          <table className="w-full min-w-[960px] text-sm">
            <thead className="sticky top-0 bg-slate-50">
              <tr className="text-left text-xs text-slate-500">
                <th className="w-[36px] px-1 py-1 text-center">차단</th>
                <th className="w-[36px] px-1 py-1 text-center">북마크</th>
                <th className="px-2 py-1">
                  <button className="hover:text-slate-800" onClick={() => toggleSort("display")}>
                    회사명{sortMark("display")}
                  </button>
                </th>
                <th className="px-2 py-1" title="차단=차단 카테고리, 메모=태그">키워드</th>
                <th className="px-2 py-1">
                  <button className="hover:text-slate-800" onClick={() => toggleSort("stars")}>
                    내 별점{sortMark("stars")}
                  </button>
                </th>
                <th className="px-2 py-1">메모</th>
                <th className="px-2 py-1">비고</th>
                <th className="px-2 py-1">
                  <button className="hover:text-slate-800" onClick={() => toggleSort("updated")}>
                    업데이트{sortMark("updated")}
                  </button>
                </th>
                <th className="w-[36px] px-1 py-1 text-center">삭제</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {items.map((c) => (
                <tr
                  key={`${c.id ?? "b"}-${c.companyNameDisplay}`}
                  className={`cursor-pointer transition-colors hover:bg-blue-50/50 ${c.blocked ? "bg-red-50/50" : c.bookmarked ? "bg-amber-50/40" : ""}`}
                  onClick={() => openRow(c.id, c.companyNameDisplay, c.companyNameNormalized)}
                >
                  <td className="px-1 py-1 text-center" onClick={(e) => e.stopPropagation()}>
                    {c.blocked ? (
                      <button
                        onClick={() => void unblockByNormalized(c.companyNameNormalized, c.companyNameDisplay)}
                        title={`차단 해제 (키워드: ${c.companyNameNormalized}${c.hiddenCount != null ? `, ${c.hiddenCount}건 숨김` : ""})`}
                        className="text-red-500 transition-colors hover:text-red-700"
                      >
                        <Ban size={15} />
                      </button>
                    ) : (
                      <button
                        onClick={() => setBlockTarget(c.companyNameDisplay)}
                        title="차단하기"
                        className="text-slate-300 transition-colors hover:text-red-500"
                      >
                        <Ban size={15} />
                      </button>
                    )}
                  </td>
                  <td className="px-1 py-1 text-center" onClick={(e) => e.stopPropagation()}>
                    {c.blocked || (c.id == null && !c.bookmarked) ? (
                      <span className="inline-block text-slate-200" title="차단된 회사는 북마크 불가">
                        <Bookmark size={15} />
                      </span>
                    ) : (
                      <button
                        onClick={() => onBookmarkClick(c)}
                        title={c.bookmarked ? "북마크 해제 (별도 함께 삭제)" : c.myStars == null ? "북마크 + 별점 지정" : "북마크"}
                        className={`transition-transform hover:scale-125 ${c.bookmarked ? "text-amber-500" : "text-slate-300 hover:text-amber-400"}`}
                      >
                        {c.bookmarked ? <BookmarkCheck size={15} className="fill-amber-400 text-amber-400" /> : <Bookmark size={15} />}
                      </button>
                    )}
                  </td>
                  <td className="px-2 py-1 font-medium text-slate-800">{c.companyNameDisplay}</td>
                  <td className="px-2 py-1">
                    {c.blocked
                      ? (c.blockReasons?.length ?? 0) > 0 ? (
                        <span className="inline-flex flex-wrap gap-1">
                          {c.blockReasons.map((t) => (
                            <span key={t.id} className="rounded-full bg-red-100 px-1.5 py-0.5 text-[11px] text-red-700">
                              {t.name}
                            </span>
                          ))}
                        </span>
                      ) : (
                        <span className="text-slate-300">-</span>
                      )
                      : (c.categories?.length ?? 0) > 0 ? (
                        <span className="inline-flex flex-wrap gap-1">
                          {c.categories.map((t) => (
                            <span key={t.id} className="rounded-full bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">
                              {t.name}
                            </span>
                          ))}
                        </span>
                      ) : (
                        <span className="text-slate-300">-</span>
                      )}
                  </td>
                  <td className="px-2 py-1">
                    <Stars value={c.myStars} />
                  </td>
                  <td className="px-2 py-1">
                    {c.hasNote ? <FileText size={15} className="text-blue-500" /> : <span className="text-slate-300">-</span>}
                  </td>
                  <td className="px-2 py-1 text-xs text-slate-500">
                    {c.blocked && c.hiddenCount != null ? (
                      <span className="font-semibold text-red-600">{c.hiddenCount}건 숨김</span>
                    ) : c.hiddenCount != null && c.hiddenCount > 0 ? (
                      <span>{c.hiddenCount}건 수집</span>
                    ) : (
                      <span className="text-slate-300">-</span>
                    )}
                  </td>
                  <td className="px-2 py-1 text-xs text-slate-500">
                    {c.updatedAt ? new Date(c.updatedAt).toLocaleDateString() : "-"}
                  </td>
                  <td className="px-1 py-1 text-center" onClick={(e) => e.stopPropagation()}>
                    {c.id != null ? (
                      <button
                        onClick={() => void removeNote(c.id as number, c.companyNameDisplay)}
                        title="메모 삭제 (차단·평점은 유지)"
                        className="text-slate-300 transition-colors hover:text-red-500"
                      >
                        <Trash2 size={15} />
                      </button>
                    ) : (
                      <span className="text-slate-200">-</span>
                    )}
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
              className="mb-2 w-full rounded border border-slate-200 px-2.5 py-1.5 text-sm"
              placeholder="예: 삼성전자(주)"
              value={addName}
              autoFocus
              onChange={(e) => setAddName(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && confirmAdd()}
            />
            {debouncedAdd.length > 0 && suggestions.length > 0 && (
              <>
                <p className="mb-1 text-[11px] font-semibold text-slate-500">내 기록</p>
                <ul className="mb-2 max-h-36 overflow-auto rounded border border-slate-200">
                  {suggestions.map((s) => (
                    <li key={`${s.id ?? "b"}-${s.companyNameDisplay}`}>
                      <button
                        className="flex w-full items-center gap-1.5 px-2.5 py-1.5 text-left text-sm hover:bg-blue-50"
                        onClick={() => {
                          setAddOpen(false);
                          setAddName("");
                          openRow(s.id, s.companyNameDisplay, s.companyNameNormalized);
                        }}
                      >
                        <span className="flex-1 truncate font-medium text-slate-800">{s.companyNameDisplay}</span>
                        {s.myStars != null && <span className="text-xs text-amber-500">★{s.myStars}</span>}
                        {s.bookmarked && <Bookmark size={13} className="fill-amber-400 text-amber-400" />}
                        {s.blocked && <Ban size={13} className="text-red-500" />}
                        {s.hasNote && <FileText size={13} className="text-blue-500" />}
                      </button>
                    </li>
                  ))}
                </ul>
              </>
            )}
            {debouncedAdd.length > 0 && crawlSuggestions.length > 0 && (
              <>
                <p className="mb-1 text-[11px] font-semibold text-slate-500">수집된 회사 (뷰어 데이터)</p>
                <ul className="mb-2 max-h-36 overflow-auto rounded border border-slate-200">
                  {crawlSuggestions.map((s) => (
                    <li key={`crawl-${s.normalized}`}>
                      <button
                        className="flex w-full items-center gap-1.5 px-2.5 py-1.5 text-left text-sm hover:bg-blue-50"
                        onClick={() => {
                          setAddOpen(false);
                          setAddName("");
                          openRow(s.noteId, s.companyName, s.normalized);
                        }}
                      >
                        <span className="flex-1 truncate font-medium text-slate-800">{s.companyName}</span>
                        {s.hasNote && <FileText size={13} className="text-blue-500" />}
                        {s.blocked && <Ban size={13} className="text-red-500" />}
                      </button>
                    </li>
                  ))}
                </ul>
              </>
            )}
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
          companyNormalized={createNormalized ?? undefined}
          onClose={() => {
            setSelectedId(null);
            setCreateName(null);
            setCreateNormalized(null);
          }}
          onSaved={(newId) => {
            setSelectedId(newId);
            setCreateName(null);
            setCreateNormalized(null);
          }}
        />
      )}
      <BlockConfirmDialog
        open={blockTarget != null}
        company={blockTarget ?? ""}
        title="회사 차단"
        confirmLabel="차단"
        editableCompany
        onCancel={() => setBlockTarget(null)}
        onConfirm={(reason, reasonIds, categoryNames, keyword) => {
          const t = blockTarget;
          setBlockTarget(null);
          if (t && keyword) void confirmBlock(keyword, reason, reasonIds, categoryNames);
        }}
      />
    </div>
  );
}
