import type { AppMenu } from "@sh-platform/shell";

/** platform 단일 메뉴 소스 (DB화 대비 — GET /menus 스키마와 동일) */
export const platformMenu: AppMenu = {
  app: "platform",
  items: [
    { id: "platform.dashboard", label: "개요", href: "/platform", primary: true, order: 10 },
    { id: "platform.me.resumes", label: "내 이력서", href: "/resume/", section: "개인 서비스", order: 20 },
    { id: "platform.me.postings", label: "공고 탐색", href: "/resume/#/postings", section: "개인 서비스", order: 30 },
    { id: "platform.me.applications", label: "지원 관리", href: "/resume/#/applications", section: "개인 서비스", order: 40 },
    { id: "platform.me.account", label: "계정 설정", href: "/platform/account", section: "개인 서비스", order: 50 },
    { id: "platform.admin", label: "관리", href: "/platform/admin", primary: true, roles: ["ADMIN"], order: 60 },
    { id: "platform.admin.roles", label: "권한 관리", href: "/platform/admin/roles", section: "관리", roles: ["ADMIN"], order: 70 },
    { id: "platform.admin.users", label: "사용자 관리", href: "/platform/admin/users", section: "관리", roles: ["ADMIN"], order: 80 },
    { id: "platform.admin.tenants", label: "테넌트 관리", href: "/platform/admin/tenants", section: "관리", roles: ["ADMIN"], order: 90 },
    { id: "platform.admin.audit", label: "감사 로그", href: "/platform/admin/audit", section: "관리", roles: ["ADMIN"], order: 100 },
    { id: "platform.admin.sessions", label: "세션 관리", href: "/platform/admin/sessions", section: "관리", roles: ["ADMIN"], order: 110 },
    { id: "platform.admin.master", label: "마스터 관리", href: "/platform/admin/master", section: "관리", roles: ["ADMIN"], order: 120 },
    { id: "platform.tools.swagger-auth", label: "Swagger · Auth", href: "/swagger-ui/index.html", external: true, section: "도구", roles: ["ADMIN"], order: 130 },
    { id: "platform.tools.swagger-scraper", label: "Swagger · Scraper", href: "/scraper/swagger-ui/index.html", external: true, section: "도구", roles: ["ADMIN"], order: 140 },
    { id: "platform.tools.swagger-resume", label: "Swagger · Resume", href: "/resume/swagger-ui/index.html", external: true, section: "도구", roles: ["ADMIN"], order: 150 },
    { id: "platform.tools.javadoc", label: "Javadoc", href: "/javadoc/", external: true, section: "도구", roles: ["ADMIN"], order: 160 },
    { id: "platform.tools.test-reports", label: "테스트 리포트", href: "/test-reports/", external: true, section: "도구", roles: ["ADMIN"], order: 170 },
    { id: "platform.tools.schemaspy", label: "SchemaSpy", href: "/schemaSpy/", external: true, section: "도구", roles: ["ADMIN"], order: 180 },
  ],
};
