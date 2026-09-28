import { useEffect, useState } from "react";
import { BrowserRouter, Link, Route, Routes } from "react-router-dom";
import { fetchStatus } from "./services/statusService";
import Dashboard from "./pages/Dashboard";
import ApiLayout from "./pages/ApiLayout";
import ApiOverviewPage from "./pages/ApiOverviewPage";
import EndpointsPage from "./pages/EndpointsPage";
import EndpointDetailPage from "./pages/EndpointDetailPage";
import SchemasPage from "./pages/SchemasPage";

type BackendState = "checking" | "up" | "down";

/**
 * App shell: header (with a small backend-connectivity indicator) plus
 * routed pages. Dashboard lists APIs; each API gets its own Overview /
 * Endpoints / Schemas sub-pages under ApiLayout (product spec section 27).
 * A Contract Analysis/History/Diff tab is added to the same layout in
 * later phases (7-10) once there's data to show there.
 */
export default function App() {
  const [backendState, setBackendState] = useState<BackendState>("checking");

  useEffect(() => {
    fetchStatus()
      .then(() => setBackendState("up"))
      .catch(() => setBackendState("down"));
  }, []);

  return (
    <BrowserRouter>
      <div className="shell">
        <header className="shell-header">
          <div>
            <Link to="/" className="logo-mark">
              API Lens
            </Link>
            <span className="tagline">API discovery, contract inspection &amp; drift detection</span>
          </div>
          <span className={`pill ${backendState === "up" ? "pill-up" : backendState === "down" ? "pill-down" : "pill-muted"}`}>
            {backendState === "checking" && "Checking backend…"}
            {backendState === "up" && "Backend connected"}
            {backendState === "down" && "Backend unreachable"}
          </span>
        </header>

        <main>
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/apis/:apiId" element={<ApiLayout />}>
              <Route index element={<ApiOverviewPage />} />
              <Route path="endpoints" element={<EndpointsPage />} />
              <Route path="endpoints/:endpointId" element={<EndpointDetailPage />} />
              <Route path="schemas" element={<SchemasPage />} />
            </Route>
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
