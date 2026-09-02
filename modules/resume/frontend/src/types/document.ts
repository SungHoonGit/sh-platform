export interface SectionItem {
  key: string;
  included: boolean;
  order: number;
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
