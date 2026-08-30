import { useEffect, useRef, useState } from "react";

export interface BlockConfirmDialogProps {
  open: boolean;
  company: string;
  onCancel: () => void;
  onConfirm: (reason: string, reasonIds: number[]) => void;
}

interface BlockCategory {
  id: number;
  name: string;
  category: string;
}

const GROUP_LABELS: Record<string, string> = {
  company_type: "회사유형",
  reason: "사유",
};

/**
 * 회사 차단 확인용 공용 다이얼로그.
 * 카테고리는 DB 마스터(block_reasons)에서 로드되어 회사유형/사유 그룹으로 나뉘어
 * 다중 선택(체크박스)된다. 추가로 자유 메모(reason)도 입력 가능.
 *
 * @param open 열림 여부
 * @param company 차단할 회사명
 * @param onCancel 취소 콜백
 * @param onConfirm 차단 확정 콜백 (자유메모, 선택한 카테고리 id 목록)
 */
export default function BlockConfirmDialog({ open, company, onCancel, onConfirm }: BlockConfirmDialogProps) {
  const [categories, setCategories] = useState<BlockCategory[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [reason, setReason] = useState("");
  const [loading, setLoading] = useState(false);
  const inFlightRef = useRef(0);

  const fetchCategories = async () => {
    const token = localStorage.getItem("accessToken") ?? "";
    const res = await fetch(`/scraper/company-blacklist/reasons`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!res.ok) return [] as BlockCategory[];
    const json = await res.json();
    return (json.data ?? json) as BlockCategory[];
  };

  useEffect(() => {
    if (!open) return;
    setSelected(new Set());
    setReason("");
    const seq = ++inFlightRef.current;
    setLoading(true);
    fetchCategories()
      .then((list) => { if (seq === inFlightRef.current) setCategories(list); })
      .catch(() => {})
      .finally(() => { if (seq === inFlightRef.current) setLoading(false); });
  }, [open]);

  if (!open) return null;

  const groups = ["company_type", "reason"]
    .map((gc) => ({ key: gc, label: GROUP_LABELS[gc] ?? gc, items: categories.filter((c) => c.category === gc) }))
    .filter((g) => g.items.length > 0);

  const toggle = (id: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const handleConfirm = () => {
    onConfirm(reason.trim(), [...selected]);
    setReason("");
  };

  const handleCancel = () => {
    setReason("");
    onCancel();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/40" onClick={handleCancel} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md max-h-[85vh] flex flex-col">
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
          <p className="text-sm font-semibold text-slate-700">회사 차단</p>
          <button onClick={handleCancel} className="text-slate-400 hover:text-slate-600 text-sm">✕</button>
        </div>
        <div className="px-4 py-4 flex-1 overflow-auto">
          <p className="text-sm text-slate-700">
            <span className="font-medium">{company}</span> 회사의 공고를 숨길까요?
          </p>
          <p className="text-xs text-slate-400 mt-1 mb-3">카테고리를 골라 차단 사유를 등록할 수 있습니다(복수 선택).</p>

          {loading && <p className="text-xs text-slate-400">카테고리 불러오는 중…</p>}

          {groups.map((g) => (
            <div key={g.key} className="mb-3">
              <p className="text-xs font-semibold text-slate-500 mb-1.5">{g.label}</p>
              <div className="flex flex-wrap gap-1.5">
                {g.items.map((c) => {
                  const checked = selected.has(c.id);
                  return (
                    <button
                      key={c.id}
                      type="button"
                      onClick={() => toggle(c.id)}
                      className={`px-2.5 py-1 text-xs rounded-full border transition-colors ${
                        checked
                          ? "bg-blue-600 border-blue-600 text-white"
                          : "bg-white border-slate-300 text-slate-700 hover:border-blue-400"
                      }`}
                    >
                      {checked ? "✓ " : ""}{c.name}
                    </button>
                  );
                })}
              </div>
            </div>
          ))}

          <div className="mt-2">
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
