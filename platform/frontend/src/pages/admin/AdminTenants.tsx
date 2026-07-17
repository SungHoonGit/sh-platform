import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import type { TenantListItem } from "../../api/admin";
import { Search, Trash2, ChevronLeft, ChevronRight, Eye } from "lucide-react";

const planTypes = ["", "FREE", "BASIC", "PRO", "ENTERPRISE"];

export default function AdminTenants() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [planFilter, setPlanFilter] = useState("");
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const size = 20;

  const { data, isLoading } = useQuery({
    queryKey: ["admin-tenants", search, planFilter, page],
    queryFn: () => adminApi.getTenants({ search: search || undefined, planType: planFilter || undefined, page, size }),
  });

  const { data: detail } = useQuery({
    queryKey: ["admin-tenant-detail", selectedId],
    queryFn: () => adminApi.getTenant(selectedId!),
    enabled: selectedId !== null,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminApi.deleteTenant(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["admin-tenants"] }); setSelectedId(null); },
  });

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">테넌트 관리</h1>
      <p className="text-slate-500 mb-6">전체 테넌트를 관리합니다</p>

      <div className="flex gap-3 mb-6">
        <div className="relative flex-1 max-w-md">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="테넌트 이름 검색"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
        <select
          value={planFilter}
          onChange={(e) => { setPlanFilter(e.target.value); setPage(0); }}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
        >
          <option value="">전체 플랜</option>
          {planTypes.filter(Boolean).map((p) => (
            <option key={p} value={p}>{p}</option>
          ))}
        </select>
      </div>

      <div className="flex gap-6">
        <div className={`${selectedId ? "w-1/2" : "w-full"} transition-all`}>
          <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200">
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">ID</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">이름</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">슬러그</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">플랜</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">상태</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">멤버</th>
                  <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">관리</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {isLoading ? (
                  <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400">로딩 중...</td></tr>
                ) : data?.content?.length === 0 ? (
                  <tr><td colSpan={7} className="px-4 py-8 text-center text-slate-400">테넌트가 없습니다</td></tr>
                ) : (
                  data?.content?.map((t: TenantListItem) => (
                    <tr key={t.id} className={`hover:bg-slate-50 cursor-pointer ${selectedId === t.id ? "bg-blue-50" : ""}`}
                        onClick={() => setSelectedId(t.id)}>
                      <td className="px-4 py-3 text-sm text-slate-600">{t.id}</td>
                      <td className="px-4 py-3 text-sm font-medium text-slate-800">{t.name}</td>
                      <td className="px-4 py-3 text-sm text-slate-500">{t.slug}</td>
                      <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-1 rounded-full ${
                          t.planType === "FREE" ? "bg-slate-100 text-slate-600" :
                          t.planType === "BASIC" ? "bg-blue-100 text-blue-700" :
                          t.planType === "PRO" ? "bg-purple-100 text-purple-700" :
                          "bg-amber-100 text-amber-700"
                        }`}>{t.planType}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className={`text-xs px-2 py-1 rounded-full ${
                          t.status === "ACTIVE" ? "bg-green-100 text-green-700" :
                          t.status === "SUSPENDED" ? "bg-red-100 text-red-700" :
                          "bg-slate-100 text-slate-600"
                        }`}>{t.status}</span>
                      </td>
                      <td className="px-4 py-3 text-sm text-slate-600">{t.memberCount}/{t.maxUsers}</td>
                      <td className="px-4 py-3">
                        <div className="flex gap-1">
                          <button onClick={(e) => { e.stopPropagation(); setSelectedId(t.id); }}
                                  className="text-blue-500 hover:text-blue-700 p-1"><Eye size={16} /></button>
                          <button onClick={(e) => {
                            e.stopPropagation();
                            if (confirm(`테넌트 ${t.name}을(를) 삭제하시겠습니까?`))
                              deleteMutation.mutate(t.id);
                          }} className="text-red-500 hover:text-red-700 p-1"><Trash2 size={16} /></button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <span className="text-sm text-slate-500">
                총 {data.totalElements}건 / 페이지 {data.number + 1} / {data.totalPages}
              </span>
              <div className="flex gap-2">
                <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
                        className="px-3 py-1 border rounded text-sm disabled:opacity-50"><ChevronLeft size={16} /></button>
                <button onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                        disabled={page >= data.totalPages - 1}
                        className="px-3 py-1 border rounded text-sm disabled:opacity-50"><ChevronRight size={16} /></button>
              </div>
            </div>
          )}
        </div>

        {selectedId && detail && (
          <div className="w-1/2">
            <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold text-slate-800">{detail.name}</h2>
                <button onClick={() => setSelectedId(null)} className="text-slate-400 hover:text-slate-600 text-sm">닫기</button>
              </div>
              <div className="grid grid-cols-2 gap-4 mb-6 text-sm">
                <div><span className="text-slate-500">슬러그:</span> <span className="font-medium">{detail.slug}</span></div>
                <div><span className="text-slate-500">플랜:</span> <span className="font-medium">{detail.planType}</span></div>
                <div><span className="text-slate-500">상태:</span> <span className="font-medium">{detail.status}</span></div>
                <div><span className="text-slate-500">최대 사용자:</span> <span className="font-medium">{detail.maxUsers}</span></div>
                <div><span className="text-slate-500">생성일:</span> <span className="font-medium">{new Date(detail.createdAt).toLocaleDateString("ko-KR")}</span></div>
              </div>
              <h3 className="text-sm font-semibold text-slate-700 mb-3">멤버 ({detail.members.length})</h3>
              <div className="space-y-2">
                {detail.members.map((m) => (
                  <div key={m.userId} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg">
                    <div>
                      <span className="text-sm font-medium text-slate-800">{m.name}</span>
                      <span className="text-xs text-slate-500 ml-2">{m.email}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`text-xs px-2 py-0.5 rounded-full ${
                        m.role === "OWNER" ? "bg-amber-100 text-amber-700" :
                        m.role === "ADMIN" ? "bg-blue-100 text-blue-700" :
                        "bg-slate-100 text-slate-600"
                      }`}>{m.role}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
