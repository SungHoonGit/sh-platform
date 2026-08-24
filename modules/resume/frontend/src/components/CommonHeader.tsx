import { useEffect, useState } from "react";
import { logout } from "../api/client";

export type Tab = "resumes" | "applications";

export default function CommonHeader({
  tab,
  loggedIn,
}: {
  tab: Tab;
  loggedIn: boolean;
}) {
  const [current, setCurrent] = useState<Tab>(tab);

  useEffect(() => setCurrent(tab), [tab]);

  const go = (t: Tab) => {
    setCurrent(t);
    window.location.hash = t === "resumes" ? "#/resumes" : "#/applications";
  };

  const tabCls = (t: Tab) =>
    `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
      current === t
        ? "bg-slate-700 text-white"
        : "text-slate-300 hover:text-white hover:bg-slate-800"
    }`;

  return (
    <header className="bg-slate-900 text-white h-14 flex items-center justify-between px-5 shrink-0 border-b border-slate-700">
      <div className="flex items-center gap-5">
        <a href="/platform/" className="text-lg font-bold tracking-tight">
          SH Platform
        </a>
        <nav className="flex items-center gap-1">
          <button onClick={() => go("resumes")} className={tabCls("resumes")}>
            이력서 관리
          </button>
          <button onClick={() => go("applications")} className={tabCls("applications")}>
            지원 관리
          </button>
        </nav>
      </div>
      <div className="flex items-center gap-3">
        {loggedIn ? (
          <button
            onClick={logout}
            className="px-3 py-1.5 text-sm text-slate-300 hover:text-white hover:bg-slate-800 rounded-lg transition-colors"
          >
            로그아웃
          </button>
        ) : (
          <a
            href={`/?redirect=${encodeURIComponent("/resume/")}`}
            className="px-4 py-1.5 bg-blue-600 hover:bg-blue-700 text-sm font-medium rounded-lg transition-colors"
          >
            로그인
          </a>
        )}
      </div>
    </header>
  );
}
