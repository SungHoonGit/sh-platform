/**
 * 통합 셸 디자인 토큰.
 * 3개 앱(scraper/resume/platform)이 동일한 값을 사용한다. 수정 시 3벌 동기화 필수!
 * SHELL_VERSION: 1 (docs/plans/014 참조)
 */
export const T = {
  headerBg: "bg-slate-900 text-white border-b border-slate-700",
  headerH: "h-12 px-4",
  brandText: "text-base font-bold tracking-tight text-white",

  navItemIdle: "text-slate-300 hover:text-white hover:bg-slate-800",
  navItemActive: "text-white font-semibold bg-slate-800",

  subnavBg: "bg-slate-100 border-b border-slate-200 px-4 h-10 flex items-center gap-4 shrink-0 overflow-x-auto",
  subnavItemIdle: "text-sm font-medium text-slate-500 hover:text-slate-900 whitespace-nowrap py-2 border-b-2 border-transparent",
  subnavItemActive: "text-sm font-semibold text-slate-900 whitespace-nowrap py-2 border-b-2 border-slate-900",

  drawerBg: "bg-slate-900 text-slate-200 w-64",
  drawerSectionHeader:
    "w-full flex items-center justify-between px-4 py-2.5 text-xs font-semibold uppercase tracking-wide text-slate-400 hover:text-white",
  drawerItem: "block w-full text-left pl-8 pr-4 py-2 text-sm text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg mx-2",
  drawerItemActive: "bg-slate-800 text-white",

  card: "bg-white rounded-xl border border-slate-200 shadow-sm",
} as const;

export type ShellApp = "platform" | "scraper" | "resume";

export const APP_LABELS: Record<ShellApp, string> = {
  platform: "플랫폼",
  scraper: "스크래퍼",
  resume: "이력서",
};

export const APP_HREFS: Record<ShellApp, string> = {
  platform: "/platform",
  scraper: "/scraper/",
  resume: "/resume/",
};
