import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useEffect } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import PlatformLayout from "./layouts/PlatformLayout";
import Dashboard from "./pages/Dashboard";
import NotFound from "./pages/NotFound";
import AdminDashboard from "./pages/admin/AdminDashboard";
import AdminUsers from "./pages/admin/AdminUsers";
import AdminTenants from "./pages/admin/AdminTenants";
import AdminAudit from "./pages/admin/AdminAudit";
import AdminSessions from "./pages/admin/AdminSessions";
import AccountSettings from "./pages/AccountSettings";

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
              <Route path="/platform/resume/*" element={<RedirectTo to="/resume/" />} />
              <Route path="/platform/admin" element={<AdminDashboard />} />
              <Route path="/platform/admin/users" element={<AdminUsers />} />
              <Route path="/platform/admin/tenants" element={<AdminTenants />} />
              <Route path="/platform/admin/audit" element={<AdminAudit />} />
              <Route path="/platform/admin/sessions" element={<AdminSessions />} />
              <Route path="/platform/account" element={<AccountSettings />} />
            </Route>
            <Route path="*" element={<NotFound />} />
          </Routes>
        )}
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
