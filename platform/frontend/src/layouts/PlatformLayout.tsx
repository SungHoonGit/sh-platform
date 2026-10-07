import { Outlet, useLocation } from "react-router-dom";
import { AppShell, toSubnavItems, toDrawerSections, useAppMenu } from "@sh-platform/shell";
import { useAuth } from "../hooks/useAuth";
import { platformMenu } from "../menus";

export default function PlatformLayout() {
  const location = useLocation();
  const { user, loading, logout } = useAuth();
  const isAdmin = user?.role === "ADMIN";
  const menu = useAppMenu("platform", platformMenu);

  return (
    <AppShell
      currentApp="platform"
      subnavItems={toSubnavItems(menu, isAdmin, (href) => href != null && location.pathname === href)}
      drawerSections={toDrawerSections(menu, isAdmin)}
      user={user ? { name: user.name, email: user.email } : null}
      authLoading={loading}
      isAdmin={isAdmin}
      onLogout={logout}
      mainClassName="flex-1 overflow-auto bg-slate-50"
    >
      <Outlet />
    </AppShell>
  );
}
