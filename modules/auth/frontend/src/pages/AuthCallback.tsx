import { useEffect } from "react";
import { useSearchParams } from "react-router-dom";

export default function AuthCallback() {
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const accessToken = searchParams.get("accessToken");
    const refreshToken = searchParams.get("refreshToken");
    const provider = searchParams.get("provider");
    const returnUrl = searchParams.get("returnUrl") || "/platform";

    if (accessToken && refreshToken) {
      localStorage.setItem("accessToken", accessToken);
      localStorage.setItem("refreshToken", refreshToken);
      if (provider) {
        localStorage.setItem("provider", provider);
      }
      window.location.href = returnUrl;
    } else {
      window.location.href = "/auth/error?message=missing_tokens";
    }
  }, [searchParams]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex items-center justify-center">
      <div className="text-white text-lg">로그인 처리 중...</div>
    </div>
  );
}
