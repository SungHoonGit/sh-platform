import { Outlet, NavLink } from "react-router-dom";
import { Shield, Users, Building2 } from "lucide-react";
import CommonHeader from "../components/CommonHeader";
import { AuthGuard } from "@common";

const adminItems = [
  { to: "/platform/admin", icon: Shield, label: "관리자 대시보드", end: true },
  { to: "/platform/admin/users", icon: Users, label: "사용자 관리" },
  { to: "/platform/admin/tenants", icon: Building2, label: "테넌트 관리" },
];

export default function PlatformLayout() {
  return (
    <AuthGuard>
      <div className="h-screen flex flex-col bg-slate-100">
        <CommonHeader />
        <div className="flex flex-1 overflow-hidden">
          <aside className="w-56 bg-slate-900 text-white flex flex-col shrink-0">
            <nav className="flex-1 p-3 space-y-1">
              <div className="pt-3 pb-2 px-3 text-xs font-medium text-slate-500 uppercase">관리</div>
              {adminItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                      isActive ? "bg-amber-600 text-white" : "text-slate-300 hover:bg-slate-800"
                    }`
                  }
                >
                  <item.icon size={18} />
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </aside>
          <main className="flex-1 overflow-auto">
            <Outlet />
          </main>
        </div>
      </div>
    </AuthGuard>
  );
}
