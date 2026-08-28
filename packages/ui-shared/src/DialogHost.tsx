import { useEffect, useState } from "react";
import { closeDialog, getActive, subscribe, type DialogRequest } from "./store";

function AlertDialog({ message, onClose }: { message: string; onClose: () => void }) {
  return (
    <>
      <div className="absolute inset-0 bg-black/40" onClick={onClose} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="px-4 py-4 text-sm text-slate-700 whitespace-pre-wrap">{message}</div>
        <div className="flex items-center justify-end gap-2 px-4 pb-4">
          <button
            onClick={onClose}
            className="px-3 py-1.5 text-sm font-medium bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition-colors"
          >
            확인
          </button>
        </div>
      </div>
    </>
  );
}

function ConfirmDialog({ message, onConfirm, onCancel }: { message: string; onConfirm: () => void; onCancel: () => void }) {
  return (
    <>
      <div className="absolute inset-0 bg-black/40" onClick={onCancel} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="px-4 py-4 text-sm text-slate-700 whitespace-pre-wrap">{message}</div>
        <div className="flex items-center justify-end gap-2 px-4 pb-4">
          <button
            onClick={onCancel}
            className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            className="px-3 py-1.5 text-sm font-medium bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition-colors"
          >
            확인
          </button>
        </div>
      </div>
    </>
  );
}

function PromptDialog({ message, initial, onSubmit, onCancel }: { message: string; initial: string; onSubmit: (value: string) => void; onCancel: () => void }) {
  const [value, setValue] = useState(initial);
  return (
    <>
      <div className="absolute inset-0 bg-black/40" onClick={onCancel} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="px-4 py-4">
          <p className="text-sm text-slate-700 whitespace-pre-wrap mb-3">{message}</p>
          <input
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") onSubmit(value); }}
            autoFocus
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex items-center justify-end gap-2 px-4 pb-4">
          <button
            onClick={onCancel}
            className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            onClick={() => onSubmit(value)}
            className="px-3 py-1.5 text-sm font-medium bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition-colors"
          >
            확인
          </button>
        </div>
      </div>
    </>
  );
}

/**
 * 전역 alert/confirm/prompt 오버라이드 결과를 렌더링하는 호스트.
 * 앱 루트에 한 번 마운트하면 된다.
 */
export default function DialogHost() {
  const [active, setActive] = useState<DialogRequest | null>(() => getActive());

  useEffect(() => {
    const unsub = subscribe(() => setActive(getActive()));
    return unsub;
  }, []);

  if (!active) return null;

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center" role="dialog" aria-modal="true">
      {active.kind === "alert" && (
        <AlertDialog message={active.message} onClose={() => closeDialog()} />
      )}
      {active.kind === "confirm" && (
        <ConfirmDialog
          message={active.message}
          onConfirm={() => closeDialog(true)}
          onCancel={() => closeDialog(false)}
        />
      )}
      {active.kind === "prompt" && (
        <PromptDialog
          key={active.id}
          message={active.message}
          initial={active.initial}
          onSubmit={(value) => closeDialog(value)}
          onCancel={() => closeDialog(null)}
        />
      )}
    </div>
  );
}
