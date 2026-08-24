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

export async function apiUpload<T>(path: string, file: File, fieldName = "file"): Promise<T> {
  const token = localStorage.getItem("accessToken");
  if (!token) throw new Error("UNAUTHORIZED");
  const fd = new FormData();
  fd.append(fieldName, file);
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      method: "POST",
      headers: { Authorization: `Bearer ${token}` },
      body: fd,
    });
  } catch {
    throw new Error("NETWORK_ERROR");
  }
  if (res.status === 401) throw new Error("UNAUTHORIZED");
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const json: ApiResponse<T> = await res.json();
  return json.data;
}

export async function apiDownload(path: string, fallbackName = "download"): Promise<void> {
  const token = localStorage.getItem("accessToken");
  if (!token) throw new Error("UNAUTHORIZED");
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (res.status === 401) throw new Error("UNAUTHORIZED");
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const match = /filename\*?=(?:UTF-8''|")?([^";]+)/i.exec(disposition);
  const name = match ? decodeURIComponent(match[1].replace(/"/g, "")) : fallbackName;
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

export function fileDownloadPath(filePath: string): string {
  return filePath.startsWith("/api/v1/") ? filePath.replace(/^\/api\/v1/, "") : filePath;
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
