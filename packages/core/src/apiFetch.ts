/**
 * 인증 fetch 공통 래퍼 (설계 034 Phase 2).
 *
 * - 인증 헤더 자동 부여 (Authorization 미지정 시 accessToken 사용)
 * - redirectOn401=true면 401 응답에서 토큰 정리 + 현재 위치 보존 로그인 redirect
 *   (이전 화면 복귀 보장 — 세션 만료 경로 통일)
 *
 * 주의: 로그인/회원가입 등 "401이 정상인" 인증 엔드포인트에는 사용하지 않는다.
 */
import { redirectToLogin } from "./auth";
import { clearTokens, getAccessToken } from "./tokens";

export interface ApiFetchOptions extends RequestInit {
  /** 인증 헤더 자동 부여 (기본 true). 공개 엔드포인트는 false */
  auth?: boolean;
  /** 401 응답 시 토큰 정리 + 로그인 redirect (기본 false — throw 정책 앱은 그대로) */
  redirectOn401?: boolean;
  /** redirect에 window.location.hash 포함 (hash 라우팅 앱: resume만 true) */
  hashRoute?: boolean;
}

/**
 * (명령형) Authorization 헤더를 자동 부여하고, 선택적으로 401을 처리하는 fetch.
 *
 * @param input 요청 URL
 * @param options RequestInit + auth/redirectOn401/hashRoute
 * @return 원본 Response (호출부의 기존 상태 분기 유지)
 */
export async function apiFetch(
  input: string,
  options: ApiFetchOptions = {},
): Promise<Response> {
  const { auth = true, redirectOn401 = false, hashRoute = false, ...init } = options;
  const headers = new Headers(init.headers);
  if (auth && !headers.has("Authorization")) {
    const token = getAccessToken();
    if (token) headers.set("Authorization", `Bearer ${token}`);
  }
  const res = await fetch(input, { ...init, headers });
  if (res.status === 401 && redirectOn401) {
    clearTokens();
    redirectToLogin(hashRoute);
  }
  return res;
}
