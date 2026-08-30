import { useEffect, useRef, useState } from "react";

export interface BlockConfirmDialogProps {
  open: boolean;
  company: string;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}

interface ReasonOption {
  id: number;
  name: string;
}

/**
 * 회사 차단 확인용 공용 다이얼로그 (사유 자동완성 + 입력).
 * 사유는 DB 마스터(block_reasons)에서 검색되어 자동완성으로 제시된다.
 *
 * @param open 열림 여부
 * @param company 차단할 회사명
 * @param onCancel 취소 콜백
 * @param onConfirm 차단 확정 콜백 (사유 전달)
 */
export default function BlockConfirmDialog({ open, company, onCancel, onConfirm }: BlockConfirmDialogProps) {
  const [reason, setReason] = useState("");
  const [suggestions, setSuggestions] = useState<ReasonOption[]>([]);
  const [showList, setShowList] = useState(false);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<number | null>(null);
  const inFlightRef = useRef(0);

  const searchReasons = async (q: string) => {
    const token = localStorage.getItem("accessToken") ?? "";
    const res = await fetch(`/scraper/company-blacklist/reasons/search?q=${encodeURIComponent(q)}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) return [];
    const json = await res.json();
    return (json.data ?? json) as ReasonOption[];
  };

  useEffect(() => {
    if (!open) {
      setReason("");
      setSuggestions([]);
      setShowList(false);
      return;
    }
    const trimmed = reason.trim();
    if (!trimmed) {
      setShowList(false);
      setSuggestions([]);
      return;
    }
    if (debounceRef.current) window.clearTimeout(debounceRef.current);
    debounceRef.current = window.setTimeout(async () => {
      const seq = ++inFlightRef.current;
      setLoading(true);
      try {
        const list = await searchReasons(trimmed);
        if (seq === inFlightRef.current) {
          setSuggestions(list);
          setShowList(true);
        }
      } finally {
        if (seq === inFlightRef.current) setLoading(false);
      }
    }, 250);
    return () => {
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
    };
  }, [reason, open]);

  useEffect(() => {
    if (!open) {
      inFlightRef.current++;
      return;
    }
  }, [open]);

  if (!open) return null;

  const handleConfirm = () => {
    onConfirm(reason.trim());
    setReason("");
  };

  const handleCancel = () => {
    setReason("");
    setShowList(false);
    onCancel();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/40" onClick={handleCancel} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
          <p className="text-sm font-semibold text-slate-700">회사 차단</p>
          <button onClick={handleCancel} className="text-slate-400 hover:text-slate-600 text-sm">
            ✕
          </button>
        </div>
        <div className="px-4 py-4">
          <p className="text-sm text-slate-700">
            <span className="font-medium">{company}</span> 회사의 공고를 숨길까요?
          </p>
          <p className="text-xs text-slate-400 mt-1 mb-3">차단한 회사의 공고는 더 이상 표시되지 않습니다.</p>
          <div className="relative">
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleConfirm();
                if (e.key === "Escape") setShowList(false);
              }}
              onFocus={() => { if (reason.trim() && suggestions.length) setShowList(true); }}
              placeholder="사유(선택) — 입력하면 추천이 표시됩니다"
              autoFocus
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            {loading && <span className="absolute right-3 top-2.5 text-xs text-slate-400">…</span>}
            {showList && suggestions.length > 0 && (
              <ul className="absolute z-10 mt-1 w-full max-h-48 overflow-auto rounded-lg border border-slate-200 bg-white shadow-lg">
                {suggestions.map((s) => (
                  <li key={s.id}>
                    <button
                      type="button"
                      className="w-full text-left px-3 py-2 text-sm text-slate-700 hover:bg-blue-50"
                      onMouseDown={(e) => { e.preventDefault(); setReason(s.name); setShowList(false); }}
                    >
                      {s.name}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
        <div className="flex items-center justify-end gap-2 px-4 pb-4">
          <button
            onClick={handleCancel}
            className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            onClick={handleConfirm}
            className="px-3 py-1.5 text-sm font-medium bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition-colors"
          >
            차단
          </button>
        </div>
      </div>
    </div>
  );
}
