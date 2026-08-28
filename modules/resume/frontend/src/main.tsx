import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";
import { DialogHost, initGlobalDialogs } from "@sh-platform/ui";

initGlobalDialogs();

createRoot(document.getElementById("root")!).render(
  <>
    <App />
    <DialogHost />
  </>
);
