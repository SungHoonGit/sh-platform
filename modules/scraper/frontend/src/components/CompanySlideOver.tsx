import { useCallback, useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import ReactMarkdown from "react-markdown";
import { X, Star, Bookmark, BookmarkCheck, Ban, Download, Save, Trash2 } from "lucide-react";
import { BlockConfirmDialog } from "@sh-platform/ui";
import TagInput, { type TagItem } from "./TagInput";
import { companyNoteApi, type CompanyNoteDetail } from "../api/companies";
import { fetchBlacklist, addBlacklist, removeBlacklist, updateBlacklist, searchBlockCategories } from "../api/scraper";

interface Props {
  /** 메모 ID (있으면 상세 조회, 없으면 companyName으로 신규 작성) */
  id: number | null;
  companyName?: string;
  /** 차단 전용 행에서 넘어올 때 정규화명 (차단 정보 표시용) */
  companyNormalized?: string;
  onClose: () => void;
  onSaved?: (id: number) => void;
}

function scoreText(label: string, v: number | null) {
  return (
    <span className="text-xs text-gray-600">
      {label} <b className="text-gray-900">{v ?? "-"}</b>
    </span>
  );
}

export default function CompanySlideOver({ id, companyName, companyNormalized, onClose, onSaved }: Props) {
  const queryClient = useQueryClient();
  const isCreate = id == null;
  const [tab, setTab] = useState<"view" | "edit">(isCreate ? "edit" : "view");

  const detailQuery = useQuery({
    queryKey: ["company-note", id],
    queryFn: () => companyNoteApi.get(id as number),
    enabled: id != null,
  });
  const detail: CompanyNoteDetail | undefined = detailQuery.data;

  const blacklistQuery = useQuery({ queryKey: ["blacklist"], queryFn: fetchBlacklist });

  const postingsQuery = useQuery({
    queryKey: ["company-postings", id],
    queryFn: () => companyNoteApi.postings(id as number, 10),
    enabled: id != null && tab === "view" && detail != null,
    staleTime: 5 * 60 * 1000,
  });

  const [stars, setStars] = useState<number | null>(null);
  const [md, setMd] = useState("");
  const [bookmarked, setBookmarked] = useState(false);
  const [noteTags, setNoteTags] = useState<TagItem[]>([]);
  const [blockCreateOpen, setBlockCreateOpen] = useState(false);
  // 노션식 등장 애니메이션: 마운트 직후 visible ON, 닫을 때 OFF 후 onClose 지연 호출
  const [visible, setVisible] = useState(false);
  useEffect(() => {
    const raf = requestAnimationFrame(() => setVisible(true));
    return () => cancelAnimationFrame(raf);
  }, []);
  const close = useCallback(() => {
    setVisible(false);
    window.setTimeout(onClose, 260);
  }, [onClose]);

  useEffect(() => {
    if (detail) {
      setStars(detail.myStars);
      setMd(detail.noteMd ?? "");
      setBookmarked(detail.bookmarked);
      setNoteTags((detail.categories ?? []).map((c) => ({ id: c.id, name: c.name })));
      // 기존 메모는 항상 보기 탭부터 (메모 없어도 평점·차단·공고 확인 가능)
      setTab("view");
    } else if (isCreate) {
      setStars(null);
      setMd("");
      setBookmarked(false);
      setNoteTags([]);
      setTab("edit");
    }
  }, [detail?.id, isCreate, companyName]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && close();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [close]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["company-notes"] });
    queryClient.invalidateQueries({ queryKey: ["company-note"] });
    queryClient.invalidateQueries({ queryKey: ["blacklist"] });
  };

  const saveMutation = useMutation({
    mutationFn: () => {
      const reasonIds = noteTags.filter((t) => t.id != null).map((t) => t.id as number);
      const categoryNames = noteTags.filter((t) => t.id == null).map((t) => t.name);
      // 차단 행은 차단 카테고리가 인라인 즉시저장되므로 메모 태그 필드 미전송 (기존 노트 태그 보존)
      const isBlocked = detail?.blocked === true || blockedEntry != null;
      const tagFields = isBlocked ? {} : { reasonIds, categoryNames };
      return isCreate
        ? companyNoteApi.upsert({ companyName, myStars: stars, isBookmarked: bookmarked, noteMd: md, ...tagFields })
        : companyNoteApi.update(id as number, { myStars: stars, isBookmarked: bookmarked, noteMd: md, ...tagFields });
    },
    onSuccess: (saved) => {
      // 별 설정 시 북마크 자동 ON 등 서버 불변식을 화면에 반영
      setBookmarked(saved.bookmarked);
      setStars(saved.myStars ?? null);
      setNoteTags((saved.categories ?? []).map((c) => ({ id: c.id, name: c.name })));
      invalidate();
      setTab("view");
      onSaved?.(saved.id);
    },
    onError: () => alert("저장 실패."),
  });

  const bookmarkMutation = useMutation({
    mutationFn: (next: boolean) => {
      if (isCreate) return companyNoteApi.upsert({ companyName, isBookmarked: next });
      // 해제 시 별도 함께 삭제됨 (서버 불변식: 별은 북마크 필수)
      return companyNoteApi.update(id as number, { isBookmarked: next });
    },
    onSuccess: (saved) => {
      setBookmarked(saved.bookmarked);
      setStars(saved.myStars ?? null);
      invalidate();
      if (isCreate) onSaved?.(saved.id);
    },
    onError: () => alert("북마크 변경 실패."),
  });

  const removeMutation = useMutation({
    mutationFn: () => companyNoteApi.remove(id as number),
    onSuccess: () => {
      invalidate();
      close();
    },
    onError: () => alert("삭제 실패."),
  });

  const blockedEntry = blacklistQuery.data?.find(
    (b) => b.companyNameNormalized === (detail?.companyNameNormalized ?? companyNormalized ?? "")
  );

  /** 생성 모드(차단 전용 행)에서 넘어온 경우에도 차단 정보를 표시한다. */
  const createBlocked = isCreate && companyNormalized != null && blockedEntry != null;

  const onBlockToggle = () => {
    if (blocked) {
      void doUnblock();
      return;
    }
    // 차단 시 확인 다이얼로그 + 키워드 저장 (차단하면 북마크·별점 해제됨)
    setBlockCreateOpen(true);
  };

  const confirmBlockCreate = async (reason: string, reasonIds: number[], categoryNames: string[], keyword: string) => {
    if (!keyword) return;
    try {
      await addBlacklist(keyword, reasonIds, reason || undefined, categoryNames);
      setBlockCreateOpen(false);
      if (id != null && (detail?.bookmarked || detail?.myStars != null)) {
        await companyNoteApi.update(id, { isBookmarked: false }).catch(() => undefined);
      }
      invalidate();
      detailQuery.refetch();
    } catch (e) {
      alert(e instanceof Error ? e.message : "차단 실패.");
    }
  };

  const exportMd = async () => {
    if (id == null) return;
    const token = localStorage.getItem("accessToken") ?? "";
    const res = await fetch(companyNoteApi.exportUrl(id), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!res.ok) {
      alert("내보내기 실패.");
      return;
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${detail?.companyNameDisplay ?? "company"}-analysis.md`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const title = detail?.companyNameDisplay ?? companyName ?? "";
  const blocked = (detail?.blocked ?? false) || createBlocked;

  const doUnblock = async () => {
    if (blockedEntry == null) return;
    if (!confirm(`'${blockedEntry.companyNameNormalized}' 차단을 해제할까요? 숨김 처리된 공고가 다시 표시됩니다.`)) return;
    try {
      await removeBlacklist(blockedEntry.id);
      invalidate();
      detailQuery.refetch();
    } catch {
      alert("차단 해제 실패.");
    }
  };

  const removeCategory = async (reasonId: number | undefined, name: string) => {
    if (blockedEntry == null) return;
    const remaining = (blockedEntry.blockReasons ?? [])
      .filter((r) => (reasonId != null ? r.id !== reasonId : r.name !== name))
      .map((r) => r.id)
      .filter((v): v is number => v != null);
    try {
      await updateBlacklist(blockedEntry.id, remaining, []);
      invalidate();
      await blacklistQuery.refetch();
    } catch {
      alert("카테고리 삭제 실패.");
    }
  };

  const addBlockCategory = async (tag: TagItem) => {
    if (blockedEntry == null) return;
    const ids = (blockedEntry.blockReasons ?? [])
      .map((r) => r.id)
      .filter((v): v is number => v != null);
    if (tag.id != null && !ids.includes(tag.id)) ids.push(tag.id);
    try {
      await updateBlacklist(blockedEntry.id, ids, tag.id == null ? [tag.name] : []);
      invalidate();
      await blacklistQuery.refetch();
    } catch {
      alert("카테고리 추가 실패.");
    }
  };

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
      <div
        className={`absolute inset-0 bg-black/40 transition-opacity duration-300 ${visible ? "opacity-100" : "opacity-0"}`}
        onClick={close}
      />
      <aside
        className={`absolute right-0 top-0 flex h-full w-full max-w-[560px] flex-col bg-white shadow-xl transition-transform duration-300 ease-out ${visible ? "translate-x-0" : "translate-x-full"}`}
      >
        <header className="flex items-center gap-2 border-b px-4 py-3">
          <h2 className="min-w-0 flex-1 truncate text-base font-semibold">{title}</h2>
          {blocked ? (
            <span className="rounded p-1.5 text-slate-300" title="차단된 회사는 북마크 불가">
              <Bookmark size={18} />
            </span>
          ) : (
            <button
              className="rounded p-1.5 hover:bg-gray-100"
              title={bookmarked ? "북마크 해제" : "북마크"}
              onClick={() => bookmarkMutation.mutate(!bookmarked)}
            >
              {bookmarked ? <BookmarkCheck size={18} className="text-amber-500" /> : <Bookmark size={18} className="text-gray-400" />}
            </button>
          )}
          <button
            className={`rounded p-1.5 hover:bg-gray-100 ${blocked ? "text-red-600" : "text-gray-400"}`}
            title={blocked ? "차단 해제" : "차단하기"}
            onClick={onBlockToggle}
          >
            <Ban size={18} />
          </button>
          {!isCreate && (
            <button className="rounded p-1.5 text-gray-400 hover:bg-gray-100" title=".md 내보내기" onClick={exportMd}>
              <Download size={18} />
            </button>
          )}
          <button className="rounded p-1.5 text-gray-500 hover:bg-gray-100" onClick={close} title="닫기(Esc)">
            <X size={18} />
          </button>
        </header>

        <div className="flex gap-1 border-b px-4 pt-2 text-sm">
          {(["view", "edit"] as const).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`border-b-2 px-3 py-2 ${tab === t ? "border-blue-600 font-semibold text-blue-700" : "border-transparent text-gray-500"}`}
            >
              {t === "view" ? "보기" : "편집"}
            </button>
          ))}
        </div>

        <div className="flex-1 overflow-y-auto px-4 py-3">
          {detailQuery.isLoading && <p className="text-sm text-gray-500">불러오는 중…</p>}
          {detailQuery.isError && <p className="text-sm text-red-600">상세 조회 실패.</p>}

          {(detail || isCreate) && tab === "view" && (
            <>
              {detail?.averageScore != null && (
                <section className="mb-3 rounded-lg bg-gray-50 p-3 text-sm">
                  <div className="mb-1 flex items-center gap-1">
                    <span className="text-gray-500">내 별점</span>
                    {detail.myStars == null ? (
                      <span className="text-xs text-gray-400">미지정</span>
                    ) : (
                      <span className="inline-flex items-center gap-0.5">
                        {[1, 2, 3, 4, 5].map((i) => (
                          <Star key={i} size={14} className={i <= (detail.myStars ?? 0) ? "fill-amber-400 text-amber-400" : "text-gray-300"} />
                        ))}
                      </span>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-x-3 gap-y-1">
                    <span className="text-xs text-gray-600">
                      크롤링 평균 <b className="text-sm text-gray-900">{detail.averageScore}</b>
                    </span>
                    {scoreText("잡플래닛", detail.jobplanetScore)}
                    {scoreText("잡코리아", detail.jobkoreaScore)}
                    {scoreText("사람인", detail.saraminScore)}
                  </div>
                </section>
              )}
              {detail?.myStars != null && detail?.averageScore == null && (
                <section className="mb-3 rounded-lg bg-gray-50 p-3 text-sm">
                  <span className="text-gray-500">내 별점</span>{" "}
                  <span className="inline-flex items-center gap-0.5">
                    {[1, 2, 3, 4, 5].map((i) => (
                      <Star key={i} size={14} className={i <= (detail.myStars ?? 0) ? "fill-amber-400 text-amber-400" : "text-gray-300"} />
                    ))}
                  </span>
                </section>
              )}
              {blocked && (
                <section className="mb-3 rounded-lg bg-red-50 p-3 text-sm">
                  <p className="text-xs font-semibold text-red-600">⛔ 차단된 회사</p>
                  <p className="mt-1 font-mono text-xs text-slate-600" title="차단 매칭 키워드(정규화명)">
                    차단 키워드: {detail?.companyNameNormalized ?? blockedEntry?.companyNameNormalized ?? companyNormalized ?? "-"}
                  </p>
                  {(blockedEntry?.blockReasons?.length ?? 0) > 0 && (
                    <div className="mt-1.5 flex flex-wrap gap-1">
                      {blockedEntry!.blockReasons!.map((r) => (
                        <span key={r.id} className="rounded-full bg-red-100 px-1.5 py-0.5 text-[11px] text-red-700">
                          {r.name}
                        </span>
                      ))}
                    </div>
                  )}
                  {(detail?.noteMd == null || !detail.noteMd.trim()) && (detail?.myStars == null) && (
                    <p className="mt-1 text-[11px] text-slate-400">메모·별점이 없습니다. 편집 탭에서 작성하세요.</p>
                  )}
                </section>
              )}
              {!blocked && (detail?.categories?.length ?? 0) > 0 && (
                <div className="mb-3 flex flex-wrap gap-1">
                  {(detail?.categories ?? []).map((c) => (
                    <span key={c.id} className="rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-600">
                      {c.name}
                    </span>
                  ))}
                </div>
              )}
              <article className="md-view">
                {detail?.noteMd?.trim() ? <ReactMarkdown>{detail.noteMd}</ReactMarkdown> : <p className="text-sm text-gray-400">작성된 분석 메모가 없습니다. 편집 탭에서 작성하세요.</p>}
              </article>
              {!isCreate && (
              <section className="mt-3">
                <p className="mb-1 text-xs font-semibold text-gray-500">관련 공고 (최근 저장분)</p>
                {postingsQuery.isLoading && (
                  <div className="space-y-1.5" aria-label="관련 공고 불러오는 중">
                    {[0, 1, 2].map((i) => (
                      <div key={i} className="animate-pulse rounded border border-slate-200 px-2.5 py-2">
                        <div className="mb-1 h-3.5 w-3/4 rounded bg-slate-200" />
                        <div className="h-3 w-1/3 rounded bg-slate-100" />
                      </div>
                    ))}
                  </div>
                )}
                {!postingsQuery.isLoading && (postingsQuery.data?.length ?? 0) === 0 && (
                  <p className="text-sm text-gray-400">저장된 관련 공고가 없습니다.</p>
                )}
                {(postingsQuery.data?.length ?? 0) > 0 && (
                  <ul className="max-h-64 divide-y divide-slate-100 overflow-y-auto rounded border border-slate-200">
                    {postingsQuery.data!.map((p, i) => (
                      <li key={`${p.url}-${i}`} className="px-2.5 py-1.5 text-sm">
                        <a
                          href={p.url}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="block truncate font-medium text-slate-800 hover:text-blue-600 hover:underline"
                          title={p.position}
                        >
                          {p.position}
                        </a>
                        <span className="text-xs text-slate-500">
                          {p.siteName} · {p.crawledAt}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
              )}
            </>
          )}

          {tab === "edit" && (
            <>
              {blockedEntry && (
                <div className="mb-3 rounded-lg bg-red-50 p-3 text-sm">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs font-semibold text-red-600">⛔ 차단됨</span>
                    <span className="font-mono text-xs text-slate-600" title="차단 매칭 키워드(정규화명)">
                      차단 키워드: {blockedEntry.companyNameNormalized}
                    </span>
                  </div>
                  <div className="mt-2">
                    <p className="mb-1 text-[11px] font-semibold text-slate-500">차단 카테고리</p>
                    <TagInput
                      tags={(blockedEntry.blockReasons ?? []).map((r) => ({ id: r.id, name: r.name }))}
                      onAdd={(t) => void addBlockCategory(t)}
                      onRemove={(t) => void removeCategory(t.id, t.name)}
                      searchFn={searchBlockCategories}
                      placeholder="카테고리 추가 (입력 후 Enter)"
                    />
                  </div>
                </div>
              )}
              {!blockedEntry && (
                <div className="mb-3">
                  <p className="mb-1 text-xs font-semibold text-gray-500">메모 태그 (북마크 포함 모든 메모에 부여 가능)</p>
                  <TagInput
                    tags={noteTags}
                    onAdd={(t) => setNoteTags((prev) => (prev.some((p) => p.name === t.name) ? prev : [...prev, t]))}
                    onRemove={(t) => setNoteTags((prev) => prev.filter((p) => p.name !== t.name))}
                    searchFn={searchBlockCategories}
                    placeholder="태그 추가 (입력 후 Enter, 저장 버튼으로 확정)"
                  />
                </div>
              )}
              <div className="mb-3 flex items-center gap-1">
                <span className="mr-1 text-sm text-gray-600">내 별점</span>
                {[1, 2, 3, 4, 5].map((i) => (
                  <button key={i} onClick={() => setStars(i)} title={`${i}점`}>
                    <Star size={20} className={stars != null && i <= stars ? "fill-amber-400 text-amber-400" : "text-gray-300 hover:text-amber-200"} />
                  </button>
                ))}
                {stars != null && (
                  <button className="ml-1 text-xs text-gray-400 underline" onClick={() => setStars(null)}>
                    지우기
                  </button>
                )}
              </div>
              {bookmarked && stars == null && (
                <p className="mb-2 text-xs text-amber-600">⭐ 북마크한 회사입니다. 별점을 함께 지정해 보세요 (별 설정 시 북마크 유지).</p>
              )}
              <textarea
                className="h-56 w-full rounded border p-2 font-mono text-sm"
                placeholder="마크다운으로 분석 메모 작성 (## 제목, - 목록, **굵게** …)"
                value={md}
                onChange={(e) => setMd(e.target.value)}
              />
              <p className="mb-1 mt-3 text-xs font-semibold text-gray-500">미리보기</p>
              <article className="md-view mb-3 min-h-20 rounded border bg-gray-50 p-2">
                {md.trim() ? <ReactMarkdown>{md}</ReactMarkdown> : <span className="text-sm text-gray-400">내용 없음</span>}
              </article>
              <button
                className="inline-flex items-center gap-1 rounded bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
                disabled={saveMutation.isPending}
                onClick={() => saveMutation.mutate()}
              >
                <Save size={15} /> 저장
              </button>
              {!isCreate && (
                <button
                  className="ml-2 inline-flex items-center gap-1 rounded border border-red-200 px-3 py-2 text-sm text-red-600 hover:bg-red-50"
                  onClick={() => {
                    if (confirm("이 회사 메모를 삭제할까요? (차단·평점은 유지됩니다)")) removeMutation.mutate();
                  }}
                >
                  <Trash2 size={15} /> 삭제
                </button>
              )}
            </>
          )}
        </div>
      </aside>
      <BlockConfirmDialog
        open={blockCreateOpen}
        company={detail?.companyNameDisplay ?? companyName ?? ""}
        title="회사 차단"
        confirmLabel="차단"
        editableCompany
        onCancel={() => setBlockCreateOpen(false)}
        onConfirm={(reason, reasonIds, categoryNames, keyword) => {
          void confirmBlockCreate(reason, reasonIds, categoryNames, keyword);
        }}
      />
    </div>
  );
}
