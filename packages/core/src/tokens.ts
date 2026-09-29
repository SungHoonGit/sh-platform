/**
 * 인증 토큰 저장 공통 유틸 (설계 034 Phase 2).
 * 4개 프론트 앱이 공유하는 localStorage 키("accessToken"/"refreshToken")의 단일 구현.
 * 키 문자열을 앱 코드에 직접 쓰지 않도록 해 키 드리프트를 방지한다.
 */

const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";

/** 액세스 토큰 조회. 없으면 null. */
export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

/** 리프레시 토큰 조회. 없으면 null. */
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

/**
 * 토큰 저장.
 *
 * @param accessToken 액세스 토큰
 * @param refreshToken 리프레시 토큰 (없으면 기존 값 유지)
 */
export function setTokens(accessToken: string, refreshToken?: string | null): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

/** 액세스·리프레시 토큰 모두 삭제. */
export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

/** 액세스 토큰 존재 여부 (로그인 상태 가드용). */
export function hasAccessToken(): boolean {
  return Boolean(localStorage.getItem(ACCESS_TOKEN_KEY));
}
