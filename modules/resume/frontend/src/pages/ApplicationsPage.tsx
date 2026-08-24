import { useCallback, useEffect, useState } from "react";
import { apiDelete, apiGet, apiPost, apiPut, logout } from "../api/client";

interface Application {
  id: number;
  postingId: number | null;
  companyName: string;
  postingTitle: string;
  postingUrl: string | null;
  applyChannel: string;
  appliedAt: string | null;
  status: string;
  documentId: number | null;
  memo: string | null;
}

interface ScrapItem {
  postingId: number;
  company: string;
  position: string;
  url: string | null;
  siteName: string | null;
}

const STATUS_ORDER = ["PREPARING", "APPLIED", "SCREEN_PASSED", "INTERVIEW", "OFFER", "REJECTED"] as const;

const STATUS_LABELS: Record<string, string> = {
  PREPARING: "준비 중",
  APPLIED: "지원 완료",
  SCREEN_PASSED: "서류 통과",
  INTERVIEW: "면접",
  OFFER: "오퍼",
  REJECTED: "불합격",
};

const STATUS_COLORS: Record<string, string> = {
  PREPARING: "bg-slate-100 text-slate-600",
  APPLIED: "bg-blue-100 text-blue-700",
  SCREEN_PASSED: "bg-indigo-100 text-indigo-700",
  INTERVIEW: "bg-violet-100 text-violet-700",
  OFFER: "bg-emerald-100 text-emerald-700",
  REJECTED: "bg-red-100 text-red-600",
};

const CHANNEL_LABELS: Record<string, string> = {
  PLATFORM: "플랫폼",
  LINK: "링크",
  EMAIL: "이메일",
  ETC: "기타",
};

const emptyForm = {
  companyName: "",
  postingTitle: "",
  postingUrl: "",
  applyChannel: "LINK",
  appliedAt: new Date().toISOString().slice(0, 10),
  status: "APPLIED",
  memo: "",
  documentId: null as number | null,
  postingId: null as number | null,
};

async function fetchScraper<T>(path: string): Promise<T> {
  const token = localStorage.getItem("accessToken");
  const res = await fetch(`/scraper${path}`, {
    headers: { Authorization: `Bearer ${token ?? ""}` },
  });
  if (res.status === 401) {
    logout();
    throw new Error("인증 만료");
  }
  if (!res.ok) throw new Error(await res.text());
  return res.json();
}

export default function ApplicationsPage() {
  const [items, setItems] = useState<Application[]>([]);
  const [documents, setDocuments] = useState<{ id: number; title: string }[]>([]);
  const [filter, setFilter] = useState<string>("ALL");
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<typeof emptyForm>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [scraps, setScraps] = useState<ScrapItem[]>([]);
  const [scrapError, setScrapError] = useState<string | null>(null);

  const load = useCallback(() => {
    apiGet<Application[]>("/applications")
      .then(setItems)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)));
    apiGet<{ id: number; title: string }[]>("/documents")
      .then(setDocuments)
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    load();
    fetchScraper<{ scraps: ScrapItem[] }>("/job-scrap")
      .then((json) => setScraps(json.scraps))
      .catch(() => setScrapError("스크랩 목록을 불러올 수 없습니다"));
  }, [load]);

  useEffect(() => {
    const raw = sessionStorage.getItem("applicationPrefill");
    if (!raw) return;
    sessionStorage.removeItem("applicationPrefill");
    try {
      const p = JSON.parse(raw) as {
        companyName?: string;
        postingTitle?: string;
        postingUrl?: string;
        postingId?: number;
      };
      setEditingId(null);
      setForm((prev) => ({
        ...prev,
        companyName: p.companyName ?? "",
        postingTitle: p.postingTitle ?? "",
        postingUrl: p.postingUrl ?? "",
        postingId: p.postingId ?? null,
        applyChannel: "PLATFORM",
        status: "APPLIED",
      }));
      setShowForm(true);
    } catch {
      /* 무시 */
    }
  }, []);

  const counts: Record<string, number> = { ALL: items.length };
  for (const s of STATUS_ORDER) counts[s] = items.filter((a) => a.status === s).length;
  const visible = filter === "ALL" ? items : items.filter((a) => a.status === filter);

  const openCreate = () => {
    setEditingId(null);
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (a: Application) => {
    setEditingId(a.id);
    setForm({
      companyName: a.companyName,
      postingTitle: a.postingTitle,
      postingUrl: a.postingUrl ?? "",
      applyChannel: a.applyChannel,
      appliedAt: a.appliedAt ?? new Date().toISOString().slice(0, 10),
      status: a.status,
      memo: a.memo ?? "",
      documentId: a.documentId,
      postingId: a.postingId,
    });
    setShowForm(true);
  };

  const submit = async () => {
    if (!form.companyName.trim() || !form.postingTitle.trim()) {
      setError("회사명과 공고 제목은 필수입니다.");
      return;
    }
    try {
      if (editingId) {
        await apiPut(`/applications/${editingId}`, form);
      } else {
        await apiPost("/applications", form);
      }
      setShowForm(false);
      setError(null);
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm("이 지원 기록을 삭제할까요?")) return;
    try {
      await apiDelete(`/applications/${id}`);
      load();
    } catch {
      setError("삭제에 실패했습니다.");
    }
  };

  const changeStatus = async (a: Application, status: string) => {
    try {
      await apiPut(`/applications/${a.id}`, {
        companyName: a.companyName,
        postingTitle: a.postingTitle,
        postingUrl: a.postingUrl ?? undefined,
        applyChannel: a.applyChannel,
        appliedAt: a.appliedAt ?? undefined,
        status,
        documentId: a.documentId,
        postingId: a.postingId,
        memo: a.memo ?? undefined,
      });
      load();
    } catch {
      setError("상태 변경에 실패했습니다.");
    }
  };

  const importFromScrap = (postingIdStr: string) => {
    const scrap = scraps.find((s) => String(s.postingId) === postingIdStr);
    if (!scrap) return;
    setForm((prev) => ({
      ...prev,
      companyName: scrap.company,
      postingTitle: scrap.position,
      postingUrl: scrap.url ?? "",
      applyChannel: "PLATFORM",
      status: "APPLIED",
      postingId: scrap.postingId,
    }));
  };

  const field = "border border-gray-300 rounded px-2 py-1.5 text-sm focus:outline-none focus:border-gray-500 w-full";
  const labelCls = "text-[11px] font-semibold text-slate-500 uppercase tracking-wide";

  return (
    <div className="max-w-5xl mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-bold text-slate-800">지원 관리</h1>
        <button
          onClick={openCreate}
          className="bg-slate-900 text-white rounded-lg px-3 py-1.5 text-sm font-semibold hover:bg-slate-800 transition-colors"
        >
          + 새 지원 등록
        </button>
      </div>

      {error && <p className="mb-3 text-sm text-red-600">{error}</p>}

      <div className="flex gap-1.5 mb-4 flex-wrap">
        {["ALL", ...STATUS_ORDER].map((s) => (
          <button
            key={s}
            onClick={() => setFilter(s)}
            className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
              filter === s
                ? "bg-slate-900 text-white"
                : "bg-white border border-slate-200 text-slate-600 hover:border-slate-400"
            }`}
          >
            {s === "ALL" ? "전체" : STATUS_LABELS[s]} ({counts[s] ?? 0})
          </button>
        ))}
      </div>

      {visible.length === 0 ? (
        <div className="text-center py-16 bg-white rounded-xl border border-gray-200">
          <p className="text-slate-500">아직 지원 기록이 없습니다.</p>
          <button onClick={openCreate} className="mt-3 text-sm font-semibold text-slate-900 underline underline-offset-4">
            첫 지원 등록하기
          </button>
        </div>
      ) : (
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-gray-200 text-left">
              <tr>
                <th className="px-3 py-2 font-semibold text-slate-600">회사 / 공고</th>
                <th className="px-2 py-2 font-semibold text-slate-600 w-[90px]">경로</th>
                <th className="px-2 py-2 font-semibold text-slate-600 w-[110px]">지원일</th>
                <th className="px-2 py-2 font-semibold text-slate-600 w-[120px]">상태</th>
                <th className="px-2 py-2 font-semibold text-slate-600 w-[130px]">이력서</th>
                <th className="px-2 py-2 w-[90px]"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {visible.map((a) => (
                <tr key={a.id} className="hover:bg-gray-50/60">
                  <td className="px-3 py-2.5">
                    <div className="font-medium text-slate-800">{a.companyName}</div>
                    <div className="text-xs text-slate-500 truncate max-w-md">
                      {a.postingUrl ? (
                        <a href={a.postingUrl} target="_blank" rel="noopener noreferrer" className="hover:text-blue-600 hover:underline" onClick={(e) => e.stopPropagation()}>
                          {a.postingTitle}
                        </a>
                      ) : (
                        a.postingTitle
                      )}
                    </div>
                    {a.memo && <div className="text-xs text-slate-400 mt-0.5 line-clamp-1">{a.memo}</div>}
                  </td>
                  <td className="px-2 py-2.5 text-slate-600 text-xs">{CHANNEL_LABELS[a.applyChannel] ?? a.applyChannel}</td>
                  <td className="px-2 py-2.5 text-slate-600 text-xs whitespace-nowrap">{a.appliedAt ?? "-"}</td>
                  <td className="px-2 py-2.5">
                    <select
                      value={a.status}
                      onChange={(e) => void changeStatus(a, e.target.value)}
                      className={`rounded-full px-2 py-1 text-xs font-medium cursor-pointer focus:outline-none ${STATUS_COLORS[a.status] ?? "bg-slate-100"}`}
                    >
                      {STATUS_ORDER.map((s) => (
                        <option key={s} value={s}>
                          {STATUS_LABELS[s]}
                        </option>
                      ))}
                    </select>
                  </td>
                  <td className="px-2 py-2.5 text-xs text-slate-500 truncate max-w-[120px]">
                    {a.documentId
                      ? documents.find((d) => d.id === a.documentId)?.title ?? `#${a.documentId}`
                      : "-"}
                  </td>
                  <td className="px-2 py-2.5 text-right whitespace-nowrap">
                    <button onClick={() => openEdit(a)} className="text-xs text-slate-500 hover:text-slate-900 mr-2">
                      수정
                    </button>
                    <button onClick={() => void remove(a.id)} className="text-xs text-slate-400 hover:text-red-600">
                      삭제
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" onClick={() => setShowForm(false)}>
          <div className="bg-white rounded-xl shadow-xl w-full max-w-lg p-5 space-y-3" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-base font-bold text-slate-800 mb-2">
              {editingId ? "지원 수정" : "새 지원 등록"}
            </h2>

            {!editingId && (
              <div>
                <label className={labelCls}>스크랩에서 불러오기</label>
                {scrapError ? (
                  <p className="text-xs text-slate-400 mt-1">{scrapError}</p>
                ) : scraps.length === 0 ? (
                  <p className="text-xs text-slate-400 mt-1">스크랩한 공고가 없습니다. 스크래퍼에서 ☆ 버튼으로 공고를 저장해 보세요.</p>
                ) : (
                  <select
                    onChange={(e) => importFromScrap(e.target.value)}
                    value=""
                    className={`${field} mt-1`}
                  >
                    <option value="">— 스크랩 공고 선택 —</option>
                    {scraps.map((s) => (
                      <option key={s.postingId} value={String(s.postingId)}>
                        [{s.siteName}] {s.company} — {s.position}
                      </option>
                    ))}
                  </select>
                )}
              </div>
            )}

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className={labelCls}>회사명 *</label>
                <input className={`${field} mt-1`} value={form.companyName}
                  onChange={(e) => setForm({ ...form, companyName: e.target.value })} />
              </div>
              <div>
                <label className={labelCls}>공고 제목 *</label>
                <input className={`${field} mt-1`} value={form.postingTitle}
                  onChange={(e) => setForm({ ...form, postingTitle: e.target.value })} />
              </div>
              <div className="col-span-2">
                <label className={labelCls}>공고 URL</label>
                <input className={`${field} mt-1`} value={form.postingUrl}
                  onChange={(e) => setForm({ ...form, postingUrl: e.target.value })} placeholder="https://" />
              </div>
              <div>
                <label className={labelCls}>지원 경로</label>
                <select className={`${field} mt-1`} value={form.applyChannel}
                  onChange={(e) => setForm({ ...form, applyChannel: e.target.value })}>
                  {Object.entries(CHANNEL_LABELS).map(([k, v]) => (
                    <option key={k} value={k}>{v}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className={labelCls}>지원일</label>
                <input type="date" className={`${field} mt-1`} value={form.appliedAt}
                  onChange={(e) => setForm({ ...form, appliedAt: e.target.value })} />
              </div>
              <div>
                <label className={labelCls}>진행 상태</label>
                <select className={`${field} mt-1`} value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}>
                  {STATUS_ORDER.map((s) => (
                    <option key={s} value={s}>{STATUS_LABELS[s]}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className={labelCls}>사용 이력서</label>
                <select className={`${field} mt-1`}
                  value={form.documentId ?? ""}
                  onChange={(e) => setForm({ ...form, documentId: e.target.value ? Number(e.target.value) : null })}>
                  <option value="">— 미선택 —</option>
                  {documents.map((d) => (
                    <option key={d.id} value={d.id}>{d.title}</option>
                  ))}
                </select>
              </div>
              <div className="col-span-2">
                <label className={labelCls}>메모</label>
                <textarea rows={2} className={`${field} mt-1`} value={form.memo}
                  onChange={(e) => setForm({ ...form, memo: e.target.value })} />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-1">
              <button onClick={() => setShowForm(false)}
                className="px-4 py-1.5 text-sm rounded-lg border border-gray-300 text-slate-600 hover:bg-gray-50">
                취소
              </button>
              <button onClick={() => void submit()}
                className="px-4 py-1.5 text-sm font-semibold rounded-lg bg-slate-900 text-white hover:bg-slate-800">
                {editingId ? "저장" : "등록"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
