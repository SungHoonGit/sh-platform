import { useState } from "react";
import { apiDelete, apiPost, apiPut, apiUpload } from "../api/client";

export interface FieldDef {
  key: string;
  label: string;
  type?: "text" | "date" | "textarea" | "select" | "file";
  options?: string[];
  accept?: string;
  required?: boolean;
  placeholder?: string;
  showIf?: { key: string; equals: string };
}

type Item = Record<string, unknown>;
type FormState = Record<string, string>;

const inputCls =
  "w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500";

export default function CrudSection({
  title,
  endpoint,
  items,
  fields,
  titleKey,
  subtitleKeys = [],
  fixedPayload = {},
  onChanged,
}: {
  title: string;
  endpoint: string;
  items: Item[];
  fields: FieldDef[];
  titleKey: string;
  subtitleKeys?: string[];
  fixedPayload?: Record<string, unknown>;
  onChanged: () => void;
}) {
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>({});
  const [fileNames, setFileNames] = useState<Record<string, string>>({});
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const visible = (f: FieldDef) => !f.showIf || form[f.showIf.key] === f.showIf.equals;

  const scrollToForm = () =>
    setTimeout(
      () =>
        document
          .getElementById(`crud-form-${title}`)
          ?.scrollIntoView({ behavior: "smooth", block: "center" }),
      60,
    );

  const openNew = () => {
    setForm(Object.fromEntries(fields.map((f) => [f.key, ""])));
    setFileNames({});
    setEditing("new");
    setError(null);
    scrollToForm();
  };

  const openEdit = (it: Item) => {
    setForm(
      Object.fromEntries(
        fields.map((f) => [f.key, it[f.key] == null ? "" : String(it[f.key])]),
      ),
    );
    setFileNames(
      Object.fromEntries(
        fields
          .filter((f) => f.type === "file" && it[f.key] != null && String(it[f.key]) !== "")
          .map((f) => [f.key, "저장된 파일 있음"]),
      ),
    );
    setEditing(String(it.id));
    setError(null);
    scrollToForm();
  };

  const save = async () => {
    const missing = fields.filter(visible).find((f) => f.required && !form[f.key]?.trim());
    if (missing) {
      setError(`${missing.label}은(는) 필수입니다.`);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const payload: Record<string, unknown> = { ...fixedPayload };
      for (const f of fields) {
        if (!visible(f)) {
          payload[f.key] = null;
        } else {
          payload[f.key] = form[f.key]?.trim() === "" ? null : form[f.key];
        }
      }
      if (editing === "new") await apiPost(endpoint, payload);
      else await apiPut(`${endpoint}/${editing}`, payload);
      setEditing(null);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "저장에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const uploadFile = async (key: string, file: File) => {
    setUploading(true);
    setError(null);
    try {
      const res = await apiUpload<{ id: number; originalName: string }>("/files", file);
      setForm((prev) => ({ ...prev, [key]: `/api/v1/files/${res.id}/download` }));
      setFileNames((prev) => ({ ...prev, [key]: res.originalName }));
    } catch (e) {
      setError(e instanceof Error ? e.message : "업로드에 실패했습니다.");
    } finally {
      setUploading(false);
    }
  };

  const remove = async (id: number) => {
    if (!window.confirm("이 항목을 삭제할까요?")) return;
    setBusy(true);
    setError(null);
    try {
      await apiDelete(`${endpoint}/${id}`);
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "삭제에 실패했습니다.");
    } finally {
      setBusy(false);
    }
  };

  const showForm = editing !== null;

  return (
    <section className="mb-6 bg-white rounded-xl border border-slate-200 p-5">
      <div className="flex justify-between items-center mb-3">
        <h2 className="font-bold text-slate-800">
          {title}
          <span className="ml-2 text-xs font-normal text-slate-400">{items.length}개</span>
        </h2>
        {!showForm && (
          <button
            onClick={openNew}
            className="px-2.5 py-1 text-sm bg-slate-900 text-white rounded hover:bg-slate-700"
          >
            + 추가
          </button>
        )}
      </div>

      {error && (
        <p className="mb-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded px-3 py-2">{error}</p>
      )}

      {items.length > 0 && (
        <ul className="divide-y divide-gray-100">
          {items.map((it) => (
            <li key={String(it.id)} className="py-2.5 flex justify-between items-start gap-3">
              <div className="min-w-0">
                <p className="font-semibold text-sm text-slate-800 truncate">
                  {String(it[titleKey] ?? "")}
                </p>
                {subtitleKeys.map((k) =>
                  it[k] != null && String(it[k]) !== "" ? (
                    <p key={k} className="text-xs text-slate-500 truncate">
                      {String(it[k])}
                    </p>
                  ) : null,
                )}
              </div>
              {editing !== String(it.id) && (
                <div className="shrink-0 flex gap-1.5">
                  <button
                    onClick={() => openEdit(it)}
                    className="px-2 py-1 text-xs border border-gray-300 rounded hover:bg-gray-50"
                  >
                    수정
                  </button>
                  <button
                    onClick={() => remove(Number(it.id))}
                    disabled={busy}
                    className="px-2 py-1 text-xs border border-red-200 text-red-600 rounded hover:bg-red-50 disabled:opacity-50"
                  >
                    삭제
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      {items.length === 0 && !showForm && (
        <p className="text-sm text-slate-400">등록된 항목이 없습니다.</p>
      )}

      {showForm && (
        <div id={`crud-form-${title}`} className="mt-3 border border-gray-200 rounded-lg p-4 bg-gray-50">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {fields.filter(visible).map((f) => (
              <div
                key={f.key}
                className={f.type === "textarea" || f.type === "file" ? "md:col-span-2" : ""}
              >
                <label className="block text-xs font-medium text-slate-600 mb-1">
                  {f.label}
                  {f.required && <span className="text-red-500 ml-0.5">*</span>}
                </label>
                {f.type === "textarea" ? (
                  <textarea
                    value={form[f.key] ?? ""}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    placeholder={f.placeholder}
                    rows={4}
                    className={inputCls}
                  />
                ) : f.type === "select" ? (
                  <select
                    value={form[f.key] ?? ""}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    className={inputCls}
                  >
                    {(f.options ?? []).map((o) => (
                      <option key={o} value={o}>
                        {o}
                      </option>
                    ))}
                  </select>
                ) : f.type === "file" ? (
                  <div className="flex items-center gap-2">
                    <input
                      type="file"
                      accept={f.accept}
                      disabled={uploading}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        if (file) void uploadFile(f.key, file);
                        e.target.value = "";
                      }}
                      className="text-sm text-slate-600 file:mr-2 file:px-2.5 file:py-1.5 file:text-xs file:border-0 file:bg-slate-900 file:text-white file:rounded hover:file:bg-slate-700"
                    />
                    {fileNames[f.key] && (
                      <span className="text-xs text-green-700 truncate">
                        {uploading ? "업로드 중..." : `✓ ${fileNames[f.key]}`}
                      </span>
                    )}
                  </div>
                ) : (
                  <input
                    type={f.type === "date" ? "date" : "text"}
                    value={form[f.key] ?? ""}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    placeholder={f.placeholder}
                    className={inputCls}
                  />
                )}
              </div>
            ))}
          </div>
          <div className="mt-4 flex gap-2 justify-end">
            <button
              onClick={() => setEditing(null)}
              className="px-3 py-1.5 text-sm border border-gray-300 rounded bg-white hover:bg-gray-50"
            >
              취소
            </button>
            <button
              onClick={save}
              disabled={busy}
              className="px-3 py-1.5 text-sm bg-slate-900 text-white rounded hover:bg-slate-700 disabled:opacity-50"
            >
              {busy ? "저장 중..." : "저장"}
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
