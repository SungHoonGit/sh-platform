export interface Profile {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  address: string | null;
  birthDate: string | null;
  photoUrl: string | null;
  headline: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Career {
  id: number;
  company: string;
  title: string;
  startDate: string;
  endDate: string | null;
  description: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface Education {
  id: number;
  school: string;
  major: string | null;
  degree: string | null;
  startDate: string;
  endDate: string | null;
  status: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface Skill {
  id: number;
  name: string;
  level: string | null;
  category: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface Certificate {
  id: number;
  name: string;
  issuer: string | null;
  acquiredAt: string;
  displayOrder: number;
  createdAt: string;
}

export interface Project {
  id: number;
  name: string;
  role: string | null;
  startDate: string;
  endDate: string | null;
  description: string | null;
  techStack: string | null;
  linkUrl: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface Introduction {
  id: number;
  title: string;
  content: string;
  displayOrder: number;
  createdAt: string;
}

export interface PortfolioItem {
  id: number;
  title: string;
  itemType: string;
  filePath: string | null;
  linkUrl: string | null;
  description: string | null;
  displayOrder: number;
  createdAt: string;
}

export interface ResumeView {
  profile: Profile | null;
  careers: Career[];
  educations: Education[];
  skills: Skill[];
  certificates: Certificate[];
  projects: Project[];
  introductions: Introduction[];
  portfolioItems: PortfolioItem[];
  generatedAt: string;
}
