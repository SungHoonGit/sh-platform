import { useEffect, useRef, useState } from "react";
import { apiDelete, apiGet, apiPost, apiPut, apiUpload } from "../api/client";

export interface FieldDef {
  key: string;
  label: string;
  type?:
    | "text"
    | "date"
    | "textarea"
    | "select"
    | "file"
    | "check"
    | "school"
    | "major";
  options?: string[];
  accept?: string;
  required?: boolean;
  placeholder?: string;
  showIf?: { key: string; equals: string };
  /** check 타입: 체크 시 이 key들을 비우고 잠근다 */
  disablesOnCheck?: string[];
}

type Item = Record<string, unknown>;
type FormState = Record<string, string>;

const inputCls =
  "w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500";

interface Sug {
  name: string;
  type?: string;
}

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
  const [schoolOpen, setSchoolOpen] = useState(false);
  const [schoolQuery, setSchoolQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Sug[]>([]);
  const suggestField = useRef<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, []);

  const fetchSuggestions = (query: string, type?: string) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(async () => {
      if (!query.trim()) {
        setSuggestions([]);
        return;
      }
      const path =
        type === undefined
          ? `/reference/majors/search?q=${encodeURIComponent(query)}`
          : `/reference/schools/search?q=${encodeURIComponent(query)}&schoolType=${encodeURIComponent(type ?? "")}`;
      try {
        const res = await apiGet<Sug[]>(path);
        setSuggestions(Array.isArray(res) ? res : []);
      } catch {
        setSuggestions([]);
      }
    }, 200);
  };

  const visible = (f: FieldDef) => !f.showIf || form[f.showIf.key] === f.showIf.equals;

  const enabled = (f: FieldDef) =>
    !fields.some(
      (c) =>
        c.type === "check" &&
        c.disablesOnCheck?.includes(f.key) &&
        form[c.key] === "true",
    );

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
    // check 필드: 대상 값이 비어 있으면 체크된 상태로 초기화 (예: 퇴사일 없음 = 재직 중)
    const derived = Object.fromEntries(
      fields
        .filter((f) => f.type === "check" && f.disablesOnCheck?.length)
        .map((f) => {
          const v = (it as Item)[f.disablesOnCheck![0]];
          return [f.key, v == null || String(v) === "" ? "true" : ""];
        }),
    );
    setForm((prev) => ({ ...prev, ...derived }));
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
        if (f.type === "check") {
          if (form[f.key] === "true") {
            f.disablesOnCheck?.forEach((k) => (payload[k] = null));
          }
          continue;
        }
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
                className={f.type === "textarea" || f.type === "file" || f.type === "school" || f.type === "major" ? "md:col-span-2" : ""}
              >
                {f.type !== "check" && (
                  <label className="block text-xs font-medium text-slate-600 mb-1">
                    {f.label}
                    {f.required && <span className="text-red-500 ml-0.5">*</span>}
                  </label>
                )}
                {f.type === "check" ? (
                  <label className="flex items-center gap-1.5 mt-1">
                    <input
                      type="checkbox"
                      checked={form[f.key] === "true"}
                      onChange={(e) => setForm({ ...form, [f.key]: e.target.checked ? "true" : "" })}
                      className="w-4 h-4 accent-slate-900"
                    />
                    <span className="text-sm text-slate-700">{f.label}</span>
                  </label>
                ) : f.type === "textarea" ? (
                  <textarea
                    value={form[f.key] ?? ""}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    placeholder={f.placeholder}
                    rows={4}
                    disabled={!enabled(f)}
                    className={`${inputCls} ${!enabled(f) ? "bg-gray-100" : ""}`}
                  />
                ) : f.type === "select" ? (
                  <select
                    value={form[f.key] ?? ""}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    disabled={!enabled(f)}
                    className={`${inputCls} ${!enabled(f) ? "bg-gray-100" : ""}`}
                  >
                    {(f.options ?? []).length > 0 && <option value="">-- 선택 --</option>}
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
                ) : f.type === "school" || f.type === "major" ? (
                  <div className="relative">
                    <input
                      type="text"
                      value={form[f.key] ?? ""}
                      placeholder={f.placeholder ?? (f.type === "school" ? "학교명 입력 후 선택" : "전공명 입력 후 선택")}
                      onChange={(e) => {
                        const v = e.target.value;
                        setForm((prev) => ({ ...prev, [f.key]: v }));
                        setSchoolQuery(v);
                        suggestField.current = f.key;
                        fetchSuggestions(
                          v,
                          f.type === "school" ? (form.schoolType || undefined) : undefined,
                        );
                        setSchoolOpen(true);
                      }}
                      onFocus={() => {
                        suggestField.current = f.key;
                        setSchoolOpen(true);
                      }}
                      onBlur={() => setTimeout(() => setSchoolOpen(false), 150)}
                      className={inputCls}
                    />
                    {schoolOpen && suggestField.current === f.key && (
                      <ul className="absolute z-20 mt-1 w-full max-h-48 overflow-auto bg-white border border-gray-200 rounded shadow-lg">
                        {suggestions.map((s) => (
                          <li key={s.name}>
                            <button
                              type="button"
                              className="w-full px-2.5 py-1.5 text-sm text-left hover:bg-gray-50 flex justify-between items-center"
                              onMouseDown={(e) => e.preventDefault()}
                              onClick={() => {
                                setForm((prev) => {
                                  const next: FormState = { ...prev, [f.key]: s.name };
                                  if (f.type === "school" && s.type) {
                                    next.schoolType = s.type;
                                  }
                                  return next;
                                });
                                setSchoolQuery("");
                                setSuggestions([]);
                                setSchoolOpen(false);
                              }}
                            >
                              <span>{s.name}</span>
                              {s.type && (
                                <span className="text-xs text-gray-400 shrink-0">{s.type}</span>
                              )}
                            </button>
                          </li>
                        ))}
                        {suggestions.length === 0 && (
                          <li className="px-2.5 py-1.5 text-xs text-gray-400">
                            "{schoolQuery}" 를 직접 입력해 저장할 수 있습니다
                          </li>
                        )}
                      </ul>
                    )}
                  </div>
                ) : (
                  <input
                    type={f.type === "date" ? "date" : "text"}
                    value={form[f.key] ?? ""}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                    placeholder={f.placeholder}
                    disabled={!enabled(f)}
                    className={`${inputCls} ${!enabled(f) ? "bg-gray-100" : ""}`}
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
