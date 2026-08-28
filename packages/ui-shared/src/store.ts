/**
 * 전역 다이얼로그 요청 타입.
 * window.alert/confirm/prompt 를 오버라이드했을 때 대기열에 쌓여 하나씩 렌더링된다.
 */
export type DialogRequest =
  | { kind: "alert"; id: number; message: string; resolve: () => void }
  | { kind: "confirm"; id: number; message: string; resolve: (value: boolean) => void }
  | { kind: "prompt"; id: number; message: string; initial: string; resolve: (value: string | null) => void };

let list: DialogRequest[] = [];
const listeners = new Set<() => void>();
let seq = 0;

function notify(): void {
  for (const fn of listeners) fn();
}

/**
 * 다이얼로그 대기열 변경을 구독한다.
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
 * 현재 대기열(표시 중 + 대기 중)의 다이얼로그 목록을 반환한다.
 *
 * @return 요청 배열
 */
export function getList(): DialogRequest[] {
  return list;
}

/**
 * 알림 다이얼로그를 대기열에 추가한다.
 *
 * @param message 표시할 메시지
 * @return 사용자가 닫을 때 resolve 되는 Promise
 */
export function showAlert(message: string): Promise<void> {
  return new Promise<void>((resolve) => {
    list = [...list, { kind: "alert", id: ++seq, message, resolve }];
    notify();
  });
}

/**
 * 확인/취소 다이얼로그를 대기열에 추가한다.
 *
 * @param message 표시할 메시지
 * @return 사용자 선택 결과 (확인=true, 취소=false)
 */
export function showConfirm(message: string): Promise<boolean> {
  return new Promise<boolean>((resolve) => {
    list = [...list, { kind: "confirm", id: ++seq, message, resolve }];
    notify();
  });
}

/**
 * 입력 다이얼로그를 대기열에 추가한다.
 *
 * @param message 안내 메시지
 * @param initial 초기 입력값
 * @return 입력 결과 (취소 시 null)
 */
export function showPrompt(message: string, initial = ""): Promise<string | null> {
  return new Promise<string | null>((resolve) => {
    list = [...list, { kind: "prompt", id: ++seq, message, initial, resolve }];
    notify();
  });
}

/**
 * 특정 다이얼로그를 닫고 결과를 resolve 한다.
 *
 * @param request 닫을 요청
 * @param value alert=무시 / confirm=boolean / prompt=string|null
 */
export function closeDialog(request: DialogRequest, value?: unknown): void {
  if (request.kind === "alert") {
    request.resolve();
  } else if (request.kind === "confirm") {
    request.resolve(value === true);
  } else {
    request.resolve(typeof value === "string" ? value : null);
  }
  list = list.filter((r) => r !== request);
  notify();
}
