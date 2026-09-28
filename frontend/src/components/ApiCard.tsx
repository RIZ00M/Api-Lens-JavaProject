import { Link } from "react-router-dom";
import type { Api } from "../types/api";

interface ApiCardProps {
  api: Api;
  onDelete: (id: string) => void;
  deleting: boolean;
}

/**
 * Card summarizing one registered API. Later phases add quality
 * percentage and change/breaking-change badges here (see product spec
 * section 19, "Dashboard") once analysis/diffing exist to populate them;
 * endpoint/schema counts now live on the API's own Overview page rather
 * than here, since fetching them for every card on the dashboard would
 * mean one extra request per API just to render the list.
 */
export default function ApiCard({ api, onDelete, deleting }: ApiCardProps) {
  return (
    <article className="api-card">
      <Link to={`/apis/${api.id}`} className="api-card-main">
        <h3>{api.name}</h3>
        <p className="api-card-url">{api.baseUrl}</p>
        {api.openApiUrl && <p className="api-card-hint">OpenAPI hint: {api.openApiUrl}</p>}
      </Link>
      <div className="api-card-meta">
        <span className="muted small">
          Added {new Date(api.createdAt).toLocaleDateString()}
        </span>
        <button
          className="btn-danger-ghost"
          onClick={() => onDelete(api.id)}
          disabled={deleting}
        >
          {deleting ? "Removing…" : "Remove"}
        </button>
      </div>
    </article>
  );
}
