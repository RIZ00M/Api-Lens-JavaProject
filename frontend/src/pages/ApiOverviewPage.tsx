import { useState } from "react";
import { useOutletContext } from "react-router-dom";
import type { ApiLayoutContext } from "./ApiLayout";
import { discoverApi, getDiscoveryAttempts } from "../services/apiService";
import type { DiscoveryAttempt, DiscoveryResultResponse } from "../types/discovery";

/**
 * API overview (product spec section 19/27): API metadata, a manual
 * "Run Discovery" action, the result of the most recent discovery run,
 * and the full attempt history for provenance (section 14).
 */
export default function ApiOverviewPage() {
  const { api, contract, refresh } = useOutletContext<ApiLayoutContext>();

  const [discovering, setDiscovering] = useState(false);
  const [discoveryResult, setDiscoveryResult] = useState<DiscoveryResultResponse | null>(null);
  const [discoveryError, setDiscoveryError] = useState<string | null>(null);

  const [attempts, setAttempts] = useState<DiscoveryAttempt[] | null>(null);
  const [attemptsOpen, setAttemptsOpen] = useState(false);
  const [attemptsLoading, setAttemptsLoading] = useState(false);

  async function runDiscovery() {
    setDiscovering(true);
    setDiscoveryError(null);
    try {
      const result = await discoverApi(api.id);
      setDiscoveryResult(result);
      refresh(); // reload the layout's contract if a new one was parsed
      if (attemptsOpen) {
        loadAttempts();
      }
    } catch (err) {
      setDiscoveryError(err instanceof Error ? err.message : "Discovery failed");
    } finally {
      setDiscovering(false);
    }
  }

  async function loadAttempts() {
    setAttemptsLoading(true);
    try {
      setAttempts(await getDiscoveryAttempts(api.id));
    } finally {
      setAttemptsLoading(false);
    }
  }

  function toggleAttempts() {
    const next = !attemptsOpen;
    setAttemptsOpen(next);
    if (next && attempts === null) {
      loadAttempts();
    }
  }

  return (
    <div className="overview-grid">
      <div className="panel">
        <div className="panel-header-row">
          <h2>Contract</h2>
          <button className="btn-primary" onClick={runDiscovery} disabled={discovering}>
            {discovering ? "Running discovery…" : "Run Discovery"}
          </button>
        </div>

        {discoveryError && <p className="error">{discoveryError}</p>}

        {discoveryResult && (
          <div className={`discovery-banner ${discoveryResult.specificationFound ? "found" : "not-found"}`}>
            {discoveryResult.specificationFound ? (
              <>
                <p>
                  Found <strong>{discoveryResult.specificationFormat}</strong> at{" "}
                  <code>{discoveryResult.sourceUrl}</code>
                  {discoveryResult.confidence && ` — confidence ${discoveryResult.confidence}`}.
                </p>
                {discoveryResult.contractParsed ? (
                  <p className="muted small">Contract parsed and saved.</p>
                ) : (
                  <p className="error small">
                    Found but could not be parsed yet
                    {discoveryResult.parseMessages.length > 0 && `: ${discoveryResult.parseMessages.join("; ")}`}
                  </p>
                )}
              </>
            ) : (
              <p>No specification found at any standard location.</p>
            )}
          </div>
        )}

        {contract ? (
          <dl className="status-grid">
            <dt>Specification</dt>
            <dd>{contract.specificationVersion ?? "—"}</dd>
            <dt>Title</dt>
            <dd>{contract.title ?? "—"}</dd>
            <dt>Endpoints</dt>
            <dd>{contract.endpoints.length}</dd>
            <dt>Schemas</dt>
            <dd>{contract.schemas.length}</dd>
            <dt>Servers</dt>
            <dd>{contract.servers.map((s) => s.url).join(", ") || "—"}</dd>
            <dt>Security schemes</dt>
            <dd>{contract.securitySchemes.map((s) => s.name).join(", ") || "—"}</dd>
            <dt>Discovered</dt>
            <dd>{new Date(contract.discoveredAt).toLocaleString()}</dd>
          </dl>
        ) : (
          <p className="muted">No contract yet — run discovery to fetch and parse this API's specification.</p>
        )}
      </div>

      <div className="panel">
        <button className="link-button" onClick={toggleAttempts}>
          {attemptsOpen ? "Hide" : "Show"} discovery attempt history
        </button>
        {attemptsOpen && (
          <>
            {attemptsLoading && <p className="muted small">Loading…</p>}
            {attempts && attempts.length === 0 && <p className="muted small">No discovery attempts yet.</p>}
            {attempts && attempts.length > 0 && (
              <table className="data-table">
                <thead>
                  <tr>
                    <th>URL</th>
                    <th>Method</th>
                    <th>Status</th>
                    <th>Result</th>
                    <th>When</th>
                  </tr>
                </thead>
                <tbody>
                  {attempts.map((a) => (
                    <tr key={a.id}>
                      <td className="mono">{a.url}</td>
                      <td>{a.discoveryMethod}</td>
                      <td>{a.httpStatus ?? "—"}</td>
                      <td>
                        {a.success ? (
                          <span className="pill pill-up">found</span>
                        ) : (
                          <span className="pill pill-muted" title={a.errorMessage ?? ""}>
                            not found
                          </span>
                        )}
                      </td>
                      <td className="muted small">{new Date(a.attemptedAt).toLocaleString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </>
        )}
      </div>
    </div>
  );
}
