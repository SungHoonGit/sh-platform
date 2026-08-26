import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { LogOut, Search } from "lucide-react";

export default function AdminSessions() {
  const qc = useQueryClient();
  const [input, setInput] = useState("");
  const [targetUserId, setTargetUserId] = useState<number | null>(null);

  const enabled = targetUserId != null && !Number.isNaN(targetUserId);

  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-sessions", targetUserId],
    queryFn: () => adminApi.getUserSessions(targetUserId!),
    enabled,
    retry: false,
  });

  const logoutMutation = useMutation({
    mutationFn: (userId: number) => adminApi.forceLogout(userId),
    onSuccess: () => {
      if (targetUserId != null) {
        qc.invalidateQueries({ queryKey: ["admin-sessions", targetUserId] });
      }
    },
  });

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">세션 관리</h1>
      <p className="text-slate-500 mb-6">사용자별 활성 세션 조회 및 강제 로그아웃</p>

      <div className="flex gap-3 mb-6">
        <div className="relative max-w-xs w-full">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            inputMode="numeric"
            placeholder="사용자 ID 입력"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && setTargetUserId(Number(input))}
            className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
        <button
          onClick={() => setTargetUserId(Number(input))}
          disabled={!input.trim()}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white text-sm font-medium rounded-lg"
        >
          조회
        </button>
      </div>

      {!enabled ? (
        <div className="bg-white rounded-xl border border-dashed border-slate-300 p-12 text-center text-sm text-slate-400">
          사용자 ID를 입력해 활성 세션을 조회하세요.
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700 text-sm">
          세션 조회에 실패했습니다. 존재하지 않는 사용자이거나 ADMIN 권한이 필요합니다.
        </div>
      ) : isLoading ? (
        <div className="text-slate-400 text-sm p-8 text-center">불러오는 중...</div>
      ) : data ? (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-sm text-slate-500">대상 사용자</p>
              <p className="text-2xl font-bold text-slate-800">#{data.userId}</p>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-slate-200">
              <p className="text-sm text-slate-500">활성 세션 수</p>
              <p className={`text-2xl font-bold ${data.activeCount > 0 ? "text-emerald-600" : "text-slate-400"}`}>
                {data.activeCount}
              </p>
            </div>
            <div className="bg-white rounded-xl p-5 shadow-sm border border-red-200 flex items-center justify-between">
              <div>
                <p className="text-sm text-slate-500">강제 로그아웃</p>
                <p className="text-xs text-slate-400 mt-0.5">모든 기기 세션 삭제</p>
              </div>
              <button
                onClick={() => {
                  if (window.confirm(`사용자 #${data.userId}의 모든 세션을 삭제할까요?`)) {
                    logoutMutation.mutate(data.userId);
                  }
                }}
                disabled={data.activeCount === 0 || logoutMutation.isPending}
                className="flex items-center gap-1.5 px-3 py-2 bg-red-50 hover:bg-red-100 text-red-600 text-sm font-medium rounded-lg disabled:opacity-40"
              >
                <LogOut size={14} />
                실행
              </button>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500 border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-3 font-medium">#</th>
                  <th className="px-4 py-3 font-medium">세션 ID</th>
                </tr>
              </thead>
              <tbody>
                {data.sessionIds.length === 0 ? (
                  <tr>
                    <td colSpan={2} className="px-4 py-8 text-center text-slate-400">
                      활성 세션이 없습니다.
                    </td>
                  </tr>
                ) : (
                  data.sessionIds.map((sid, i) => (
                    <tr key={sid} className="border-b border-slate-100 last:border-0">
                      <td className="px-4 py-3 text-slate-400">{i + 1}</td>
                      <td className="px-4 py-3 font-mono text-xs">{sid}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {logoutMutation.isSuccess && (
            <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-emerald-700 text-sm">
              강제 로그아웃이 실행되었습니다. 해당 행위는 감사 로그에 기록됩니다.
            </div>
          )}
        </div>
      ) : null}
    </div>
  );
}
