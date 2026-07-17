const API_BASE = "/api/v1/admin";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const token = localStorage.getItem("accessToken");
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  });
  if (res.status === 403) throw new Error("ADMIN 권한이 필요합니다");
  if (!res.ok) throw new Error(`API Error: ${res.status}`);
  const json = await res.json();
  return json.data ?? json;
}

export interface AdminStats {
  totalUsers: number;
  totalTenants: number;
  activeTenants: number;
  totalMembers: number;
}

export interface UserListItem {
  id: number;
  name: string;
  email: string;
  role: string;
  provider: string;
  emailVerified: boolean;
  createdAt: string;
}

export interface TenantListItem {
  id: number;
  name: string;
  slug: string;
  planType: string;
  status: string;
  maxUsers: number;
  memberCount: number;
  createdAt: string;
}

export interface TenantDetail {
  id: number;
  name: string;
  slug: string;
  planType: string;
  status: string;
  maxUsers: number;
  members: MemberInfo[];
  createdAt: string;
}

export interface MemberInfo {
  userId: number;
  name: string;
  email: string;
  role: string;
  status: string;
  joinedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export const adminApi = {
  getStats: () => request<AdminStats>("/stats"),
  getUsers: (params?: { search?: string; role?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.search) q.set("search", params.search);
    if (params?.role) q.set("role", params.role);
    q.set("page", String(params?.page ?? 0));
    q.set("size", String(params?.size ?? 20));
    return request<PageResponse<UserListItem>>(`/users?${q}`);
  },
  getUser: (id: number) => request<UserListItem>(`/users/${id}`),
  updateUserRole: (id: number, role: string) =>
    request<void>(`/users/${id}/role`, { method: "PUT", body: JSON.stringify({ role }) }),
  deleteUser: (id: number) => request<void>(`/users/${id}`, { method: "DELETE" }),
  getTenants: (params?: { search?: string; planType?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.search) q.set("search", params.search);
    if (params?.planType) q.set("planType", params.planType);
    q.set("page", String(params?.page ?? 0));
    q.set("size", String(params?.size ?? 20));
    return request<PageResponse<TenantListItem>>(`/tenants?${q}`);
  },
  getTenant: (id: number) => request<TenantDetail>(`/tenants/${id}`),
  updateTenant: (id: number, data: { planType: string; status: string }) =>
    request<void>(`/tenants/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteTenant: (id: number) => request<void>(`/tenants/${id}`, { method: "DELETE" }),
  inviteMember: (tenantId: number, email: string) =>
    request<void>(`/tenants/${tenantId}/members`, { method: "POST", body: JSON.stringify({ email }) }),
  removeMember: (tenantId: number, userId: number) =>
    request<void>(`/tenants/${tenantId}/members/${userId}`, { method: "DELETE" }),
  updateMemberRole: (tenantId: number, userId: number, role: string) =>
    request<void>(`/tenants/${tenantId}/members/${userId}/role`, { method: "PUT", body: JSON.stringify({ role }) }),
};
