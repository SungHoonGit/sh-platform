import { Outlet, NavLink, useNavigate } from "react-router-dom";
import { LayoutDashboard, Search, FileText, Briefcase, LogOut } from "lucide-react";

const navItems = [
  { to: "/platform", icon: LayoutDashboard, label: "대시보드", end: true },
  { to: "/platform/scraper", icon: Search, label: "스크래퍼" },
  { to: "/platform/resume", icon: FileText, label: "이력서" },
  { to: "/platform/portfolio", icon: Briefcase, label: "포트폴리오" },
];

export default function PlatformLayout() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("refreshToken");
    navigate("/");
  };

  return (
    <div className="flex h-screen bg-slate-100">
      {/* 사이드바 */}
      <aside className="w-64 bg-slate-900 text-white flex flex-col">
        <div className="p-5 border-b border-slate-700">
          <h1 className="text-xl font-bold">SH Platform</h1>
          <p className="text-xs text-slate-400 mt-1">SaaS 플랫폼</p>
        </div>

        <nav className="flex-1 p-3 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm transition-colors ${
                  isActive
                    ? "bg-blue-600 text-white"
                    : "text-slate-300 hover:bg-slate-800"
                }`
              }
            >
              <item.icon size={18} />
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="p-3 border-t border-slate-700">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 w-full px-3 py-2.5 rounded-lg text-sm text-slate-300 hover:bg-slate-800 transition-colors"
          >
            <LogOut size={18} />
            로그아웃
          </button>
        </div>
      </aside>

      {/* 메인 */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
