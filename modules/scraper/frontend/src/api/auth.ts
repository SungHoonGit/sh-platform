import { apiFetch, clearTokens, getAccessToken, getRefreshToken } from "@sh-platform/core";

export interface UserProfile {
  id: number;
  email: string;
  name: string;
  role: string;
  provider: string | null;
  emailVerified: boolean;
  locale: string;
  createdAt: string;
  updatedAt: string;
}

export async function fetchProfile(): Promise<UserProfile> {
  const token = getAccessToken();
  if (!token) throw new Error("Not authenticated");
  const res = await apiFetch("/api/v1/auth/me", { redirectOn401: true });
  if (!res.ok) throw new Error("Failed to fetch profile");
  const json = await res.json();
  return json.data;
}

export function logout() {
  const accessToken = getAccessToken();
  const refreshToken = getRefreshToken();
  clearTokens();
  fetch("/api/v1/auth/logout", {
    method: "POST",
    headers: {
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  }).catch(() => {});
}
