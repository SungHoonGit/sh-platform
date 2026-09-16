import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import ReactMarkdown from "react-markdown";
import { X, Star, Bookmark, BookmarkCheck, Ban, Download, Save } from "lucide-react";
import { companyNoteApi, type CompanyNoteDetail } from "../api/companies";
import { fetchBlacklist, addBlacklist, removeBlacklist } from "../api/scraper";

interface Props {
  /** 메모 ID (있으면 상세 조회, 없으면 companyName으로 신규 작성) */
  id: number | null;
  companyName?: string;
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

export default function CompanySlideOver({ id, companyName, onClose, onSaved }: Props) {
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

  const [stars, setStars] = useState<number | null>(null);
  const [md, setMd] = useState("");
  const [bookmarked, setBookmarked] = useState(false);

  useEffect(() => {
    if (detail) {
      setStars(detail.myStars);
      setMd(detail.noteMd ?? "");
      setBookmarked(detail.bookmarked);
      setTab(detail.noteMd?.trim() ? "view" : "edit");
    } else if (isCreate) {
      setStars(null);
      setMd("");
      setBookmarked(false);
      setTab("edit");
    }
  }, [detail?.id, isCreate, companyName]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === "Escape" && onClose();
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["company-notes"] });
    queryClient.invalidateQueries({ queryKey: ["company-note"] });
    queryClient.invalidateQueries({ queryKey: ["blacklist"] });
  };

  const saveMutation = useMutation({
    mutationFn: () =>
      isCreate
        ? companyNoteApi.upsert({ companyName, myStars: stars, isBookmarked: bookmarked, noteMd: md })
        : companyNoteApi.update(id as number, { myStars: stars, isBookmarked: bookmarked, noteMd: md }),
    onSuccess: (saved) => {
      invalidate();
      setTab("view");
      onSaved?.(saved.id);
    },
    onError: () => alert("저장 실패."),
  });

  const bookmarkMutation = useMutation({
    mutationFn: (next: boolean) => {
      if (isCreate) return companyNoteApi.upsert({ companyName, isBookmarked: next });
      return companyNoteApi.update(id as number, { isBookmarked: next });
    },
    onSuccess: (saved) => {
      setBookmarked(saved.bookmarked);
      invalidate();
      if (isCreate) onSaved?.(saved.id);
    },
    onError: () => alert("북마크 변경 실패."),
  });

  const blockMutation = useMutation({
    mutationFn: async (): Promise<boolean> => {
      const list = blacklistQuery.data ?? (await fetchBlacklist());
      const normalized = detail?.companyNameNormalized;
      const existing = normalized ? list.find((b) => b.companyNameNormalized === normalized) : undefined;
      if (existing) {
        await removeBlacklist(existing.id);
        return false;
      }
      await addBlacklist(detail?.companyNameDisplay ?? companyName ?? "");
      return true;
    },
    onSuccess: async (nowBlocked) => {
      // 차단+북마크 상호배타: 차단 시 북마크 자동 해제
      if (nowBlocked && id != null && detail?.bookmarked) {
        await companyNoteApi.update(id, { isBookmarked: false }).catch(() => undefined);
      }
      invalidate();
      detailQuery.refetch();
    },
    onError: () => alert("차단 변경 실패."),
  });

  const removeMutation = useMutation({
    mutationFn: () => companyNoteApi.remove(id as number),
    onSuccess: () => {
      invalidate();
      onClose();
    },
    onError: () => alert("삭제 실패."),
  });

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
  const blocked = detail?.blocked ?? false;

  return (
    <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <aside className="absolute right-0 top-0 flex h-full w-full max-w-[560px] flex-col bg-white shadow-xl">
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
          {!isCreate && (
            <>
              <button
                className={`rounded p-1.5 hover:bg-gray-100 ${blocked ? "text-red-600" : "text-gray-400"}`}
                title={blocked ? "차단 해제" : "차단하기"}
                onClick={() => blockMutation.mutate()}
              >
                <Ban size={18} />
              </button>
              <button className="rounded p-1.5 text-gray-400 hover:bg-gray-100" title=".md 내보내기" onClick={exportMd}>
                <Download size={18} />
              </button>
            </>
          )}
          <button className="rounded p-1.5 text-gray-500 hover:bg-gray-100" onClick={onClose} title="닫기(Esc)">
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

          {(detail || isCreate) && tab === "view" && detail && (
            <>
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
                    크롤링 평균 <b className="text-sm text-gray-900">{detail.averageScore ?? "-"}</b>
                  </span>
                  {scoreText("잡플래닛", detail.jobplanetScore)}
                  {scoreText("잡코리아", detail.jobkoreaScore)}
                  {scoreText("사람인", detail.saraminScore)}
                </div>
                {blocked && <p className="mt-1 text-xs font-semibold text-red-600">⛔ 차단된 회사</p>}
              </section>
              <article className="md-view">
                {detail.noteMd?.trim() ? <ReactMarkdown>{detail.noteMd}</ReactMarkdown> : <p className="text-sm text-gray-400">작성된 분석 메모가 없습니다. 편집 탭에서 작성하세요.</p>}
              </article>
            </>
          )}

          {tab === "edit" && (
            <>
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
                  className="ml-2 text-xs text-red-500 underline"
                  onClick={() => {
                    if (confirm("이 회사 메모를 삭제할까요? (차단·평점은 유지됩니다)")) removeMutation.mutate();
                  }}
                >
                  메모 삭제
                </button>
              )}
            </>
          )}
        </div>
      </aside>
    </div>
  );
}
