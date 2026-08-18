import { useEffect, useState } from "react";

interface MeResponse {
  id: number;
  email: string;
  name: string;
  provider: string | null;
  emailVerified: boolean;
  passwordSet: boolean;
  linkedProviders: string[];
}

const PROVIDER_UI: Record<string, { label: string; color: string; textColor: string; icon: string }> = {
  kakao: { label: "카카오", color: "bg-[#FEE500]", textColor: "text-[#191919]", icon: "K" },
  naver: { label: "네이버", color: "bg-[#03C75A]", textColor: "text-white", icon: "N" },
  google: { label: "구글", color: "bg-white", textColor: "text-slate-700", icon: "G" },
  github: { label: "GitHub", color: "bg-[#24292E]", textColor: "text-white", icon: "G" },
};

const ALL_PROVIDERS = ["kakao", "naver", "google", "github"];

export default function AccountSettings() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [newPassword, setNewPassword] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    fetchMe();
  }, []);

  const fetchMe = async () => {
    try {
      const res = await fetch("/api/v1/auth/me", {
        headers: { Authorization: `Bearer ${localStorage.getItem("accessToken")}` },
      });
      if (res.ok) {
        const data = await res.json();
        setMe(data.data);
      } else if (res.status === 401) {
        window.location.replace("/");
      }
    } catch {
      setError("계정 정보를 불러오는데 실패했습니다");
    } finally {
      setLoading(false);
    }
  };

  const handleSetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage("");
    setError("");
    if (newPassword !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다");
      return;
    }
    try {
      const res = me?.passwordSet
        ? await fetch("/api/v1/auth/password", {
            method: "PUT",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
            },
            body: JSON.stringify({ currentPassword, newPassword }),
          })
        : await fetch("/api/v1/auth/me/password", {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
            },
            body: JSON.stringify({ newPassword }),
          });
      if (res.ok) {
        setMessage("비밀번호가 저장되었습니다");
        setNewPassword("");
        setCurrentPassword("");
        setConfirmPassword("");
        fetchMe();
      } else {
        const data = await res.json();
        setError(data.message || "비밀번호 저장 실패");
      }
    } catch {
      setError("오류가 발생했습니다");
    }
  };

  const handleUnlink = async (provider: string) => {
    if (!window.confirm(`${PROVIDER_UI[provider]?.label} 로그인 연결을 해제하시겠습니까?`)) return;
    setMessage("");
    setError("");
    try {
      const res = await fetch(`/api/v1/auth/oauth2/providers/${provider}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${localStorage.getItem("accessToken")}` },
      });
      if (res.ok) {
        setMessage("연결이 해제되었습니다");
        fetchMe();
      } else {
        const data = await res.json();
        setError(data.message || "연결 해제 실패");
      }
    } catch {
      setError("오류가 발생했습니다");
    }
  };

  if (loading) {
    return (
      <div className="p-8">
        <div className="text-slate-500">불러오는 중...</div>
      </div>
    );
  }

  if (!me) return null;

  return (
    <div className="p-8 max-w-2xl">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800">계정 설정</h1>
        <p className="text-slate-500 mt-1">내 계정 정보와 로그인 수단을 관리합니다</p>
      </div>

      {message && <div className="mb-4 p-3 bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg">{message}</div>}
      {error && <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg">{error}</div>}

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">기본 정보</h2>
        <dl className="space-y-3 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">이메일</dt>
            <dd className="text-slate-800 font-medium">{me.email}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">이름</dt>
            <dd className="text-slate-800 font-medium">{me.name}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-slate-500">이메일 인증</dt>
            <dd className={me.emailVerified ? "text-green-600 font-medium" : "text-amber-600 font-medium"}>
              {me.emailVerified ? "인증됨" : "미인증"}
            </dd>
          </div>
        </dl>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 mb-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">연결된 로그인 수단</h2>
        <div className="space-y-3">
          {ALL_PROVIDERS.map((p) => {
            const ui = PROVIDER_UI[p];
            const linked = me.linkedProviders.includes(p);
            return (
              <div key={p} className="flex items-center justify-between p-3 border border-slate-200 rounded-lg">
                <div className="flex items-center gap-3">
                  <span className={`w-8 h-8 ${ui.color} ${ui.textColor} rounded-lg flex items-center justify-center font-semibold text-sm`}>
                    {ui.icon}
                  </span>
                  <span className="text-sm text-slate-800 font-medium">{ui.label}</span>
                  <span className={`text-xs px-2 py-0.5 rounded-full ${linked ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-500"}`}>
                    {linked ? "연결됨" : "연결 안됨"}
                  </span>
                </div>
                {linked ? (
                  <button
                    onClick={() => handleUnlink(p)}
                    className="text-xs text-red-500 hover:text-red-700 border border-red-200 rounded-lg px-3 py-1.5 hover:bg-red-50 transition-colors"
                  >
                    해제
                  </button>
                ) : (
                  <a
                    href={`/oauth2/authorization/${p}`}
                    className="text-xs text-blue-600 hover:text-blue-800 border border-blue-200 rounded-lg px-3 py-1.5 hover:bg-blue-50 transition-colors"
                  >
                    연결
                  </a>
                )}
              </div>
            );
          })}
        </div>
        <p className="text-xs text-slate-400 mt-3">
          연결한 수단으로 간편 로그인을 할 수 있습니다. 마지막 남은 로그인 수단은 해제할 수 없습니다.
        </p>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">
          {me.passwordSet ? "비밀번호 변경" : "비밀번호 설정"}
        </h2>
        <form onSubmit={handleSetPassword} className="space-y-4">
          {me.passwordSet && (
            <div>
              <label className="block text-sm font-medium text-slate-600 mb-1.5">현재 비밀번호</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1.5">새 비밀번호</label>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="영문+숫자+특수문자 8~20자"
              className="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-600 mb-1.5">새 비밀번호 확인</label>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full px-4 py-2.5 border border-slate-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg text-sm transition-colors"
          >
            저장
          </button>
        </form>
      </div>
    </div>
  );
}
