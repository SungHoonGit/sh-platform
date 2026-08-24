import { useNavigate } from "react-router-dom";
import { Search, FileText, ArrowRight } from "lucide-react";

const modules = [
  {
    name: "스크래퍼",
    description: "채용공고 수집 및 검색",
    icon: Search,
    path: "/scraper/",
    external: true,
    color: "bg-blue-500",
  },
  {
    name: "이력서",
    description: "경력·프로젝트 관리 및 이력서 뷰",
    icon: FileText,
    path: "/resume/",
    external: true,
    color: "bg-green-500",
  },
];

export default function Dashboard() {
  const navigate = useNavigate();

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800">대시보드</h1>
        <p className="text-slate-500 mt-1">SH Platform에 오신 것을 환영합니다</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {modules.map((module) => (
          <div
            key={module.name}
            onClick={() => (module.external ? window.open(module.path, "_blank") : navigate(module.path))}
            className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 hover:shadow-md hover:border-slate-300 transition-all cursor-pointer group"
          >
            <div className={`w-12 h-12 ${module.color} rounded-xl flex items-center justify-center mb-4`}>
              <module.icon className="text-white" size={24} />
            </div>
            <h3 className="text-lg font-semibold text-slate-800 mb-1">{module.name}</h3>
            <p className="text-sm text-slate-500 mb-4">{module.description}</p>
            <div className="flex items-center text-sm text-blue-600 group-hover:text-blue-700">
              바로가기 <ArrowRight size={16} className="ml-1" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
