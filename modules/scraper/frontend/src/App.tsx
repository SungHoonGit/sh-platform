import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import AuthGuard from "./components/AuthGuard";
import Layout from "./components/Layout";
import Search from "./pages/Search";
import Schedule from "./pages/Schedule";
import Viewer from "./pages/Viewer";
import NotFound from "./pages/NotFound";
import { CrawlProgressProvider } from "./contexts/CrawlProgressContext";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 5 * 60 * 1000, retry: 1 },
  },
});

function registerServiceWorker() {
  if ("serviceWorker" in navigator && "PushManager" in window) {
    navigator.serviceWorker
      .register("/scraper/sw.js")
      .then((reg) => console.log("SW registered:", reg.scope))
      .catch((err) => console.error("SW registration failed:", err));
  }
}

export default function App() {
  useEffect(() => {
    registerServiceWorker();
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <CrawlProgressProvider>
        <AuthGuard>
          <BrowserRouter basename="/scraper">
            <Routes>
              <Route element={<Layout />}>
                <Route path="/" element={<Search />} />
                <Route path="/search" element={<Search />} />
                <Route path="/schedule" element={<Schedule />} />
                <Route path="/viewer" element={<Viewer />} />
                <Route path="*" element={<NotFound />} />
              </Route>
            </Routes>
          </BrowserRouter>
        </AuthGuard>
      </CrawlProgressProvider>
    </QueryClientProvider>
  );
}
