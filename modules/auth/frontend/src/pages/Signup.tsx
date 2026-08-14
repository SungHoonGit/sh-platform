import { useState } from "react";
import { Link } from "react-router-dom";

type Step = "info" | "verify" | "done";

export default function Signup() {
  const [step, setStep] = useState<Step>("info");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (password !== confirmPassword) {
      setError("비밀번호가 일치하지 않습니다");
      return;
    }
    setError("");
    setLoading(true);
    try {
      const res = await fetch("/api/v1/auth/verify-email", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, purpose: "SIGNUP" }),
      });
      if (res.ok) {
        setStep("verify");
      } else {
        const data = await res.json();
        setError(data.message || "인증 메일 발송 실패");
      }
    } catch {
      setError("오류가 발생했습니다");
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await fetch("/api/v1/auth/verify-code", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, code, purpose: "SIGNUP" }),
      });
      if (res.ok) {
        await handleSignup();
      } else {
        const data = await res.json();
        setError(data.message || "인증 코드가 올바르지 않습니다");
      }
    } catch {
      setError("오류가 발생했습니다");
    } finally {
      setLoading(false);
    }
  };

  const handleSignup = async () => {
    try {
      const res = await fetch("/api/v1/auth/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, email, password }),
      });
      if (res.ok) {
        setStep("done");
      } else {
        const data = await res.json();
        setError(data.message || "회원가입 실패");
      }
    } catch {
      setError("오류가 발생했습니다");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-white mb-2">SH Platform</h1>
          <p className="text-slate-400">새로운 계정을 만드세요</p>
        </div>

        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 shadow-2xl border border-white/20">
          {/* 단계 표시 */}
          <div className="flex items-center justify-center gap-2 mb-6">
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step === "info" ? "bg-blue-600 text-white" : "bg-blue-600/30 text-blue-300"}`}>1</div>
            <div className={`w-12 h-0.5 ${step === "info" ? "bg-white/20" : "bg-blue-600"}`}></div>
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step === "verify" ? "bg-blue-600 text-white" : step === "done" ? "bg-blue-600/30 text-blue-300" : "bg-white/10 text-slate-500"}`}>2</div>
            <div className={`w-12 h-0.5 ${step === "done" ? "bg-blue-600" : "bg-white/20"}`}></div>
            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${step === "done" ? "bg-blue-600 text-white" : "bg-white/10 text-slate-500"}`}>3</div>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-500/20 border border-red-500/30 rounded-xl text-red-300 text-sm text-center">
              {error}
            </div>
          )}

          {/* Step 1: 기본 정보 입력 */}
          {step === "info" && (
            <form onSubmit={handleSendCode} className="space-y-5">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">이름</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="홍길동"
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                />
              </div>
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
                  placeholder="8자 이상"
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                  minLength={8}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">비밀번호 확인</label>
                <input
                  type="password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="비밀번호 재입력"
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  required
                  minLength={8}
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-600/50 text-white font-semibold rounded-xl transition-colors"
              >
                {loading ? "발송 중..." : "인증번호 받기"}
              </button>
            </form>
          )}

          {/* Step 2: 이메일 인증 */}
          {step === "verify" && (
            <form onSubmit={handleVerifyCode} className="space-y-5">
              <div className="text-center">
                <p className="text-slate-300 text-sm">
                  <span className="text-blue-400 font-medium">{email}</span> 로<br />
                  인증번호가 발송되었습니다.
                </p>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-1.5">인증번호 (6자리)</label>
                <input
                  type="text"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  placeholder="000000"
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-xl text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-center text-2xl tracking-[0.5em]"
                  required
                  maxLength={6}
                  autoFocus
                />
              </div>
              <button
                type="submit"
                disabled={loading || code.length !== 6}
                className="w-full py-3 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-600/50 text-white font-semibold rounded-xl transition-colors"
              >
                {loading ? "확인 중..." : "인증하고 가입하기"}
              </button>
              <button
                type="button"
                onClick={() => { setStep("info"); setError(""); }}
                className="w-full py-3 bg-white/10 hover:bg-white/20 text-white font-semibold rounded-xl transition-colors"
              >
                뒤로 가기
              </button>
            </form>
          )}

          {/* Step 3: 완료 */}
          {step === "done" && (
            <div className="text-center space-y-4">
              <div className="text-5xl mb-2">✅</div>
              <p className="text-white font-semibold">회원가입이 완료되었습니다!</p>
              <p className="text-slate-400 text-sm">이제 로그인할 수 있습니다.</p>
              <a
                href="/"
                className="inline-block w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl transition-colors"
              >
                로그인하기
              </a>
            </div>
          )}

          <div className="mt-6 text-center">
            <Link to="/" className="text-sm text-blue-400 hover:text-blue-300">
              이미 계정이 있으신가요? 로그인
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
