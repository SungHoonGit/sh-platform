import { useEffect, useState } from "react";
import ResumeViewPage from "./pages/ResumeViewPage";
import EditPage from "./pages/EditPage";
import CommonHeader from "./components/CommonHeader";

type Mode = "view" | "edit";

function currentMode(): Mode {
  return window.location.hash === "#edit" ? "edit" : "view";
}

export default function App() {
  const [mode, setMode] = useState<Mode>(currentMode);

  useEffect(() => {
    const onHash = () => setMode(currentMode());
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  const loggedIn = Boolean(localStorage.getItem("accessToken"));

  return (
    <div className="min-h-screen flex flex-col">
      <CommonHeader loggedIn={loggedIn} />
      <main className="flex-1">{mode === "edit" ? <EditPage /> : <ResumeViewPage />}</main>
    </div>
  );
}
