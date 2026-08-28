import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App";
import ErrorBoundary from "./components/ErrorBoundary";
import { DialogHost, initGlobalDialogs } from "@sh-platform/ui";

initGlobalDialogs();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
      <DialogHost />
    </ErrorBoundary>
  </StrictMode>
);
