import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import type { AuditLogItem } from "../../api/admin";
import { ChevronLeft, ChevronRight, ShieldAlert } from "lucide-react";

const actions = ["", "ROLE_CHANGE", "DELETE_USER", "FORCE_LOGOUT"];

const actionLabel: Record<string, string> = {
  ROLE_CHANGE: "권한 변경",
  DELETE_USER: "사용자 삭제",
  FORCE_LOGOUT: "강제 로그아웃",
};

const actionColor: Record<string, string> = {
  ROLE_CHANGE: "bg-amber-100 text-amber-700",
  DELETE_USER: "bg-red-100 text-red-700",
  FORCE_LOGOUT: "bg-blue-100 text-blue-700",
};

export default function AdminAudit() {
  const [action, setAction] = useState("");
  const [actorInput, setActorInput] = useState("");
  const [actorUserId, setActorUserId] = useState<number | undefined>(undefined);
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-audit", action, actorUserId, page],
    queryFn: () => adminApi.getAuditLogs({ action: action || undefined, actorUserId, page, size }),
  });

  const applyActorFilter = () => {
    const n = Number(actorInput);
    setActorUserId(actorInput.trim() && !Number.isNaN(n) ? n : undefined);
    setPage(0);
  };

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">감사 로그</h1>
      <p className="text-slate-500 mb-6">관리자 행위 이력 (권한 변경 · 사용자 삭제 · 강제 로그아웃)</p>

      <div className="flex gap-3 mb-6">
        <select
          value={action}
          onChange={(e) => { setAction(e.target.value); setPage(0); }}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
        >
          {actions.map((a) => (
            <option key={a} value={a}>{a ? actionLabel[a] ?? a : "전체 행위"}</option>
          ))}
        </select>
        <input
          type="text"
          placeholder="관리자 ID로 필터"
          value={actorInput}
          onChange={(e) => setActorInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && applyActorFilter()}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm w-48"
        />
        <button
          onClick={applyActorFilter}
          className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white text-sm font-medium rounded-lg"
        >
          필터 적용
        </button>
      </div>

      {error ? (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-red-700">ADMIN 권한이 필요합니다.</div>
      ) : isLoading ? (
        <div className="text-slate-400 text-sm p-8 text-center">불러오는 중...</div>
      ) : !data || data.content.length === 0 ? (
        <div className="text-slate-400 text-sm p-12 text-center bg-white rounded-xl border border-slate-200">
          감사 로그가 없습니다.
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-slate-500 border-b border-slate-200 bg-slate-50">
                  <th className="px-4 py-3 font-medium">일시</th>
                  <th className="px-4 py-3 font-medium">행위</th>
                  <th className="px-4 py-3 font-medium">관리자</th>
                  <th className="px-4 py-3 font-medium">대상</th>
                  <th className="px-4 py-3 font-medium">변경 내용</th>
                  <th className="px-4 py-3 font-medium">IP</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((log: AuditLogItem) => (
                  <tr key={log.id} className="border-b border-slate-100 hover:bg-slate-50">
                    <td className="px-4 py-3 whitespace-nowrap">{log.createdAt ?? "-"}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${actionColor[log.action] ?? "bg-slate-100 text-slate-600"}`}>
                        {actionLabel[log.action] ?? log.action}
                      </span>
                    </td>
                    <td className="px-4 py-3">#{log.actorUserId}</td>
                    <td className="px-4 py-3">{log.targetUserId != null ? `#${log.targetUserId}` : "-"}</td>
                    <td className="px-4 py-3 max-w-xs truncate">
                      {log.beforeValue || log.afterValue ? (
                        <span className="flex items-center gap-1.5">
                          <span className="text-slate-400 line-through">{log.beforeValue ?? "-"}</span>
                          <span aria-hidden>→</span>
                          <span className="font-medium text-slate-700">{log.afterValue ?? "-"}</span>
                        </span>
                      ) : (
                        "-"
                      )}
                    </td>
                    <td className="px-4 py-3 text-slate-400">{log.ip ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-slate-500">
              총 {data.totalElements.toLocaleString()}건
            </p>
            <div className="flex items-center gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
                className="p-2 border border-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-50"
              >
                <ChevronLeft size={16} />
              </button>
              <span className="text-sm text-slate-600 px-2">
                {page + 1} / {Math.max(data.totalPages, 1)}
              </span>
              <button
                disabled={page + 1 >= data.totalPages}
                onClick={() => setPage((p) => p + 1)}
                className="p-2 border border-slate-300 rounded-lg disabled:opacity-40 hover:bg-slate-50"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </>
      )}

      <div className="mt-6 flex items-start gap-2 text-xs text-slate-400">
        <ShieldAlert size={14} className="shrink-0 mt-0.5" />
        <p>감사 로그는 관리자 권한 변경·삭제·강제 로그아웃 시 자동 기록되며 임의 삭제할 수 없습니다.</p>
      </div>
    </div>
  );
}
