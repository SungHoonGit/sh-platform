export interface BlacklistItemLike {
  id: number;
  companyNameNormalized: string;
  reason?: string | null;
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
