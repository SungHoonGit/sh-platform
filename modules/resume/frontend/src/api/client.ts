const API_BASE = "/resume/api/v1";

interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

function authHeaders(): HeadersInit {
  const token = localStorage.getItem("accessToken");
  if (!token) throw new Error("UNAUTHORIZED");
  return { Authorization: `Bearer ${token}`, "Content-Type": "application/json" };
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method,
      headers: authHeaders(),
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error("NETWORK_ERROR");
  }
  if (res.status === 401) throw new Error("UNAUTHORIZED");
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const json: ApiResponse<T> = await res.json();
  return json.data;
}

export function apiGet<T>(path: string): Promise<T> {
  return request<T>("GET", path);
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return request<T>("POST", path, body);
}

export function apiPut<T>(path: string, body: unknown): Promise<T> {
  return request<T>("PUT", path, body);
}

export function apiDelete<T = null>(path: string): Promise<T> {
  return request<T>("DELETE", path);
}

export function logout(): void {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  window.location.href = "/";
}
