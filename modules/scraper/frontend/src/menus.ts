import { Search, CalendarPlus, FileText, Building2 } from "lucide-react";
import type { AppMenu } from "@sh-platform/shell";

/** scraper 단일 메뉴 소스 (DB화 대비 — GET /menus 스키마와 동일) */
export const scraperMenu: AppMenu = {
  app: "scraper",
  items: [
    { id: "scraper.search", label: "통합검색", href: "/", icon: Search, primary: true, section: "검색", order: 10 },
    { id: "scraper.schedule", label: "스케줄 등록", href: "/schedule", icon: CalendarPlus, primary: true, section: "수집", order: 20 },
    { id: "scraper.viewer", label: "공고 뷰어", href: "/viewer", icon: FileText, primary: true, section: "수집", order: 30 },
    { id: "scraper.companies", label: "회사 관리", href: "/companies", icon: Building2, section: "데이터", order: 40 },
  ],
};
