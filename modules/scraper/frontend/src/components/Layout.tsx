import { Outlet, useLocation } from "react-router-dom";
import { AppShell, toSubnavItems, toDrawerSections, useAppMenu } from "@sh-platform/shell";
import { useAuth } from "../hooks/useAuth";
import { useCrawlNotifications } from "./crawlNotifications";
import { scraperMenu } from "../menus";

export default function Layout() {
  const location = useLocation();
  const { user, loading, logout } = useAuth();
  const notifications = useCrawlNotifications();
  const menu = useAppMenu("scraper", scraperMenu);

  const isActive = (path?: string) =>
    path != null &&
    (location.pathname === path || (path === "/" && location.pathname === "/search"));

  return (
    <AppShell
      currentApp="scraper"
      subnavItems={toSubnavItems(menu, user?.role === "ADMIN", isActive)}
      drawerSections={toDrawerSections(menu, user?.role === "ADMIN")}
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
