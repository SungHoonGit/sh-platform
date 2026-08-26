import { Outlet, useLocation } from "react-router-dom";
import AppShell from "../shell/AppShell";
import type { DrawerSection } from "../shell/SideDrawer";
import { useAuth } from "../hooks/useAuth";

export default function PlatformLayout() {
  const location = useLocation();
  const { user, loading, logout } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const subnavItems = [
    { label: "개요", href: "/platform", active: location.pathname === "/platform" },
    ...(isAdmin
      ? [{ label: "관리", href: "/platform/admin", active: location.pathname.startsWith("/platform/admin") }]
      : []),
  ];

  const drawerSections: DrawerSection[] = [
    {
      label: "개인 서비스",
      items: [
        { label: "내 이력서", href: "/resume/" },
        { label: "공고 탐색", href: "/resume/#/postings" },
        { label: "지원 관리", href: "/resume/#/applications" },
        { label: "계정 설정", href: "/platform/account" },
      ],
    },
    ...(isAdmin
      ? [
            {
              label: "관리",
              items: [
                { label: "사용자 관리", href: "/platform/admin/users" },
                { label: "테넌트 관리", href: "/platform/admin/tenants" },
                { label: "감사 로그", href: "/platform/admin/audit" },
                { label: "세션 관리", href: "/platform/admin/sessions" },
              ],
            },
        ]
      : []),
    {
      label: "개발자 링크",
      items: [
        { label: "Swagger · Auth", href: "/swagger-ui/index.html", external: true },
        { label: "Swagger · Scraper", href: "/scraper/swagger-ui/index.html", external: true },
        { label: "Swagger · Resume", href: "/resume/swagger-ui/index.html", external: true },
        { label: "Javadoc", href: "/javadoc/", external: true },
        { label: "테스트 리포트", href: "/test-reports/", external: true },
        { label: "SchemaSpy", href: "/schemaSpy/", external: true },
      ],
    },
  ];

  return (
    <AppShell
      currentApp="platform"
      subnavItems={subnavItems}
      drawerSections={drawerSections}
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
