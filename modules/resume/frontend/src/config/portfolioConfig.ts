import type { FieldDef } from "../components/CrudSection";

export const PORTFOLIO_ENDPOINT = "/portfolio-items";
export const PORTFOLIO_LIST_KEY = "portfolioItems";

export const PORTFOLIO_FIELDS: FieldDef[] = [
  { key: "title", label: "작업물 제목", required: true },
  { key: "role", label: "맡은 역할" },
  { key: "startDate", label: "기간", type: "dateRange", endKey: "endDate" },
  { key: "techStack", label: "기술 스택", type: "skill", maxLength: 300, placeholder: "기술명 입력 후 Enter (예: Java, Spring, MySQL)" },
  {
    key: "thumbnailPath",
    label: "썸네일 이미지 (jpg/png, 5MB 이하)",
    type: "file",
    accept: ".jpg,.jpeg,.png",
    image: true,
  },
  { key: "githubUrl", label: "GitHub 링크", placeholder: "https://github.com/user/repo" },
  { key: "demoUrl", label: "데모/배포 링크", placeholder: "https://your-demo.example.com" },
  { key: "videoUrl", label: "시연 영상 링크", placeholder: "https://youtube.com/watch?v=..." },
  {
    key: "filePath",
    label: "첨부파일 — pdf/pptx/docx/png/jpg, 10MB 이하",
    type: "file",
    accept: ".pdf,.pptx,.ppt,.docx,.png,.jpg,.jpeg",
  },
  { key: "description", label: "설명 (마크다운)", type: "markdown" },
];