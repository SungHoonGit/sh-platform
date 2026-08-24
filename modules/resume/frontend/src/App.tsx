import { useEffect, useState } from "react";
import CommonHeader from "./components/CommonHeader";
import ResumesPage from "./pages/ResumesPage";
import ResumeViewPage from "./pages/ResumeViewPage";
import EditPage from "./pages/EditPage";
import ApplicationsPage from "./pages/ApplicationsPage";

type Route =
  | { name: "resumes" }
  | { name: "view"; documentId: number }
  | { name: "edit"; documentId: number }
  | { name: "applications" };

function parseHash(): Route {
  const h = window.location.hash.replace(/^#/, "");
  const viewMatch = /^\/r\/(\d+)$/.exec(h);
  if (viewMatch) return { name: "view", documentId: Number(viewMatch[1]) };
  const editMatch = /^\/r\/(\d+)\/edit$/.exec(h);
  if (editMatch) return { name: "edit", documentId: Number(editMatch[1]) };
  if (h === "/applications") return { name: "applications" };
  return { name: "resumes" };
}

export default function App() {
  const [route, setRoute] = useState<Route>(parseHash);

  useEffect(() => {
    const onHash = () => setRoute(parseHash());
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  const loggedIn = Boolean(localStorage.getItem("accessToken"));
  const tab = route.name === "applications" ? "applications" : "resumes";

  let page;
  if (route.name === "view") page = <ResumeViewPage documentId={route.documentId} />;
  else if (route.name === "edit") page = <EditPage documentId={route.documentId} />;
  else if (route.name === "applications") page = <ApplicationsPage />;
  else page = <ResumesPage />;

  return (
    <div className="min-h-screen flex flex-col">
      <CommonHeader tab={tab} loggedIn={loggedIn} />
      <main className="flex-1">{page}</main>
    </div>
  );
}
