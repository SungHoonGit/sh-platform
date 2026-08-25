import { useState, type ReactNode } from "react";
import GlobalHeader, { type Props as GlobalHeaderProps } from "./GlobalHeader";
import SubNav from "./SubNav";
import SideDrawer, { type DrawerSection } from "./SideDrawer";
import type { ShellApp } from "./tokens";

/**
 * 통합 앱 셸. 3개 앱의 Layout이 이 컴포넌트만 사용한다.
 * props: 현재 앱, 서브내비, 사이드바 섹션, (선택) 알림 벨, 사용자 정보.
 * SHELL_VERSION: 1
 */
export default function AppShell({
  currentApp,
  subnavItems,
  drawerSections,
  notifications,
  user,
  authLoading,
  isAdmin,
  onLogout,
  mainClassName = "flex-1 overflow-auto",
  /** 내부 라우트 프리픽스 (예: scraper는 "/scraper"). subnav/drawer href에 자동 적용 */
  basePath = "",
  children,
}: {
  currentApp: ShellApp;
  subnavItems: { label: string; href?: string; icon?: import("lucide-react").LucideIcon; active?: boolean }[];
  drawerSections: DrawerSection[];
  notifications?: GlobalHeaderProps["notifications"];
  user: { name: string; email?: string } | null;
  authLoading?: boolean;
  isAdmin?: boolean;
  onLogout: () => void;
  mainClassName?: string;
  basePath?: string;
  children: ReactNode;
}) {
  const [drawerOpen, setDrawerOpen] = useState(false);

  const withBase = (href?: string) => (href ? `${basePath}${href}` : undefined);
  const prefixedSubnav = subnavItems.map((i) => ({ ...i, href: withBase(i.href) }));
  const prefixedDrawer = drawerSections.map((s) => ({
    ...s,
    items: s.items.map((i) => ({ ...i, href: withBase(i.href) })),
  }));

  return (
    <div className="h-screen flex flex-col bg-slate-50">
      <GlobalHeader
        currentApp={currentApp}
        user={user}
        authLoading={authLoading}
        isAdmin={isAdmin}
        notifications={notifications}
        onLogout={onLogout}
        onToggleDrawer={() => setDrawerOpen((v) => !v)}
      />
      <SubNav items={prefixedSubnav} />
      <main className={mainClassName}>{children}</main>
      <SideDrawer open={drawerOpen} onClose={() => setDrawerOpen(false)} sections={prefixedDrawer} />
    </div>
  );
}
