/**
 * 인증 redirect 공통 유틸 (설계 034).
 * 4개 프론트 앱(platform/scraper/resume/auth)이 공유한다.
 * React 의존 없음 — Pure TS.
 */

const LOGIN_PATH = "/";

/**
 * (명령형) 고정 경로 기준 로그인 URL을 만든다.
 *
 * @param path 로그인 후 이동할 상대경로 (예: "/scraper/", "/platform")
 * @return `/?redirect=<path>` 형태의 URL
 */
export function loginUrl(path: string): string {
  return `${LOGIN_PATH}?redirect=${encodeURIComponent(path)}`;
}

/**
 * (명령형) 현재 주소 기준 로그인 URL을 만든다.
 * hash 라우팅 앱(resume)은 hashRoute=true로 현재 화면(`#/r/1/edit` 등)까지 보존한다.
 *
 * @param hashRoute window.location.hash 포함 여부
 * @return `/?redirect=<현재 위치>` 형태의 URL
 */
export function loginUrlHere(hashRoute = false): string {
  const path =
    window.location.pathname + (hashRoute ? window.location.hash : "");
  return loginUrl(path);
}

/**
 * (명령형) 현재 주소 기준으로 로그인 페이지로 이동한다.
 *
 * @param hashRoute window.location.hash 포함 여부 (hash 라우팅 앱은 true)
 */
export function redirectToLogin(hashRoute = false): void {
  window.location.replace(loginUrlHere(hashRoute));
}

/**
 * (검증) 로그인 페이지 ?redirect 값을 상대경로만 허용하도록 정제한다.
 * open-redirect 방지: `//evil.com`, 백slash, 제어문자, 외부 URL은 fallback.
 *
 * @param raw 원본 redirect 쿼리값
 * @param fallback 허용되지 않을 때 사용할 기본 경로
 * @return 검증된 상대경로
 */
export function sanitizeRedirect(
  raw: string | null | undefined,
  fallback: string,
): string {
  if (!raw) return fallback;
  const value = raw.trim();
  if (value === "" || !value.startsWith("/")) return fallback;
  if (value.startsWith("//") || value.includes("\\")) return fallback;
  for (let i = 0; i < value.length; i++) {
    if (value.charCodeAt(i) < 32) return fallback;
  }
  return value;
}
