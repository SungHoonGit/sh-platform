import { create } from "zustand";
import type { Crawler } from "../types";

interface CrawlerState {
  selectedCrawler: Crawler | null;
  selectedFile: string | null;
  currentSite: string;
  currentPage: number;
  executing: boolean;

  setSelectedCrawler: (crawler: Crawler | null) => void;
  setSelectedFile: (path: string | null) => void;
  setCurrentSite: (site: string) => void;
  setCurrentPage: (page: number) => void;
  setExecuting: (v: boolean) => void;
}

export const useCrawlerStore = create<CrawlerState>((set) => ({
  selectedCrawler: null,
  selectedFile: null,
  currentSite: "all",
  currentPage: 0,
  executing: false,

  setSelectedCrawler: (crawler) =>
    set({ selectedCrawler: crawler, selectedFile: null, currentSite: "all", currentPage: 0 }),
  setSelectedFile: (path) => set({ selectedFile: path, currentSite: "all", currentPage: 0 }),
  setCurrentSite: (site) => set({ currentSite: site, currentPage: 0 }),
  setCurrentPage: (page) => set({ currentPage: page }),
  setExecuting: (v) => set({ executing: v }),
}));
