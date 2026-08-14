import { useEffect } from "react";
import { useSearchParams, Link } from "react-router-dom";

const ERROR_MESSAGES: Record<string, string> = {
  missing_tokens: "로그인 토큰이 전달되지 않았습니다.",
  oauth2_failed: "소셜 로그인에 실패했습니다.",
  access_denied: "사용자가 로그인을 거부했습니다.",
  invalid_request: "잘못된 요청입니다.",
  server_error: "서버 오류가 발생했습니다.",
};

export default function AuthError() {
  const [searchParams] = useSearchParams();
  const message = searchParams.get("message") || "unknown_error";
  const displayMessage = ERROR_MESSAGES[message] || `오류: ${message}`;

  useEffect(() => {
    const timer = setTimeout(() => {
      window.location.href = "/";
    }, 5000);
    return () => clearTimeout(timer);
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md text-center">
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-8 shadow-2xl border border-white/20">
          <div className="text-5xl mb-4">⚠️</div>
          <h1 className="text-xl font-bold text-white mb-2">로그인 실패</h1>
          <p className="text-slate-400 mb-6">{displayMessage}</p>
          <Link
            to="/"
            className="inline-block px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white font-semibold rounded-xl transition-colors"
          >
            로그인으로 돌아가기
          </Link>
          <p className="text-slate-500 text-sm mt-4">5초 후 자동으로 이동합니다...</p>
        </div>
      </div>
    </div>
  );
}
