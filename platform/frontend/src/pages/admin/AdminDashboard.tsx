import { useQuery } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { Users, Building2, UserCheck, UsersRound, LogIn, ShieldAlert } from "lucide-react";

const statCards = [
  { key: "totalUsers" as const, label: "전체 사용자", icon: Users, color: "bg-blue-500" },
  { key: "totalTenants" as const, label: "전체 테넌트", icon: Building2, color: "bg-green-500" },
  { key: "activeTenants" as const, label: "활성 테넌트", icon: UserCheck, color: "bg-emerald-500" },
  { key: "totalMembers" as const, label: "전체 멤버", icon: UsersRound, color: "bg-purple-500" },
];

export default function AdminDashboard() {
  const { data: stats, isLoading, error } = useQuery({
    queryKey: ["admin-stats"],
    queryFn: adminApi.getStats,
  });

  const { data: analytics } = useQuery({
    queryKey: ["admin-analytics"],
    queryFn: adminApi.getAnalytics,
  });

  if (error) {
    return (
      <div className="p-8">
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700">
          ADMIN 권한이 필요합니다.
        </div>
      </div>
    );
  }

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">관리자 대시보드</h1>
      <p className="text-slate-500 mb-8">플랫폼 전체 현황</p>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {statCards.map((card) => (
          <div key={card.key} className="bg-white rounded-xl p-6 shadow-sm border border-slate-200">
            <div className="flex items-center gap-4">
              <div className={`w-12 h-12 ${card.color} rounded-xl flex items-center justify-center`}>
                <card.icon className="text-white" size={24} />
              </div>
              <div>
                <p className="text-sm text-slate-500">{card.label}</p>
                <p className="text-2xl font-bold text-slate-800">
                  {isLoading ? "-" : stats?.[card.key] ?? 0}
                </p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <h2 className="text-lg font-bold text-slate-800 mt-10 mb-4">오늘 로그인 현황</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-xl">
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex items-center gap-4">
          <div className="w-12 h-12 bg-emerald-500 rounded-xl flex items-center justify-center">
            <LogIn className="text-white" size={24} />
          </div>
          <div>
            <p className="text-sm text-slate-500">로그인 성공</p>
            <p className="text-2xl font-bold text-emerald-600">{analytics?.todaySuccessCount ?? 0}</p>
          </div>
        </div>
        <div className="bg-white rounded-xl p-6 shadow-sm border border-slate-200 flex items-center gap-4">
          <div className="w-12 h-12 bg-red-400 rounded-xl flex items-center justify-center">
            <ShieldAlert className="text-white" size={24} />
          </div>
          <div>
            <p className="text-sm text-slate-500">로그인 실패</p>
            <p className="text-2xl font-bold text-red-500">{analytics?.todayFailCount ?? 0}</p>
          </div>
        </div>
      </div>
    </div>
  );
}
