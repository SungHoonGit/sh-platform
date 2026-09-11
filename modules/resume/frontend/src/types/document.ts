export interface SectionItem {
  key: string;
  included: boolean;
  order: number;
  /** 이력서(문서)에서 숨길 항목 id 목록 (예: 자격증 hiddenItemIds) */
  hiddenItemIds?: number[];
}

export interface ResumeDocument {
  id: number;
  title: string;
  templateCode: string;
  primary: boolean;
  sectionConfig: string;
  createdAt: string;
  updatedAt: string;
}

export interface ShareLink {
  documentId: number;
  token: string;
  expiresAt: string | null;
  createdAt: string;
}
