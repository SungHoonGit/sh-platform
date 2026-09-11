import { useEffect, useRef, useState } from "react";
import { apiDelete, apiDownload, apiGet, apiPost, apiPut, apiUpload, fileDownloadPath } from "../api/client";

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
    | "major"
    | "skill";
  options?: string[];
  accept?: string;
  required?: boolean;
  placeholder?: string;
  showIf?: { key: string; equals: string };
  /** check 타입: 체크 시 이 key들을 비우고 잠근다 */
  disablesOnCheck?: string[];
  /** file 타입이 이미지(jpg/png)면 업로드 후 미리보기를 표시 */
  image?: boolean;
}

type Item = Record<string, unknown>;
type FormState = Record<string, string>;

const inputCls =
  "w-full border border-gray-300 rounded px-2.5 py-1.5 text-sm focus:outline-none focus:border-gray-500";

function ImageThumb({
  path,
  className,
}: {
  path: string | undefined;
  className: string;
}) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    if (!path) return;
    const token = localStorage.getItem("accessToken");
    if (!token) return;
    let url: string | null = null;
    fetch(`/resume${fileDownloadPath(path)}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => (res.ok ? res.blob() : Promise.reject(new Error(String(res.status)))))
      .then((blob) => {
        url = URL.createObjectURL(blob);
        setSrc(url);
      })
      .catch(() => setSrc(null));
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [path]);

  if (!src) return null;
  return <img src={src} alt="미리보기" className={className} />;
}

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
  inline = false,
  sectionDragActive = false,
  dragHandle,
  importOptions = null,
  renderRowExtra,
  rowToggle,
  onChanged,
}: {
  title: string;
  endpoint: string;
  items: Item[];
  fields: FieldDef[];
  titleKey: string;
  subtitleKeys?: string[];
  fixedPayload?: Record<string, unknown>;
  /** inline: 각 목록 행 자체가 편집 폼을 여는 방식 (자기소개 등) */
  inline?: boolean;
  /** 상위(EditPage)에서 섹션 카드 드래그가 진행 중일 때, 항목 드래그 표시를 비활성화 */
  sectionDragActive?: boolean;
  /** 카드 헤더 전체를 드래그 핸들로 만들 때 (섹션 카드 순서 이동) */
  dragHandle?: {
    active: boolean;
    onDragStart: (e: React.DragEvent<HTMLDivElement>) => void;
    onDragEnd: (e: React.DragEvent<HTMLDivElement>) => void;
  };
  /** 폼 상단에 특정 작업물(포트폴리오)에서 필드를 가져올 수 있는 셀렉트를 표시 */
  importOptions?: {
    items: Item[];
    /** form 필드 key → 작업물 필드 key */
    fieldMap: Record<string, string>;
  } | null;
  /** 각 행 아래 추가 UI를 그린다 (예: 경력의 기간별 상세 항목 편집기) */
  renderRowExtra?: (it: Item) => React.ReactNode;
  /** 항목별 보임/숨김 토글 (이력서 문서 설정). true면 이 이력서에서 표시됨. */
  rowToggle?: {
    visible: (it: Item) => boolean;
    onToggle: (it: Item) => void;
  };
  onChanged: () => void;
}) {
  const [editing, setEditing] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>({});
  const [fileNames, setFileNames] = useState<Record<string, string>>({});
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [dragId, setDragId] = useState<string | null>(null);
  const [overId, setOverId] = useState<string | null>(null);
  const [dropPos, setDropPos] = useState<"before" | "after" | null>(null);
  const [schoolOpen, setSchoolOpen] = useState(false);
  const [schoolQuery, setSchoolQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Sug[]>([]);
  const suggestField = useRef<string | null>(null);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [importVal, setImportVal] = useState("");

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
        type === "skill"
          ? `/reference/skills/search?q=${encodeURIComponent(query)}`
          : type === undefined
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
    setImportVal("");
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
    setImportVal("");
    setError(null);
    scrollToForm();
  };

  const applyImport = () => {
    if (!importOptions || importVal === "") return;
    const src = importOptions.items[Number(importVal)];
    if (!src) return;
    const next: FormState = { ...form };
    for (const [formKey, srcKey] of Object.entries(importOptions.fieldMap)) {
      const v = src[srcKey];
      next[formKey] = v == null ? "" : String(v);
    }
    setForm(next);
    setImportVal("");
    setError(null);
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

  const downloadFile = (path: string) => {
    void apiDownload(fileDownloadPath(path), "첨부파일").catch((e) =>
      setError(e instanceof Error && e.message === "UNAUTHORIZED" ? "로그인이 필요합니다." : "첨부파일 다운로드에 실패했습니다."),
    );
  };

  const fileFields = fields.filter((f) => f.type === "file");

  const remove = async (id: number) => {
    if (!(await window.confirm("이 항목을 삭제할까요?"))) return;
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

  const persistOrder = async (next: Item[]) => {
    setBusy(true);
    setError(null);
    try {
      await apiPut(`${endpoint}/reorder`, { ids: next.map((it) => Number(it.id)) });
      onChanged();
    } catch (e) {
      setError(e instanceof Error ? e.message : "순서 저장에 실패했습니다.");
    } finally {
      setBusy(false);
      setDragId(null);
      setOverId(null);
      setDropPos(null);
    }
  };

  const moveItem = (it: Item, dir: -1 | 1) => {
    const idx = items.findIndex((x) => String(x.id) === String(it.id));
    const target = idx + dir;
    if (idx < 0 || target < 0 || target >= items.length) return;
    const next = [...items];
    const cur = next[idx];
    const other = next[target];
    next[idx] = other;
    next[target] = cur;
    void persistOrder(next);
  };

  const reorderItems = (draggedId: string, targetId: string, pos: "before" | "after") => {
    const from = items.findIndex((x) => String(x.id) === draggedId);
    if (from < 0) return;
    const rest = items.filter((x) => String(x.id) !== draggedId);
    const targetIndex = rest.findIndex((x) => String(x.id) === targetId);
    if (targetIndex < 0) return;
    const dragged = items[from];
    const insertAt = pos === "after" ? targetIndex + 1 : targetIndex;
    rest.splice(insertAt, 0, dragged);
    setDragId(null);
    setOverId(null);
    setDropPos(null);
    void persistOrder(rest);
  };

  const showForm = editing !== null;

  const renderForm = () => (
    <div id={`crud-form-${title}`} className="mt-3 border border-gray-200 rounded-lg p-4 bg-gray-50">
      {importOptions && importOptions.items.length > 0 && (
        <div className="mb-3 flex flex-wrap items-end gap-2">
          <div className="flex-1 min-w-[220px]">
            <label className="block text-xs font-medium text-slate-600 mb-1">
              작업물에서 가져오기
            </label>
            <select
              value={importVal}
              onChange={(e) => setImportVal(e.target.value)}
              className={inputCls}
            >
              <option value="">-- 작업물 선택 --</option>
              {importOptions.items.map((it, i) => (
                <option key={String(it.id ?? i)} value={String(i)}>
                  {String(it.title ?? "작업물")}
                </option>
              ))}
            </select>
          </div>
          <button
            onClick={applyImport}
            disabled={importVal === ""}
            className="px-3 py-1.5 text-sm bg-slate-900 text-white rounded hover:bg-slate-700 disabled:opacity-50"
          >
            필드 채우기
          </button>
        </div>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {fields.filter(visible).map((f) => (
          <div
            key={f.key}
            className={f.type === "textarea" || f.type === "file" || f.type === "school" || f.type === "major" || f.type === "skill" ? "md:col-span-2" : ""}
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
              <div>
                <div className="flex items-center gap-2">
                  <input
                    type="file"
                    accept={f.accept}
                    disabled={uploading}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      if (!file) return;
                      if (f.image && file.size > 5 * 1024 * 1024) {
                        setError("썸네일 이미지는 5MB 이하만 가능합니다.");
                        return;
                      }
                      void uploadFile(f.key, file);
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
                {f.image && form[f.key] && (
                  <div className="mt-2">
                    <ImageThumb
                      key={form[f.key]}
                      path={form[f.key]}
                      className="max-h-32 rounded border border-gray-200"
                    />
                  </div>
                )}
              </div>
            ) : f.type === "school" || f.type === "major" || f.type === "skill" ? (
              <div className="relative">
                <input
                  type="text"
                  value={form[f.key] ?? ""}
                  placeholder={
                    f.placeholder ??
                    (f.type === "school"
                      ? "학교명 입력 후 선택"
                      : f.type === "major"
                        ? "전공명 입력 후 선택"
                        : "기술명 입력 후 선택 (쉼표로 여러 개)")
                  }
                  onChange={(e) => {
                    const v = e.target.value;
                    setForm((prev) => ({ ...prev, [f.key]: v }));
                    setSchoolQuery(v);
                    suggestField.current = f.key;
                    fetchSuggestions(
                      v,
                      f.type === "school"
                        ? form.schoolType || undefined
                        : f.type === "skill"
                          ? "skill"
                          : undefined,
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
                {schoolOpen && suggestField.current === f.key && f.type === "skill" && (
                  <ul className="absolute z-20 mt-1 w-full max-h-48 overflow-auto bg-white border border-gray-200 rounded shadow-lg">
                    {suggestions.map((s) => (
                      <li key={s.name}>
                        <button
                          type="button"
                          className="w-full px-2.5 py-1.5 text-sm text-left hover:bg-gray-50 flex justify-between items-center"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => {
                            const cur = (form[f.key] ?? "").trim();
                            const parts = cur
                              .split(",")
                              .map((p) => p.trim())
                              .filter((p) => p !== "");
                            parts[parts.length - 1] = s.name;
                            setForm((prev) => ({ ...prev, [f.key]: parts.join(", ") }));
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
                {schoolOpen && suggestField.current === f.key && f.type !== "skill" && (
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
  );

  const orderControls = (it: Item, i: number) => (
    <div className="shrink-0 flex items-center gap-0.5">
      <div className="flex flex-col" title="드래그나 ▲▼로 순서 변경">
        <button
          onClick={() => moveItem(it, -1)}
          disabled={i === 0 || busy}
          title="위로"
          className="text-[10px] text-slate-400 hover:text-slate-700 disabled:opacity-30 disabled:hover:text-slate-400 leading-none py-0.5"
        >
          ▲
        </button>
        <button
          onClick={() => moveItem(it, 1)}
          disabled={i === items.length - 1 || busy}
          title="아래로"
          className="text-[10px] text-slate-400 hover:text-slate-700 disabled:opacity-30 disabled:hover:text-slate-400 leading-none py-0.5"
        >
          ▼
        </button>
      </div>
      <span
        draggable
        onDragStart={() => {
          setDragId(String(it.id));
          setOverId(null);
          setDropPos(null);
        }}
        onDragEnd={() => {
          setDragId(null);
          setOverId(null);
          setDropPos(null);
        }}
        className="px-1 text-slate-300 hover:text-slate-500 cursor-grab active:cursor-grabbing select-none"
        title="드래그로 순서 변경"
      >
        ⋮⋮
      </span>
    </div>
  );

  const rowDragProps = (it: Item) => ({
    onDragOver: (e: React.DragEvent) => {
      if (sectionDragActive) return;
      e.preventDefault();
      const rect = e.currentTarget.getBoundingClientRect();
      const pos: "before" | "after" =
        e.clientY < rect.top + rect.height / 2 ? "before" : "after";
      setOverId(String(it.id));
      setDropPos(pos);
    },
    onDrop: (e: React.DragEvent) => {
      if (sectionDragActive) return;
      e.preventDefault();
      if (dragId && dropPos) reorderItems(dragId, String(it.id), dropPos);
    },
  });

  const isOverRow = (it: Item) =>
    overId === String(it.id) &&
    (dropPos === "after" ? "border-b-2 border-blue-400" : "border-t-2 border-blue-400");

  const fileKeys = new Set(fileFields.map((f) => f.key));

  const toggleButton = (it: Item) =>
    rowToggle ? (
      <button
        onClick={() => rowToggle.onToggle(it)}
        title={rowToggle.visible(it) ? "이 이력서에서 숨기기" : "이 이력서에서 표시하기"}
        className={`px-2 py-1 text-xs rounded whitespace-nowrap ${
          rowToggle.visible(it)
            ? "bg-green-50 text-green-600 hover:bg-green-100"
            : "bg-gray-100 text-gray-400 hover:bg-gray-200"
        }`}
      >
        {rowToggle.visible(it) ? "보임" : "숨김"}
      </button>
    ) : null;

  const renderRowInfo = (it: Item) => (
    <div className="min-w-0 flex-1">
      <p className="font-semibold text-sm text-slate-800 truncate">
        {String(it[titleKey] ?? "")}
      </p>
      {subtitleKeys.map((k) =>
        it[k] != null && String(it[k]) !== "" && !fileKeys.has(k) ? (
          <p key={k} className="text-xs text-slate-500 truncate">
            {String(it[k])}
          </p>
        ) : null,
      )}
      {fileFields
        .filter((f) => !f.image)
        .map((f) => {
          const v = it[f.key];
        if (v == null || String(v) === "") return null;
        return (
          <button
            key={f.key}
            onClick={(e) => {
              e.stopPropagation();
              downloadFile(String(v));
            }}
            className="mt-1 text-xs text-blue-700 border border-blue-200 bg-blue-50 rounded px-2 py-0.5 hover:bg-blue-100"
            title={f.label}
          >
            {f.label} 다운로드
          </button>
        );
      })}
    </div>
  );

  const itemList = (
    <div className="space-y-2">
      {inline && editing === "new" && renderForm()}
      {items.map((it, i) => (
        <div
          key={String(it.id)}
          {...rowDragProps(it)}
          className={
            overId === String(it.id)
              ? `rounded-lg border ${isOverRow(it)}`
              : "rounded-lg border border-gray-100"
          }
        >
          <div className="py-2.5 px-1 flex justify-between items-start gap-2">
            {orderControls(it, i)}
            {renderRowInfo(it)}
            {editing !== String(it.id) && (
              <div className="shrink-0 flex gap-1.5">
                {toggleButton(it)}
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
          </div>
          {renderRowExtra?.(it)}
          {inline && editing === String(it.id) && renderForm()}
        </div>
      ))}
    </div>
  );

  return (
    <section className="mb-6 bg-white rounded-xl border border-slate-200 p-5">
      <div
        draggable={!!dragHandle}
        onDragStart={dragHandle?.onDragStart}
        onDragEnd={dragHandle?.onDragEnd}
        className={`flex justify-between items-center mb-3 ${
          dragHandle
            ? dragHandle.active
              ? "cursor-grabbing select-none"
              : "cursor-grab active:cursor-grabbing select-none hover:bg-slate-50 rounded px-1 -mx-1"
            : ""
        }`}
        title={dragHandle ? "이 헤더를 드래그하여 섹션 순서 변경" : undefined}
      >
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

      {inline ? (
        <>
          {itemList}
          {items.length === 0 && !showForm && (
            <p className="text-sm text-slate-400">등록된 항목이 없습니다.</p>
          )}
        </>
      ) : (
        <>
          {items.length > 0 && (
            <ul className="divide-y divide-gray-100">
              {items.map((it, i) => (
                <li
                  key={String(it.id)}
                  {...rowDragProps(it)}
                  className={`py-2.5 flex flex-col gap-2 ${
                    overId === String(it.id) ? isOverRow(it) : ""
                  }`}
                >
                  <div className="flex justify-between items-start gap-2">
                    {orderControls(it, i)}
                    {renderRowInfo(it)}
                    {editing !== String(it.id) && (
                      <div className="shrink-0 flex gap-1.5">
                        {toggleButton(it)}
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
                  </div>
                  {renderRowExtra?.(it)}
                </li>
              ))}
            </ul>
          )}
          {items.length === 0 && !showForm && (
            <p className="text-sm text-slate-400">등록된 항목이 없습니다.</p>
          )}
          {showForm && renderForm()}
        </>
      )}
    </section>
  );
}
