import { useEffect, useRef, useState } from "react";

export interface BlockConfirmDialogProps {
  open: boolean;
  company: string;
  onCancel: () => void;
  onConfirm: (reason: string, reasonIds: number[], categoryNames: string[]) => void;
}

interface Suggestion {
  id: number;
  name: string;
}

interface Tag {
  id?: number;
  name: string;
}

/**
 * 회사 차단 확인용 공용 다이얼로그.
 * 카테고리는 자유 입력 + 드롭다운 제안 방식(Notion 태그 스타일)이다.
 * 입력 중 기존 카테고리가 검색 제안으로 나오고, 선택/Enter로 태그에 추가(복수 허용).
 * 기존에 없는 새 입력은 백엔드가 마스터로 승격시켜(categoryNames) 다음부터 제안된다.
 *
 * @param open 열림 여부
 * @param company 차단할 회사명
 * @param onCancel 취소 콜백
 * @param onConfirm 차단 확정 콜백 (자유메모, 선택한 기존 카테고리 id 목록, 신규 입력 카테고리명 목록)
 */
export default function BlockConfirmDialog({ open, company, onCancel, onConfirm }: BlockConfirmDialogProps) {
  const [tags, setTags] = useState<Tag[]>([]);
  const [input, setInput] = useState("");
  const [suggestions, setSuggestions] = useState<Suggestion[]>([]);
  const [showSuggest, setShowSuggest] = useState(false);
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<number | null>(null);
  const inFlightRef = useRef(0);

  const searchReasons = async (q: string): Promise<Suggestion[]> => {
    const token = localStorage.getItem("accessToken") ?? "";
    const res = await fetch(`/scraper/company-blacklist/reasons/search?q=${encodeURIComponent(q)}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) return [];
    const json = await res.json();
    return (json.data ?? json) as Suggestion[];
  };

  useEffect(() => {
    if (!open) {
      setTags([]);
      setInput("");
      setReason("");
      setSuggestions([]);
      setShowSuggest(false);
      return;
    }
    const q = input.trim();
    if (!q) {
      setSuggestions([]);
      setShowSuggest(false);
      return;
    }
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(async () => {
      const seq = ++inFlightRef.current;
      setLoading(true);
      try {
        const list = await searchReasons(q);
        if (seq === inFlightRef.current) setSuggestions(list);
      } finally {
        if (seq === inFlightRef.current) setLoading(false);
      }
    }, 250);
    return () => {
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
    };
  }, [input, open]);

  useEffect(() => {
    if (!open) { inFlightRef.current++; return; }
  }, [open]);

  if (!open) return null;

  const addTag = (tag: Tag) => {
    setTags((prev) => (prev.some((t) => t.name === tag.name) ? prev : [...prev, tag]));
    setInput("");
    setShowSuggest(false);
  };

  const removeTag = (name: string) => {
    setTags((prev) => prev.filter((t) => t.name !== name));
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    const q = input.trim();
    if (e.key === "Enter") {
      e.preventDefault();
      if (q) {
        const exact = suggestions.find((s) => s.name.toLowerCase() === q.toLowerCase());
        addTag(exact ? { id: exact.id, name: exact.name } : { name: q });
      } else {
        handleConfirm();
      }
    } else if (e.key === "Backspace" && !q && tags.length > 0) {
      removeTag(tags[tags.length - 1].name);
    } else if (e.key === "Escape") {
      setShowSuggest(false);
    }
  };

  const handleConfirm = () => {
    const existingIds = tags.filter((t) => t.id != null).map((t) => t.id as number);
    const newNames = tags.filter((t) => t.id == null).map((t) => t.name);
    onConfirm(reason.trim(), existingIds, newNames);
    setReason("");
    setTags([]);
  };

  const handleCancel = () => {
    setReason("");
    setTags([]);
    setInput("");
    setShowSuggest(false);
    onCancel();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/40" onClick={handleCancel} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
          <p className="text-sm font-semibold text-slate-700">회사 차단</p>
          <button onClick={handleCancel} className="text-slate-400 hover:text-slate-600 text-sm">✕</button>
        </div>
        <div className="px-4 py-4">
          <p className="text-sm text-slate-700">
            <span className="font-medium">{company}</span> 회사의 공고를 숨길까요?
          </p>
          <p className="text-xs text-slate-400 mt-1 mb-3">카테고리를 입력하고 Enter. 이전에 쓴 항목이 추천으로 나옵니다.</p>

          <div className="relative">
            <div className="flex flex-wrap items-center gap-1.5 border border-slate-300 rounded-lg px-2 py-1.5 focus-within:ring-2 focus-within:ring-blue-500">
              {tags.map((t) => (
                <span key={t.name} className="inline-flex items-center gap-1 bg-blue-50 text-blue-700 text-xs rounded-full px-2 py-0.5">
                  {t.name}
                  <button type="button" onClick={() => removeTag(t.name)} className="text-blue-400 hover:text-blue-700">✕</button>
                </span>
              ))}
              <input
                value={input}
                onChange={(e) => { setInput(e.target.value); setShowSuggest(true); }}
                onKeyDown={handleKeyDown}
                onBlur={() => window.setTimeout(() => setShowSuggest(false), 150)}
                placeholder={tags.length === 0 ? "예: 스타트업" : "추가 카테고리"}
                autoFocus
                className="flex-1 min-w-[80px] text-sm focus:outline-none"
              />
            </div>
            {loading && <span className="absolute right-3 top-2.5 text-xs text-slate-400">…</span>}
            {showSuggest && suggestions.length > 0 && (
              <ul className="absolute z-10 mt-1 w-full max-h-44 overflow-auto rounded-lg border border-slate-200 bg-white shadow-lg">
                {suggestions.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      className="w-full text-left px-3 py-2 text-sm text-slate-700 hover:bg-blue-50"
                      onMouseDown={(e) => { e.preventDefault(); addTag({ id: s.id, name: s.name }); }}
                    >
                      {s.name}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="mt-3">
            <p className="text-xs text-slate-500 mb-1">메모(선택)</p>
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="간단한 메모를 남길 수 있어요"
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>
        <div className="flex items-center justify-end gap-2 px-4 pb-4 border-t border-slate-100 pt-3">
          <button onClick={handleCancel} className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors">취소</button>
          <button onClick={handleConfirm} className="px-3 py-1.5 text-sm font-medium bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition-colors">차단</button>
        </div>
      </div>
    </div>
  );
}
