const API_BASE = "/resume/api/v1";
const SHARE_BASE = "/resume/share";

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
  const name = parseDownloadName(disposition) ?? fallbackName;
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

function parseDownloadName(disposition: string): string | null {
  const rfc5987 = /filename\*=(?:UTF-8''|")?([^";]+)/i.exec(disposition);
  if (rfc5987) {
    try {
      return decodeURIComponent(rfc5987[1].trim());
    } catch {
      // 잘못된 퍼센트 인코딩이면 아래 폴백으로
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition);
  if (!plain) return null;
  return decodeEncodedWord(plain[1].trim());
}

function decodeEncodedWord(value: string): string {
  const m = /=\?([^?]+)\?([bq])\?([^?]*)\?=/i.exec(value);
  if (!m) return value;
  const encoding = m[2].toLowerCase();
  const body = m[3];
  try {
    if (encoding === "q") {
      const bytes: number[] = [];
      for (let i = 0; i < body.length; i++) {
        if (body[i] === "_") {
          bytes.push(0x20);
        } else if (body[i] === "=" && i + 2 < body.length) {
          bytes.push(parseInt(body.slice(i + 1, i + 3), 16));
          i += 2;
        } else {
          bytes.push(body.charCodeAt(i));
        }
      }
      return new TextDecoder("utf-8").decode(new Uint8Array(bytes));
    }
    const binary = atob(body);
    const bytes = Array.from(binary, (c) => c.charCodeAt(0));
    return new TextDecoder("utf-8").decode(new Uint8Array(bytes));
  } catch {
    return value;
  }
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

/**
 * 공개 공유 엔드포인트 조회 (인증 없이 접근). path는 "/{token}" 형태여야 한다.
 */
export async function apiGetShare<T>(path: string): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${SHARE_BASE}${path}`, {
      headers: { "Content-Type": "application/json" },
    });
  } catch {
    throw new Error("NETWORK_ERROR");
  }
  if (res.status === 404) throw new Error("NOT_FOUND");
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const json: ApiResponse<T> = await res.json();
  return json.data;
}

/**
 * 공개 공유 이력서 PDF 다운로드 (인증 없이 접근).
 */
export async function apiDownloadShare(path: string, fallbackName = "download"): Promise<void> {
  let res: Response;
  try {
    res = await fetch(`${SHARE_BASE}${path}`);
  } catch {
    throw new Error("NETWORK_ERROR");
  }
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const disposition = res.headers.get("Content-Disposition") ?? "";
  const name = parseDownloadName(disposition) ?? fallbackName;
  const url = URL.createObjectURL(await res.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  URL.revokeObjectURL(url);
}

export function logout(): void {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  window.location.href = "/";
}
