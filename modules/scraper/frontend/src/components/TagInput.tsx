import { useEffect, useRef, useState } from "react";
import { X } from "lucide-react";

export interface TagItem {
  id?: number;
  name: string;
}

interface Props {
  tags: TagItem[];
  onAdd: (tag: TagItem) => void;
  onRemove: (tag: TagItem) => void;
  searchFn: (q: string) => Promise<{ id: number; name: string }[]>;
  placeholder?: string;
}

/**
 * Notion 태그 스타일 인라인 편집기. 입력 중 마스터 검색 제안, Enter/선택으로 추가, x로 제거.
 * 저장 시점은 부모가 결정한다 (즉시 API 호출 또는 로컬 상태 후 일괄 저장).
 */
export default function TagInput({ tags, onAdd, onRemove, searchFn, placeholder }: Props) {
  const [input, setInput] = useState("");
  const [suggestions, setSuggestions] = useState<{ id: number; name: string }[]>([]);
  const [showSuggest, setShowSuggest] = useState(false);
  const debounceRef = useRef<number | null>(null);

  useEffect(() => {
    const q = input.trim();
    if (!q) {
      setSuggestions([]);
      setShowSuggest(false);
      return;
    }
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(async () => {
      try {
        const list = await searchFn(q);
        setSuggestions(list.filter((s) => !tags.some((t) => t.name === s.name)));
        setShowSuggest(true);
      } catch {
        setSuggestions([]);
      }
    }, 250);
    return () => {
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
    };
  }, [input, searchFn, tags]);

  const addTag = (tag: TagItem) => {
    if (!tag.name.trim() || tags.some((t) => t.name === tag.name)) return;
    onAdd({ ...tag, name: tag.name.trim() });
    setInput("");
    setShowSuggest(false);
  };

  return (
    <div className="relative">
      <div className="flex flex-wrap items-center gap-1.5 rounded-lg border border-slate-300 px-2 py-1.5 focus-within:ring-2 focus-within:ring-blue-500">
        {tags.map((t) => (
          <span key={t.id ?? t.name} className="inline-flex items-center gap-1 rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">
            {t.name}
            <button type="button" onClick={() => onRemove(t)} className="text-blue-400 hover:text-red-600" title={`${t.name} 제거`}>
              <X size={12} />
            </button>
          </span>
        ))}
        <input
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            setShowSuggest(true);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              const q = input.trim();
              if (!q) return;
              const exact = suggestions.find((s) => s.name.toLowerCase() === q.toLowerCase());
              addTag(exact ? { id: exact.id, name: exact.name } : { name: q });
            } else if (e.key === "Escape") {
              setShowSuggest(false);
            }
          }}
          onBlur={() => window.setTimeout(() => setShowSuggest(false), 150)}
          placeholder={placeholder ?? "입력 후 Enter"}
          className="min-w-[80px] flex-1 text-sm focus:outline-none"
        />
      </div>
      {showSuggest && suggestions.length > 0 && (
        <ul className="absolute z-10 mt-1 max-h-44 w-full overflow-auto rounded-lg border border-slate-200 bg-white shadow-lg">
          {suggestions.map((s) => (
            <li key={s.id}>
              <button
                type="button"
                className="w-full px-3 py-2 text-left text-sm text-slate-700 hover:bg-blue-50"
                onMouseDown={(e) => {
                  e.preventDefault();
                  addTag({ id: s.id, name: s.name });
                }}
              >
                {s.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
