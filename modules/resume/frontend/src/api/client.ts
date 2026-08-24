const API_BASE = "/resume/api/v1";

interface ApiResponse<T> {
  code: string;
  message: string;
  data: T;
}

export async function apiGet<T>(path: string): Promise<T> {
  const token = localStorage.getItem("accessToken");
  if (!token) throw new Error("UNAUTHORIZED");
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const json: ApiResponse<T> = await res.json();
  return json.data;
}
