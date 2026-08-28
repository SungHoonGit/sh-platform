import { useState } from "react";

export interface BlockConfirmDialogProps {
  open: boolean;
  company: string;
  onCancel: () => void;
  onConfirm: (reason: string) => void;
}

/**
 * 회사 차단 확인용 공용 다이얼로그 (사유 입력 + 취소).
 *
 * @param open 열림 여부
 * @param company 차단할 회사명
 * @param onCancel 취소 콜백
 * @param onConfirm 차단 확정 콜백 (사유 전달)
 */
export default function BlockConfirmDialog({ open, company, onCancel, onConfirm }: BlockConfirmDialogProps) {
  const [reason, setReason] = useState("");

  if (!open) return null;

  const handleConfirm = () => {
    onConfirm(reason.trim());
    setReason("");
  };

  const handleCancel = () => {
    setReason("");
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
          <input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") handleConfirm(); }}
            placeholder="사유(선택)"
            autoFocus
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
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
