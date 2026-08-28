export interface BlacklistItem {
  id: number;
  accountId: number;
  companyNameNormalized: string;
  reason: string | null;
  createdAt: string;
}

interface BlacklistManagerModalProps {
  open: boolean;
  items: BlacklistItem[];
  onClose: () => void;
  onUnblock: (item: BlacklistItem) => void;
}

export default function BlacklistManagerModal({ open, items, onClose, onUnblock }: BlacklistManagerModalProps) {
  if (!open) return null;
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
    >
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-md max-h-[70vh] flex flex-col">
        <div className="flex items-center justify-between px-4 py-3 border-b border-slate-200">
          <p className="text-sm font-semibold text-slate-700">
            차단한 회사{items.length > 0 ? ` (${items.length})` : ""}
          </p>
          <button
            onClick={onClose}
            className="text-xs text-slate-400 hover:text-slate-600"
          >
            ✕
          </button>
        </div>
        <div className="flex-1 overflow-auto px-4 py-2">
          {items.length === 0 ? (
            <p className="text-xs text-slate-400 py-6 text-center">
              차단한 회사가 없습니다. 결과 행의 EyeOff 아이콘으로 차단하세요.
            </p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {items.map((b) => (
                <li key={b.id} className="flex items-center justify-between py-2.5">
                  <span className="text-sm">
                    {b.companyNameNormalized}
                    {b.reason && <span className="text-xs text-slate-400 ml-2">— {b.reason}</span>}
                  </span>
                  <button
                    onClick={() => onUnblock(b)}
                    className="text-xs text-blue-600 hover:underline"
                  >
                    해제
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
