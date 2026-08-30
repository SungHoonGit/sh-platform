export interface BlacklistItemLike {
  id: number;
  companyNameNormalized: string;
  reason?: string | null;
  blockReasons?: { id: number; name: string; category: string }[];
}

export interface BlacklistManagerModalProps<T extends BlacklistItemLike> {
  open: boolean;
  items: T[];
  onClose: () => void;
  onUnblock: (item: T) => void;
  emptyHint?: string;
}

/**
 * 차단 회사 목록 관리용 공용 모달.
 *
 * @param open 열림 여부
 * @param items 차단 목록
 * @param onClose 닫기 콜백
 * @param onUnblock 해제 콜백
 * @param emptyHint 빈 목록 안내 문구
 */
export default function BlacklistManagerModal<T extends BlacklistItemLike>({ open, items, onClose, onUnblock, emptyHint }: BlacklistManagerModalProps<T>) {
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
              {emptyHint ?? "차단한 회사가 없습니다."}
            </p>
          ) : (
            <ul className="divide-y divide-slate-100">
              {items.map((b) => (
                <li key={b.id} className="flex items-center justify-between gap-3 py-2.5">
                  <div className="min-w-0">
                    <span className="text-sm font-medium text-slate-700">{b.companyNameNormalized}</span>
                    {b.blockReasons && b.blockReasons.length > 0 && (
                      <div className="flex flex-wrap gap-1 mt-1">
                        {b.blockReasons.map((r) => (
                          <span
                            key={r.id}
                            className={`inline-block px-1.5 py-0.5 rounded text-[10px] font-medium ${
                              r.category === "company_type"
                                ? "bg-violet-100 text-violet-700"
                                : "bg-slate-100 text-slate-600"
                            }`}
                          >
                            {r.name}
                          </span>
                        ))}
                      </div>
                    )}
                    {b.reason && <p className="text-xs text-slate-400 mt-1">— {b.reason}</p>}
                  </div>
                  <button
                    onClick={() => onUnblock(b)}
                    className="text-xs text-blue-600 hover:underline shrink-0"
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
