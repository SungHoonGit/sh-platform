import { showAlert, showConfirm, showPrompt } from "./store";

declare global {
  interface Window {
    __shDialogsInstalled?: boolean;
  }
}

/**
 * window.alert/confirm/prompt 를 커스텀 다이얼로그로 오버라이드한다.
 *
 * 주의: confirm/prompt 는 Promise 를 반환하므로 호출부에서 반드시 `await` 해야 한다.
 * 반환값을 쓰는 기존 코드는 `if (!(await confirm(...)))` 형태로 전환 필요.
 *
 * @example
 * initGlobalDialogs();
 * const ok = await window.confirm("정말 삭제할까요?");
 */
export function initGlobalDialogs(): void {
  if (window.__shDialogsInstalled) return;
  window.__shDialogsInstalled = true;
  window.alert = (message?: unknown) => {
    void showAlert(String(message ?? ""));
  };
  window.confirm = ((message?: unknown) => showConfirm(String(message ?? ""))) as unknown as typeof window.confirm;
  window.prompt = ((message?: unknown, defaultValue?: unknown) =>
    showPrompt(String(message ?? ""), defaultValue == null ? "" : String(defaultValue))) as unknown as typeof window.prompt;
}
