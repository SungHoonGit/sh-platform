import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import PlatformLayout from "./layouts/PlatformLayout";
import Dashboard from "./pages/Dashboard";

const queryClient = new QueryClient();

function App() {
  const token = localStorage.getItem("accessToken");

  if (!token) {
    return <Navigate to="/" replace />;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<PlatformLayout />}>
            <Route path="/platform" element={<Dashboard />} />
            <Route path="/platform/scraper/*" element={<div>스크래퍼 모듈 (임베딩 예정)</div>} />
            <Route path="/platform/resume/*" element={<div>이력서 모듈 (추후)</div>} />
            <Route path="/platform/portfolio/*" element={<div>포트폴리오 모듈 (추후)</div>} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
