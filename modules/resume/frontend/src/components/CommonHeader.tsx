import { logout } from "../api/client";

export default function CommonHeader({ loggedIn }: { loggedIn: boolean }) {
  return (
    <header className="bg-slate-900 text-white h-14 flex items-center justify-between px-5 shrink-0 border-b border-slate-700">
      <a href="/platform/" className="text-lg font-bold tracking-tight">
        SH Platform
      </a>
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
