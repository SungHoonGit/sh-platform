/**
 * 전역 다이얼로그 요청 타입.
 * window.alert/confirm/prompt 를 오버라이드했을 때 하나씩 활성화되어 렌더링된다.
 */
export type DialogRequest =
  | { kind: "alert"; id: number; message: string; resolve: () => void }
  | { kind: "confirm"; id: number; message: string; resolve: (value: boolean) => void }
  | { kind: "prompt"; id: number; message: string; initial: string; resolve: (value: string | null) => void };

let active: DialogRequest | null = null;
const listeners = new Set<() => void>();
let seq = 0;

function notify(): void {
  for (const fn of listeners) fn();
}

/**
 * 활성 다이얼로그 변경을 구독한다.
 *
 * @param fn 상태 변경 콜백
 * @return 구독 해제 함수
 */
export function subscribe(fn: () => void): () => void {
  listeners.add(fn);
  return () => {
    listeners.delete(fn);
  };
}

/**
 * 현재 활성화된 다이얼로그 요청을 반환한다.
 *
 * @return 활성 요청 (없으면 null)
 */
export function getActive(): DialogRequest | null {
  return active;
}

/**
 * 알림 다이얼로그를 띄운다.
 *
 * @param message 표시할 메시지
 * @return 사용자가 닫을 때 resolve 되는 Promise
 */
export function showAlert(message: string): Promise<void> {
  return new Promise<void>((resolve) => {
    active = { kind: "alert", id: ++seq, message, resolve };
    notify();
  });
}

/**
 * 확인/취소 다이얼로그를 띄운다.
 *
 * @param message 표시할 메시지
 * @return 사용자 선택 결과 (확인=true, 취소=false)
 */
export function showConfirm(message: string): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    active = { kind: "confirm", id: ++seq, message, resolve };
    notify();
  });
}

/**
 * 입력 다이얼로그를 띄운다.
 *
 * @param message 안내 메시지
 * @param initial 초기 입력값
 * @return 입력 결과 (취소 시 null)
 */
export function showPrompt(message: string, initial = ""): Promise<string | null> {
  return new Promise<string | null>((resolve) => {
    active = { kind: "prompt", id: ++seq, message, initial, resolve };
    notify();
  });
}

/**
 * 활성 다이얼로그를 닫고 결과를 resolve 한다.
 *
 * @param value alert=무시 / confirm=boolean / prompt=string|string|null
 */
export function closeDialog(value?: unknown): void {
  const cur = active;
  active = null;
  notify();
  if (!cur) return;
  if (cur.kind === "alert") {
    cur.resolve();
  } else if (cur.kind === "confirm") {
    cur.resolve(value === true);
  } else {
    cur.resolve(typeof value === "string" ? value : null);
  }
}
