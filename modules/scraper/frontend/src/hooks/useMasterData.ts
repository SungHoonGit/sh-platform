import { useQuery } from "@tanstack/react-query";
import { fetchSites, fetchRegions, type SiteDefinitionInfo } from "../api/scraper";

/** 지역 다중 선택의 기본 선택 목록 (표시 순서 1~9: 서울~세종) */
export const DEFAULT_LOCATIONS_FALLBACK = ["서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "세종"];

/** 채용사이트 마스터 조회 (등록 순서 정렬) */
export function useSites() {
  return useQuery({ queryKey: ["sites"], queryFn: fetchSites, staleTime: 5 * 60 * 1000 });
}

/** 활성 채용사이트만 반환 (UI 목록용) */
export function useEnabledSites() {
  const { data = [] } = useSites();
  return data.filter((s) => s.isEnabled);
}

/** 지역 마스터 조회 (활성만, 표시 순서) */
export function useRegions() {
  return useQuery({ queryKey: ["regions"], queryFn: fetchRegions, staleTime: 5 * 60 * 1000 });
}

const BADGE_COLORS: Record<string, string> = {
  blue: "bg-blue-100 text-blue-700 border-blue-200",
  green: "bg-green-100 text-green-700 border-green-200",
  red: "bg-red-100 text-red-700 border-red-200",
  orange: "bg-orange-100 text-orange-700 border-orange-200",
  purple: "bg-purple-100 text-purple-700 border-purple-200",
  yellow: "bg-yellow-100 text-yellow-700 border-yellow-200",
};

const TAB_COLORS: Record<string, string> = {
  blue: "bg-blue-600 text-white",
  green: "bg-green-600 text-white",
  red: "bg-red-600 text-white",
  orange: "bg-orange-600 text-white",
  purple: "bg-purple-600 text-white",
  yellow: "bg-yellow-600 text-white",
};

/** Tailwind는 런타임 클래스 생성 불가 → 사이트 색상 토큰을 고정 클래스로 변환 */
export function siteBadgeColor(token?: string | null): string {
  return (token && BADGE_COLORS[token]) || "bg-slate-100 text-slate-600";
}

export function siteTabColor(token?: string | null): string {
  return (token && TAB_COLORS[token]) || "bg-blue-600 text-white";
}

/** 사이트 코드 → 표시명 (없으면 코드 그대로) */
export function siteDisplayName(sites: SiteDefinitionInfo[], code?: string): string {
  if (!code) return "";
  return sites.find((s) => s.siteName === code)?.displayName || code;
}