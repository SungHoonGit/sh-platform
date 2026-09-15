/**
 * 마스터 데이터 (scraper 도메인) 관리 API 클라이언트.
 * scraper 백엔드의 /sites, /regions, /search-mappings 원시 JSON 엔드포인트를 호출한다.
 */
const token = () => localStorage.getItem("accessToken") ?? "";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`/scraper${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token() ? { Authorization: `Bearer ${token()}` } : {}),
      ...options?.headers,
    },
  });
  if (!res.ok) throw new Error(`API_ERROR_${res.status}`);
  const text = await res.text();
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

// ── 사이트 (site_definition) ─────────────────────────────────────

export interface SiteDefinition {
  id: number;
  siteName: string;
  displayName: string;
  baseUrl: string;
  isEnabled: boolean;
  displayOrder: number;
  icon?: string | null;
  color?: string | null;
  createdAt?: string;
}

export type SiteUpdateRequest = Partial<
  Pick<SiteDefinition, "displayName" | "baseUrl" | "isEnabled" | "displayOrder" | "icon" | "color">
>;

export const masterApi = {
  getSites: () => request<SiteDefinition[]>("/sites"),
  updateSite: (id: number, data: SiteUpdateRequest) =>
    request<SiteDefinition>(`/sites/${id}`, { method: "PUT", body: JSON.stringify(data) }),
};

// ── 지역 (region) ────────────────────────────────────────────────

export interface Region {
  id: number;
  name: string;
  displayOrder: number;
  isActive: boolean;
  createdAt?: string;
}

export interface RegionRequest {
  name: string;
  displayOrder?: number | null;
  isActive?: boolean | null;
}

export const regionApi = {
  getRegions: () => request<Region[]>("/regions"),
  createRegion: (data: RegionRequest) => request<Region>("/regions", { method: "POST", body: JSON.stringify(data) }),
  updateRegion: (id: number, data: RegionRequest) =>
    request<Region>(`/regions/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteRegion: (id: number) => request<void>(`/regions/${id}`, { method: "DELETE" }),
};

// ── 검색 매핑 (site_search_mapping) ──────────────────────────────

export type ValueType = "direct" | "mapped" | "range";

export interface SearchMapping {
  id: number;
  siteName: string;
  siteDisplayName: string;
  standardKey: string;
  urlParamName: string;
  valueType: ValueType;
  valueMapping: string | null;
  isEnabled: boolean;
  displayOrder: number;
  updatedAt?: string;
}

export interface SearchMappingRequest {
  siteDefinitionId?: number | null;
  standardKey?: string | null;
  urlParamName: string;
  valueType: ValueType;
  valueMapping?: string | null;
  isEnabled?: boolean | null;
  displayOrder?: number | null;
}

export const searchMappingApi = {
  listAll: () => request<SearchMapping[]>("/search-mappings"),
  listBySite: (siteName: string) => request<SearchMapping[]>(`/search-mappings?siteName=${encodeURIComponent(siteName)}`),
  create: (data: SearchMappingRequest) => request<SearchMapping>("/search-mappings", { method: "POST", body: JSON.stringify(data) }),
  update: (id: number, data: SearchMappingRequest) =>
    request<SearchMapping>(`/search-mappings/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  delete: (id: number) => request<void>(`/search-mappings/${id}`, { method: "DELETE" }),
};