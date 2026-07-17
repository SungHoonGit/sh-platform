import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import type { UserListItem } from "../../api/admin";
import { Search, Trash2, ChevronLeft, ChevronRight } from "lucide-react";

const roles = ["", "USER", "ADMIN"];

export default function AdminUsers() {
  const qc = useQueryClient();
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [page, setPage] = useState(0);
  const size = 20;

  const { data, isLoading } = useQuery({
    queryKey: ["admin-users", search, roleFilter, page],
    queryFn: () => adminApi.getUsers({ search: search || undefined, role: roleFilter || undefined, page, size }),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: number; role: string }) => adminApi.updateUserRole(id, role),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminApi.deleteUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  return (
    <div className="p-8">
      <h1 className="text-2xl font-bold text-slate-800 mb-1">사용자 관리</h1>
      <p className="text-slate-500 mb-6">전체 사용자를 관리합니다</p>

      <div className="flex gap-3 mb-6">
        <div className="relative flex-1 max-w-md">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            placeholder="이름 또는 이메일 검색"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            className="w-full pl-9 pr-4 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
        <select
          value={roleFilter}
          onChange={(e) => { setRoleFilter(e.target.value); setPage(0); }}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white"
        >
          <option value="">전체 역할</option>
          {roles.filter(Boolean).map((r) => (
            <option key={r} value={r}>{r}</option>
          ))}
        </select>
      </div>

      <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="bg-slate-50 border-b border-slate-200">
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">ID</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">이름</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">이메일</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">역할</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">제공자</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">이메일 인증</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">가입일</th>
              <th className="text-left px-4 py-3 text-xs font-medium text-slate-500 uppercase">관리</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading ? (
              <tr><td colSpan={8} className="px-4 py-8 text-center text-slate-400">로딩 중...</td></tr>
            ) : data?.content?.length === 0 ? (
              <tr><td colSpan={8} className="px-4 py-8 text-center text-slate-400">사용자가 없습니다</td></tr>
            ) : (
              data?.content?.map((u: UserListItem) => (
                <tr key={u.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 text-sm text-slate-600">{u.id}</td>
                  <td className="px-4 py-3 text-sm font-medium text-slate-800">{u.name}</td>
                  <td className="px-4 py-3 text-sm text-slate-600">{u.email}</td>
                  <td className="px-4 py-3">
                    <select
                      value={u.role}
                      onChange={(e) => roleMutation.mutate({ id: u.id, role: e.target.value })}
                      className="text-xs px-2 py-1 rounded-full border bg-white"
                    >
                      {roles.filter(Boolean).map((r) => (
                        <option key={r} value={r}>{r}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-500">{u.provider || "local"}</td>
                  <td className="px-4 py-3 text-sm">
                    {u.emailVerified ? (
                      <span className="text-green-600">인증됨</span>
                    ) : (
                      <span className="text-slate-400">미인증</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-sm text-slate-500">
                    {new Date(u.createdAt).toLocaleDateString("ko-KR")}
                  </td>
                  <td className="px-4 py-3">
                    <button
                      onClick={() => {
                        if (confirm(`사용자 ${u.name}을(를) 삭제하시겠습니까?`))
                          deleteMutation.mutate(u.id);
                      }}
                      className="text-red-500 hover:text-red-700 p-1"
                    >
                      <Trash2 size={16} />
                    </button>
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
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1 border rounded text-sm disabled:opacity-50"
            >
              <ChevronLeft size={16} />
            </button>
            <button
              onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
              disabled={page >= data.totalPages - 1}
              className="px-3 py-1 border rounded text-sm disabled:opacity-50"
            >
              <ChevronRight size={16} />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
