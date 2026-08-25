import { Outlet, useLocation } from "react-router-dom";
import { Search, CalendarPlus, FileText } from "lucide-react";
import AppShell from "../shell/AppShell";
import type { DrawerSection } from "../shell/SideDrawer";
import { useAuth } from "../hooks/useAuth";
import { useCrawlNotifications } from "./crawlNotifications";

const drawerSections: DrawerSection[] = [
  {
    label: "데이터",
    items: [{ label: "통합검색", href: "/" }],
  },
  {
    label: "수집",
    items: [
      { label: "스케줄 등록", href: "/schedule" },
      { label: "공고 뷰어", href: "/viewer" },
    ],
  },
];

export default function Layout() {
  const location = useLocation();
  const { user, loading, logout } = useAuth();
  const notifications = useCrawlNotifications();

  const isActive = (path: string) =>
    location.pathname === path || (path === "/" && location.pathname === "/search");

  const subnavItems = [
    { label: "통합검색", href: "/", icon: Search, active: isActive("/") },
    { label: "스케줄 등록", href: "/schedule", icon: CalendarPlus, active: isActive("/schedule") },
    { label: "뷰어", href: "/viewer", icon: FileText, active: isActive("/viewer") },
  ];

  return (
    <AppShell
      currentApp="scraper"
      subnavItems={subnavItems}
      drawerSections={drawerSections}
      notifications={notifications}
      user={user ? { name: user.name, email: user.email } : null}
      authLoading={loading}
      isAdmin={user?.role === "ADMIN"}
      onLogout={logout}
      mainClassName="flex-1 overflow-hidden"
      basePath="/scraper"
    >
      <Outlet />
    </AppShell>
  );
}
