import { useEffect, useState } from "react";
import { apiDelete, apiGet, apiPost, apiPut } from "../api/client";
import type { CareerItem } from "../types/resume";

const inputCls =
  "w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500";

interface FormState {
  title: string;
  startDate: string;
  endDate: string;
  description: string;
}

const EMPTY: FormState = { title: "", startDate: "", endDate: "", description: "" };

/**
 * 경력(회사) 내 기간별 상세 항목 편집기.
 * 임베디드 경력기술서 양식의 "근무기간 × 업무"를 관리한다.
 */
export default function CareerItemsEditor({
  careerId,
  documentId,
  onChanged,
}: {
  careerId: number;
  documentId?: number;
  onChanged: () => void;
}) {
  const [items, setItems] = useState<CareerItem[]>([]);
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>(EMPTY);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const endpoint = `/careers/${careerId}/items`;
  const docParams = documentId ? { documentId: String(documentId) } : undefined;

  const load = async () => {
    try {
      const res = await apiGet<CareerItem[]>(endpoint, docParams);
      setItems(Array.isArray(res) ? res : []);
    } catch {
      setItems([]);
    }
  };

  useEffect(() => {
    if (!open) return;
    void load();
  }, [open]);

  const startEdit = (it?: CareerItem) => {
    setForm(
      it
        ? {
            title: it.title ?? "",
            startDate: it.startDate ?? "",
            endDate: it.endDate ?? "",
            description: it.description ?? "",
          }
        : EMPTY,
    );
    setEditing(it ? String(it.id) : "new");
    setError(null);
  };

  const save = async () => {
    if (!form.title.trim()) {
      setError("항목 제목은 필수입니다.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const payload = {
        title: form.title.trim(),
        startDate: form.startDate.trim() === "" ? null : form.startDate,
        endDate: form.endDate.trim() === "" ? null : form.endDate,
        description: form.description.trim() === "" ? null : form.description,
        displayOrder: 0,
      };
      if (editing === "new") await apiPost(endpoint, payload, docParams);
      else await apiPut(`${endpoint}/${editing}`, payload, docParams);
      setEditing(null);
      onChanged();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const remove = async (id: number) => {
    if (!(await window.confirm("이 항목을 삭제할까요?"))) return;
    setBusy(true);
    setError(null);
    try {
      await apiDelete(`${endpoint}/${id}`, docParams);
      onChanged();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const reorder = async (id: number, dir: -1 | 1) => {
    const idx = items.findIndex((x) => x.id === id);
    const target = idx + dir;
    if (idx < 0 || target < 0 || target >= items.length) return;
    const next = [...items];
    const cur = next[idx];
    const other = next[target];
    next[idx] = other;
    next[target] = cur;
    setBusy(true);
    setError(null);
    try {
      await apiPut(`${endpoint}/reorder`, { ids: next.map((x) => x.id) }, docParams);
      onChanged();
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "순서 저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const renderForm = () => (
    <div className="mt-3 border border-gray-200 rounded-lg p-4 bg-gray-50">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        <div className="md:col-span-2">
          <label className="block text-xs font-medium text-slate-600 mb-1">
            항목 제목
            <span className="text-red-500 ml-0.5">*</span>
          </label>
          <input
            value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })}
            placeholder="예: 채용공고 수집·파싱 API 개발"
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">시작일</label>
          <input
            type="date"
            value={form.startDate}
            onChange={(e) => setForm({ ...form, startDate: e.target.value })}
            className={inputCls}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-slate-600 mb-1">종료일</label>
          <input
            type="date"
            value={form.endDate}
            onChange={(e) => setForm({ ...form, endDate: e.target.value })}
            placeholder="진행 중이면 비움"
            className={inputCls}
          />
        </div>
        <div className="md:col-span-2">
          <label className="block text-xs font-medium text-slate-600 mb-1">
            상세 내용
          </label>
          <textarea
            value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            rows={3}
            placeholder="무엇을, 어떻게, 어떤 성과를 냈는지"
            className={inputCls}
          />
        </div>
      </div>
      <div className="mt-3 flex gap-2 justify-end">
        <button
          onClick={() => setEditing(null)}
          className="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white hover:bg-gray-50"
        >
          취소
        </button>
        <button
          onClick={() => void save()}
          disabled={busy}
          className="px-3 py-1.5 text-sm bg-slate-900 text-white rounded hover:bg-slate-700 disabled:opacity-50"
        >
          {busy ? "저장 중..." : "저장"}
        </button>
      </div>
    </div>
  );

  return (
    <div className="ml-6 border-l-2 border-gray-100 pl-3">
      <div className="flex items-center justify-between mb-1">
        <button
          onClick={() => setOpen((v) => !v)}
          className="text-xs text-slate-500 hover:text-slate-800 font-medium"
        >
          {open ? "▾ 기간별 상세 항목" : `▸ 기간별 상세 항목 (${items.length})`}
        </button>
        {open && !editing && (
          <button
            onClick={() => startEdit()}
            className="px-2 py-0.5 text-xs bg-slate-900 text-white rounded hover:bg-slate-700"
          >
            + 항목 추가
          </button>
        )}
      </div>

      {open && (
        <div className="space-y-1.5">
          {error && (
            <p className="text-xs text-red-600 bg-red-50 border border-red-200 rounded px-2 py-1">
              {error}
            </p>
          )}
          {editing === "new" && renderForm()}
          {items.map((it, i) => (
            <div
              key={it.id}
              className="rounded border border-gray-100 bg-white px-2 py-1.5"
            >
              <div className="flex items-start gap-1.5">
                <div className="shrink-0 flex flex-col leading-none">
                  <button
                    onClick={() => void reorder(it.id, -1)}
                    disabled={i === 0 || busy}
                    title="위로"
                    className="text-[10px] text-slate-400 hover:text-slate-700 disabled:opacity-30"
                  >
                    ▲
                  </button>
                  <button
                    onClick={() => void reorder(it.id, 1)}
                    disabled={i === items.length - 1 || busy}
                    title="아래로"
                    className="text-[10px] text-slate-400 hover:text-slate-700 disabled:opacity-30"
                  >
                    ▼
                  </button>
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-800 truncate">
                    {it.title}
                    {it.startDate && (
                      <span className="ml-2 text-xs font-normal text-slate-400">
                        {it.startDate.replace(/-/g, ".")}
                        {it.endDate ? ` ~ ${it.endDate.replace(/-/g, ".")}` : " ~"}
                      </span>
                    )}
                  </p>
                  {it.description && (
                    <p className="text-xs text-slate-500 line-clamp-2 whitespace-pre-line">
                      {it.description}
                    </p>
                  )}
                </div>
                {editing !== String(it.id) && (
                  <div className="shrink-0 flex gap-1">
                    <button
                      onClick={() => startEdit(it)}
                      className="px-1.5 py-0.5 text-xs border border-gray-300 rounded hover:bg-gray-50"
                    >
                      수정
                    </button>
                    <button
                      onClick={() => void remove(it.id)}
                      disabled={busy}
                      className="px-1.5 py-0.5 text-xs border border-red-200 text-red-600 rounded hover:bg-red-50 disabled:opacity-50"
                    >
                      삭제
                    </button>
                  </div>
                )}
              </div>
              {editing === String(it.id) && renderForm()}
            </div>
          ))}
          {items.length === 0 && !editing && (
            <p className="text-xs text-slate-400 px-1">
              등록된 상세 항목이 없습니다. 경력기술서의 근무기간별 업무를 문서화하세요.
            </p>
          )}
        </div>
      )}
    </div>
  );
}