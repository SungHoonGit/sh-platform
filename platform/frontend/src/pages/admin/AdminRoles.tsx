import { useQuery } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { Shield, ShieldAlert, ArrowRight } from "lucide-react";

const rolePolicies = [
  {
    role: "USER",
    title: "일반 사용자",
    icon: Shield,
    color: "bg-slate-500",
    description: "본인 소유 데이터만 접근 가능",
    permissions: [
      "내 프로필·계정 설정 관리",
      "내 크롤러 설정 및 스케줄 운영",
      "내 수집 공고 탐색 · 스크랩 · 지원 관리",
      "내 이력서 작성 및 파일 업로드",
      "동시 로그인 기기 3대까지 허용",
    ],
  },
  {
    role: "ADMIN",
    title: "관리자",
    icon: ShieldAlert,
    color: "bg-blue-600",
    description: "플랫폼 전체 운영 권한",
    permissions: [
      "사용자 목록 조회 · 역할 변경 · 삭제",
      "테넌트 생성/수정/삭제 및 멤버 관리",
      "사용자 세션 조회 · 강제 로그아웃",
      "감사 로그 · 로그인 애널리틱스 조회",
      "모든 행위는 감사 로그에 자동 기록됨",
    ],
  },
];

export default function AdminRoles() {
  const { data, isLoading } = useQuery({
    queryKey: ["admin-audit", "ROLE_CHANGE"],
    queryFn: () => adminApi.getAuditLogs({ action: "ROLE_CHANGE", size: 10 }),
  });

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">권한 관리</h1>
      <p className="text-slate-500 mb-8">역할 정책과 최근 권한 변경 이력</p>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-10">
        {rolePolicies.map((p) => (
          <div key={p.role} className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <div className={`px-6 py-4 flex items-center gap-3 ${p.color}`}>
              <p.icon className="text-white" size={22} />
              <div>
                <p className="text-white font-bold">{p.title}</p>
                <p className="text-white/70 text-xs">{p.description}</p>
              </div>
            </div>
            <ul className="p-6 space-y-2">
              {p.permissions.map((perm) => (
                <li key={perm} className="flex items-start gap-2 text-sm text-slate-600">
                  <span className="w-1.5 h-1.5 rounded-full bg-slate-300 mt-1.5 shrink-0" />
                  {perm}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <h2 className="text-lg font-bold text-slate-800 mb-3">보호 규칙</h2>
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-5 text-sm text-amber-800 space-y-1.5 mb-10">
        <p>· 자기 계정의 권한은 변경할 수 없습니다.</p>
        <p>· 관리자 페이지에서 자기 계정을 삭제할 수 없습니다. (계정 설정 메뉴 이용)</p>
        <p>· 마지막 남은 ADMIN의 권한은 회수할 수 없습니다.</p>
        <p>· 모든 변경은 감사 로그에 변경 전/후 값과 함께 기록됩니다.</p>
      </div>

      <h2 className="text-lg font-bold text-slate-800 mb-3">최근 권한 변경 이력</h2>
      {isLoading ? (
        <div className="text-slate-400 text-sm p-8 text-center bg-white rounded-xl border border-slate-200">불러오는 중...</div>
      ) : !data || data.content.length === 0 ? (
        <div className="text-slate-400 text-sm p-12 text-center bg-white rounded-xl border border-slate-200">
          권한 변경 이력이 없습니다.
        </div>
      ) : (
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-slate-500 border-b border-slate-200 bg-slate-50">
                <th className="px-4 py-3 font-medium">일시</th>
                <th className="px-4 py-3 font-medium">대상 사용자</th>
                <th className="px-4 py-3 font-medium">변경</th>
                <th className="px-4 py-3 font-medium">수행 관리자</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((log) => (
                <tr key={log.id} className="border-b border-slate-100 last:border-0 hover:bg-slate-50">
                  <td className="px-4 py-3 whitespace-nowrap">{log.createdAt ?? "-"}</td>
                  <td className="px-4 py-3">#{log.targetUserId}</td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center gap-1.5">
                      <span>{log.beforeValue ?? "-"}</span>
                      <ArrowRight size={13} className="text-slate-400" />
                      <span className="font-medium text-slate-700">{log.afterValue ?? "-"}</span>
                    </span>
                  </td>
                  <td className="px-4 py-3">#{log.actorUserId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="mt-3 text-xs text-slate-400">역할 변경은 사용자 관리 페이지에서 수행합니다.</p>
    </div>
  );
}
