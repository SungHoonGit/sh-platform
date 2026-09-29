import { useAuth as useShellAuth, type AuthState, type UserProfile } from "@sh-platform/shell";

export type { AuthState, UserProfile };

/**
 * 스크래퍼용 인증 훅. 공통 셸 useAuth에 앱 설정만 전달한다 (설계 035).
 */
export function useAuth(): AuthState {
  return useShellAuth("scraper");
}
