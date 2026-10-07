import type { LucideIcon } from "lucide-react";
import type { DrawerSection } from "./SideDrawer";

export type AppName = "platform" | "scraper" | "resume" | "auth";
export type MenuRole = "ALL" | "ADMIN";

/**
 * 앱 메뉴 단일 정의 (DB화 대비 스키마 — 향후 GET /menus 응답과 동일).
 * primary: 상단 SubNav 노출, section: 드로어 섹션 라벨.
 */
export interface MenuItem {
  id: string;
  label: string;
  href?: string;
  icon?: LucideIcon;
  external?: boolean;
  primary?: boolean;
  section?: string;
  order: number;
  roles?: MenuRole[];
  visible?: boolean;
}

export interface AppMenu {
  app: AppName;
  items: MenuItem[];
}

const visible = (i: MenuItem, isAdmin: boolean) =>
  i.visible !== false && (i.roles == null || i.roles.includes("ALL") || (isAdmin && i.roles.includes("ADMIN")));

/** AppMenu → AppShell subnavItems (primary·표시순 정렬) */
export function toSubnavItems(
  menu: AppMenu,
  isAdmin: boolean,
  isActive: (href?: string) => boolean,
): { label: string; href?: string; icon?: LucideIcon; active: boolean }[] {
  return menu.items
    .filter((i) => i.primary && visible(i, isAdmin))
    .sort((a, b) => a.order - b.order)
    .map((i) => ({ label: i.label, href: i.href, icon: i.icon, active: isActive(i.href) }));
}

/** AppMenu → AppShell drawerSections (section 순서 = 최초 order, 항목은 order순) */
export function toDrawerSections(menu: AppMenu, isAdmin: boolean): DrawerSection[] {
  const groups = new Map<string, MenuItem[]>();
  for (const i of menu.items.filter((i) => i.section && visible(i, isAdmin)).sort((a, b) => a.order - b.order)) {
    const g = groups.get(i.section!);
    if (g) g.push(i);
    else groups.set(i.section!, [i]);
  }
  return [...groups.entries()].map(([label, items]) => ({
    label,
    items: items.map((i) => ({ label: i.label, href: i.href, external: i.external })),
  }));
}
