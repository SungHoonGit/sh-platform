import { Link } from "react-router-dom";

export default function NotFound() {
  return (
    <div className="h-full flex flex-col items-center justify-center p-8">
      <div className="text-7xl font-bold text-slate-300 mb-4">404</div>
      <h1 className="text-xl font-semibold text-slate-800 mb-2">페이지를 찾을 수 없습니다</h1>
      <p className="text-sm text-slate-500 mb-6">
        요청하신 페이지가 존재하지 않거나 이동되었습니다.
      </p>
      <Link
        to="/platform"
        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors"
      >
        대시보드로 돌아가기
      </Link>
    </div>
  );
}
