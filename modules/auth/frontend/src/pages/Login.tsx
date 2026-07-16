import { useState } from "react";
import { Link } from "react-router-dom";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
      if (res.ok) {
        const data = await res.json();
        localStorage.setItem("accessToken", data.data?.accessToken);
        localStorage.setItem("refreshToken", data.data?.refreshToken);
        window.location.href = "/platform";
      } else {
        alert("로그인 실패");
      }
    } catch {
      alert("로그인 중 오류가 발생했습니다");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">SH Platform</h1>
          <p className="text-slate-400">SaaS 플랫폼에 로그인하세요</p>
        </div>

        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 shadow-2xl border border-white/20">
          <form onSubmit={handleLogin} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="user@example.com"
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-300 mb-1.5">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl transition-colors"
            >
              로그인
            </button>
          </form>

          <div className="mt-6">
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-white/20"></div>
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-2 bg-transparent text-slate-400">또는</span>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-2 gap-3">
              <a href="/oauth2/authorization/kakao" className="flex items-center justify-center gap-2 py-3 bg-[#FEE500] hover:bg-[#FDD835] text-[#191919] font-medium rounded-xl transition-colors">
                <span className="text-lg">K</span> 카카오
              </a>
              <a href="/oauth2/authorization/naver" className="flex items-center justify-center gap-2 py-3 bg-[#03C75A] hover:bg-[#02B34D] text-white font-medium rounded-xl transition-colors">
                <span className="text-lg">N</span> 네이버
              </a>
              <a href="/oauth2/authorization/google" className="flex items-center justify-center gap-2 py-3 bg-white hover:bg-slate-100 text-slate-700 font-medium rounded-xl transition-colors">
                <span className="text-lg">G</span> 구글
              </a>
              <a href="/oauth2/authorization/github" className="flex items-center justify-center gap-2 py-3 bg-[#24292E] hover:bg-[#2F363D] text-white font-medium rounded-xl transition-colors">
                <span className="text-lg">G</span> GitHub
              </a>
            </div>
          </div>

          <div className="mt-6 text-center">
            <Link to="/signup" className="text-sm text-blue-400 hover:text-blue-300">
              계정이 없으신가요? 회원가입
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
