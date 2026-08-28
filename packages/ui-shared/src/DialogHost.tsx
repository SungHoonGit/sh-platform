import { useEffect, useState } from "react";
import { closeDialog, getList, subscribe, type DialogRequest } from "./store";

function AlertToast({ request }: { request: Extract<DialogRequest, { kind: "alert" }> }) {
  const [leaving, setLeaving] = useState(false);
  useEffect(() => {
    const t = setTimeout(() => {
      setLeaving(true);
      setTimeout(() => closeDialog(request), 200);
    }, 4000);
    return () => clearTimeout(t);
  }, [request]);

  return (
    <div
      className={`pointer-events-auto flex items-start justify-between gap-3 rounded-xl bg-white shadow-lg ring-1 ring-slate-200 px-4 py-3 text-sm text-slate-700 transition-all duration-200 ${
        leaving ? "translate-x-4 opacity-0" : "translate-x-0 opacity-100"
      }`}
    >
      <span className="whitespace-pre-wrap">{request.message}</span>
      <button
        onClick={() => closeDialog(request)}
        className="shrink-0 text-slate-400 hover:text-slate-600 transition-colors leading-none"
        aria-label="닫기"
      >
        ✕
      </button>
    </div>
  );
}

function ConfirmToast({ request }: { request: Extract<DialogRequest, { kind: "confirm" }> }) {
  return (
    <div className="pointer-events-auto flex items-center justify-between gap-4 rounded-xl bg-white shadow-lg ring-1 ring-slate-200 px-4 py-3 text-sm text-slate-700">
      <span className="whitespace-pre-wrap">{request.message}</span>
      <div className="flex items-center gap-2 shrink-0">
        <button
          onClick={() => closeDialog(request, false)}
          className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
        >
          취소
        </button>
        <button
          onClick={() => closeDialog(request, true)}
          className="px-3 py-1.5 text-sm font-medium bg-slate-800 text-white rounded-lg hover:bg-slate-700 transition-colors"
        >
          확인
        </button>
      </div>
    </div>
  );
}

function PromptModal({ request }: { request: Extract<DialogRequest, { kind: "prompt" }> }) {
  const [value, setValue] = useState(request.initial);
  return (
    <>
      <div className="absolute inset-0 bg-black/40" onClick={() => closeDialog(request, null)} />
      <div className="relative bg-white rounded-xl shadow-xl w-full max-w-sm">
        <div className="px-4 py-4">
          <p className="text-sm text-slate-700 whitespace-pre-wrap mb-3">{request.message}</p>
          <input
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={(e) => { if (e.key === "Enter") closeDialog(request, value); }}
            autoFocus
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
        <div className="flex items-center justify-end gap-2 px-4 pb-4">
          <button
            onClick={() => closeDialog(request, null)}
            className="px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition-colors"
          >
            취소
          </button>
          <button
            onClick={() => closeDialog(request, value)}
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
 * - alert / confirm : 우측 상단 토스트 스택
 * - prompt          : 중앙 모달
 * 앱 루트에 한 번 마운트하면 된다.
 */
export default function DialogHost() {
  const [list, setList] = useState<DialogRequest[]>(() => getList());

  useEffect(() => {
    const unsub = subscribe(() => setList(getList()));
    return unsub;
  }, []);

  const prompt = list.find((r) => r.kind === "prompt");
  const toasts = list.filter((r) => r.kind !== "prompt");

  return (
    <>
      {toasts.length > 0 && (
        <div className="fixed top-4 right-4 z-[100] flex flex-col items-end gap-2">
          {toasts.map((r) =>
            r.kind === "alert" ? <AlertToast key={r.id} request={r} /> : <ConfirmToast key={r.id} request={r} />
          )}
        </div>
      )}
      {prompt && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center" role="dialog" aria-modal="true">
          <PromptModal key={prompt.id} request={prompt} />
        </div>
      )}
    </>
  );
}
