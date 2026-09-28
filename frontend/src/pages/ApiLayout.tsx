import { NavLink, Outlet, useParams } from "react-router-dom";
import { useLatestContract } from "../hooks/useLatestContract";
import type { Api } from "../types/api";
import type { ContractDetail } from "../types/contract";

export interface ApiLayoutContext {
  api: Api;
  contract: ContractDetail | null;
  refresh: () => void;
}

/**
 * Shared shell for everything scoped to one API: header (name, base URL),
 * tab navigation (Overview / Endpoints / Schemas), and a single fetch of
 * the API plus its latest contract, shared with child routes via
 * react-router's outlet context rather than each page re-fetching.
 */
export default function ApiLayout() {
  const { apiId } = useParams<{ apiId: string }>();
  const { state, api, contract, error, refresh } = useLatestContract(apiId);

  if (state === "loading") {
    return <p className="muted">Loading API…</p>;
  }
  if (state === "error" || !api) {
    return <p className="error">{error ?? "Could not load this API."}</p>;
  }

  return (
    <section>
      <div className="api-header">
        <div>
          <h1>{api.name}</h1>
          <p className="api-card-url">{api.baseUrl}</p>
        </div>
      </div>

      <nav className="tab-nav">
        <NavLink to="" end className={({ isActive }) => (isActive ? "tab active" : "tab")}>
          Overview
        </NavLink>
        <NavLink to="endpoints" className={({ isActive }) => (isActive ? "tab active" : "tab")}>
          Endpoints{contract ? ` (${contract.endpoints.length})` : ""}
        </NavLink>
        <NavLink to="schemas" className={({ isActive }) => (isActive ? "tab active" : "tab")}>
          Schemas{contract ? ` (${contract.schemas.length})` : ""}
        </NavLink>
      </nav>

      <div className="tab-content">
        <Outlet context={{ api, contract, refresh } satisfies ApiLayoutContext} />
      </div>
    </section>
  );
}
