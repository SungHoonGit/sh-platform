import type { AppMenu } from "@sh-platform/shell";

/**
 * resume 단일 메뉴 소스 (DB화 대비 — GET /menus 스키마와 동일).
 * 동적 "내 이력서" 문서 섹션은 App.tsx에서 병합.
 */
export const resumeMenu: AppMenu = {
  app: "resume",
  items: [
    { id: "resume.resumes", label: "이력서 만들기", href: "#/resumes", primary: true, section: "탐색 메뉴", order: 10 },
    { id: "resume.portfolio", label: "프로젝트", href: "#/portfolio", primary: true, section: "탐색 메뉴", order: 20 },
    { id: "resume.postings", label: "채용 탐색", href: "#/postings", primary: true, section: "탐색 메뉴", order: 30 },
    { id: "resume.applications", label: "지원 현황", href: "#/applications", primary: true, section: "탐색 메뉴", order: 40 },
    { id: "resume.tools.swagger", label: "Swagger · Resume", href: "/resume/swagger-ui/index.html", external: true, section: "도구", roles: ["ADMIN"], order: 50 },
  ],
};
