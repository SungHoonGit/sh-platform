import { useEffect, useState } from "react";
import { CalendarDays, FileText, Search } from "lucide-react";
import AppShell from "./shell/AppShell";
import type { DrawerSection } from "./shell/SideDrawer";
import ResumesPage from "./pages/ResumesPage";
import ResumeViewPage from "./pages/ResumeViewPage";
import EditPage from "./pages/EditPage";
import ApplicationsPage from "./pages/ApplicationsPage";
import PostingsBrowsePage from "./pages/PostingsBrowsePage";
import ShareViewPage from "./pages/ShareViewPage";
import { apiGet } from "./api/client";
import { useAuth } from "./hooks/useAuth";

type Route =
  | { name: "resumes" }
  | { name: "view"; documentId: number }
  | { name: "edit"; documentId: number }
  | { name: "applications" }
  | { name: "postings" }
  | { name: "share"; token: string };

function parseHash(): Route {
  const h = window.location.hash.replace(/^#/, "");
  const viewMatch = /^\/r\/(\d+)$/.exec(h);
  if (viewMatch) return { name: "view", documentId: Number(viewMatch[1]) };
  const editMatch = /^\/r\/(\d+)\/edit$/.exec(h);
  if (editMatch) return { name: "edit", documentId: Number(editMatch[1]) };
  const shareMatch = /^\/s\/([A-Za-z0-9-]+)$/.exec(h);
  if (shareMatch) return { name: "share", token: shareMatch[1] };
  if (h === "/applications") return { name: "applications" };
  if (h === "/postings") return { name: "postings" };
  return { name: "resumes" };
}

export default function App() {
  const [route, setRoute] = useState<Route>(parseHash);
  const [docs, setDocs] = useState<{ id: number; title: string }[]>([]);
  const { user, loading, logout } = useAuth();

  useEffect(() => {
    const onHash = () => setRoute(parseHash());
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  const loggedIn = Boolean(localStorage.getItem("accessToken"));

  // 사이드바 동적 메뉴용 내 이력서 목록
  useEffect(() => {
    if (!loggedIn) return;
    apiGet<{ id: number; title: string }[]>("/documents")
      .then(setDocs)
      .catch(() => {});
  }, [loggedIn, route.name]);

  const tab =
    route.name === "applications"
      ? "applications"
      : route.name === "postings"
        ? "postings"
        : "resumes";

  const subnavItems = [
    {
      label: "이력서 관리",
      href: "#/resumes",
      icon: FileText,
      active: route.name === "resumes" || route.name === "view" || route.name === "edit",
    },
    { label: "공고 탐색", href: "#/postings", icon: Search, active: tab === "postings" },
    { label: "지원 관리", href: "#/applications", icon: CalendarDays, active: tab === "applications" },
  ];

  const drawerSections: DrawerSection[] = [
    {
      label: "내 이력서",
      items:
        docs.length > 0
          ? docs.slice(0, 8).map((d) => ({ label: d.title || `이력서 #${d.id}`, href: `#/r/${d.id}` }))
          : [{ label: "이력서 관리에서 생성", href: "#/resumes" }],
    },
    {
      label: "탐색·지원",
      items: [
        { label: "공고 탐색", href: "#/postings" },
        { label: "지원 관리", href: "#/applications" },
      ],
    },
    {
      label: "개발자 링크",
      items: [{ label: "Swagger · Resume", href: "/resume/swagger-ui/index.html", external: true }],
    },
  ];

  let page;
  if (route.name === "share") return <ShareViewPage token={route.token} />;
  if (route.name === "view") page = <ResumeViewPage documentId={route.documentId} />;
  else if (route.name === "edit") page = <EditPage documentId={route.documentId} />;
  else if (route.name === "applications") page = <ApplicationsPage />;
  else if (route.name === "postings") page = <PostingsBrowsePage />;
  else page = <ResumesPage />;

  return (
    <AppShell
      currentApp="resume"
      subnavItems={subnavItems}
      drawerSections={drawerSections}
      user={user ? { name: user.name, email: user.email } : null}
      authLoading={loading}
      isAdmin={user?.role === "ADMIN"}
      onLogout={logout}
      mainClassName="flex-1 bg-slate-50"
    >
      <div className="min-h-full">{page}</div>
    </AppShell>
  );
}
