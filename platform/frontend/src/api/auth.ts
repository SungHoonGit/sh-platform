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
  const token = localStorage.getItem("accessToken");
  if (!token) throw new Error("Not authenticated");
  const res = await fetch("/api/v1/auth/me", {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error("Failed to fetch profile");
  const json = await res.json();
  return json.data;
}

export function logout() {
  const refreshToken = localStorage.getItem("refreshToken");
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  fetch("/api/v1/auth/logout", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  }).catch(() => {});
}
