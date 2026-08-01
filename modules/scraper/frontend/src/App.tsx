import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import AuthGuard from "./components/AuthGuard";
import Layout from "./components/Layout";
import Search from "./pages/Search";
import Schedule from "./pages/Schedule";
import Viewer from "./pages/Viewer";
import NotFound from "./pages/NotFound";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 5 * 60 * 1000, retry: 1 },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
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
    </QueryClientProvider>
  );
}
