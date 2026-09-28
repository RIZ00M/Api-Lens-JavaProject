import { Link, useOutletContext, useParams } from "react-router-dom";
import type { ApiLayoutContext } from "./ApiLayout";
import MethodBadge from "../components/MethodBadge";
import JsonViewer from "../components/JsonViewer";

/**
 * Full detail for one endpoint (product spec section 10). There's no
 * per-endpoint backend fetch yet (see ApiLayout's doc comment) -- the
 * endpoint is found by id within the already-loaded ContractDetail.
 */
export default function EndpointDetailPage() {
  const { contract } = useOutletContext<ApiLayoutContext>();
  const { endpointId } = useParams<{ endpointId: string }>();

  const endpoint = contract?.endpoints.find((e) => e.id === endpointId);

  if (!contract || !endpoint) {
    return (
      <div>
        <p className="error">Endpoint not found.</p>
        <Link to="..">Back to endpoints</Link>
      </div>
    );
  }

  return (
    <div>
      <Link to=".." className="back-link">
        ← Back to endpoints
      </Link>

      <div className="endpoint-detail-header">
        <MethodBadge method={endpoint.method} />
        <h2 className="mono">{endpoint.path}</h2>
        {endpoint.deprecated && <span className="pill pill-muted">deprecated</span>}
      </div>

      {endpoint.summary && <p className="endpoint-summary-text">{endpoint.summary}</p>}
      {endpoint.description && <p className="muted">{endpoint.description}</p>}

      <div className="detail-meta-row">
        {endpoint.operationId && (
          <span className="muted small">
            operationId: <code>{endpoint.operationId}</code>
          </span>
        )}
        {endpoint.tags.length > 0 && (
          <span className="muted small">tags: {endpoint.tags.join(", ")}</span>
        )}
      </div>

      <section className="detail-section">
        <h3>Authentication</h3>
        {endpoint.requiredSecuritySchemeNames.length === 0 ? (
          <p className="muted small">Authentication requirements are not documented for this operation.</p>
        ) : (
          <p>{endpoint.requiredSecuritySchemeNames.join(", ")}</p>
        )}
      </section>

      <section className="detail-section">
        <h3>Parameters</h3>
        {endpoint.parameters.length === 0 ? (
          <p className="muted small">No parameters.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>In</th>
                <th>Required</th>
                <th>Type</th>
                <th>Default</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              {endpoint.parameters.map((p) => (
                <tr key={`${p.location}:${p.name}`}>
                  <td className="mono">{p.name}</td>
                  <td>{p.location}</td>
                  <td>{p.required ? "yes" : "no"}</td>
                  <td>
                    {p.type ?? "—"}
                    {p.format ? ` (${p.format})` : ""}
                    {p.enumValues.length > 0 && (
                      <div className="muted small">enum: {p.enumValues.join(", ")}</div>
                    )}
                  </td>
                  <td>{p.defaultValue ?? "—"}</td>
                  <td className="muted small">{p.description ?? "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {endpoint.requestBody && (
        <section className="detail-section">
          <h3>Request Body {endpoint.requestBody.required && <span className="muted small">(required)</span>}</h3>
          <JsonViewer value={endpoint.requestBody.content} />
        </section>
      )}

      <section className="detail-section">
        <h3>Responses</h3>
        {endpoint.responses.length === 0 ? (
          <p className="muted small">No responses documented.</p>
        ) : (
          endpoint.responses.map((r) => (
            <div key={r.statusCode} className="response-block">
              <div className="response-header">
                <span className="status-code-badge">{r.statusCode}</span>
                <span className="muted">{r.description}</span>
              </div>
              <JsonViewer value={r.content} />
            </div>
          ))
        )}
      </section>
    </div>
  );
}
