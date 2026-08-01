import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useEffect } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import PlatformLayout from "./layouts/PlatformLayout";
import Dashboard from "./pages/Dashboard";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminUsers from "./pages/admin/AdminUsers";
import AdminTenants from "./pages/admin/AdminTenants";

const queryClient = new QueryClient();

function RedirectTo({ to }: { to: string }) {
  useEffect(() => {
    window.location.replace(to);
  }, [to]);
  return null;
}

function App() {
  const token = localStorage.getItem("accessToken");

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        {!token ? (
          <RedirectTo to="/" />
        ) : (
          <Routes>
            <Route element={<PlatformLayout />}>
              <Route path="/platform" element={<Dashboard />} />
              <Route path="/platform/scraper/*" element={<RedirectTo to="/scraper/" />} />
              <Route path="/platform/resume/*" element={<div>이력서 모듈 (추후)</div>} />
              <Route path="/platform/portfolio/*" element={<div>포트폴리오 모듈 (추후)</div>} />
              <Route path="/platform/admin" element={<AdminDashboard />} />
              <Route path="/platform/admin/users" element={<AdminUsers />} />
              <Route path="/platform/admin/tenants" element={<AdminTenants />} />
            </Route>
          </Routes>
        )}
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
