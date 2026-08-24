import { useEffect, useState } from "react";
import ResumeViewPage from "./pages/ResumeViewPage";
import EditPage from "./pages/EditPage";

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

  return mode === "edit" ? <EditPage /> : <ResumeViewPage />;
}
