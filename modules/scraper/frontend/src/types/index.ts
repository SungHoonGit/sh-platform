export interface SiteConfig {
  siteName: string;
  displayName: string;
  baseUrl: string;
  isEnabled: boolean;
  paramValues: string;
}

export interface Crawler {
  id: number;
  name: string;
  localPath: string;
  schedule: string;
  siteConfigs: SiteConfig[];
}

export interface JobItem {
  site: string;
  company: string;
  position: string;
  career?: string;
  tech?: string;
  location?: string;
  deadline?: string;
  url: string;
}

export interface JobsResponse {
  jobs: JobItem[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
  sites: string[];
  currentSite: string;
}

export interface SearchFilters {
  keyword: string;
  category: string;
  sites: string[];
  career: string;
  location: string;
}
