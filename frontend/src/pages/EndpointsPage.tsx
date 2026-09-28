import { useMemo, useState } from "react";
import { Link, useOutletContext } from "react-router-dom";
import type { ApiLayoutContext } from "./ApiLayout";
import type { EndpointView } from "../types/contract";
import MethodBadge from "../components/MethodBadge";

const ALL = "ALL";

function matchesSearch(endpoint: EndpointView, query: string): boolean {
  if (!query) return true;
  const haystack = [
    endpoint.path,
    endpoint.operationId ?? "",
    endpoint.summary ?? "",
    endpoint.description ?? "",
    ...endpoint.tags,
  ]
    .join(" ")
    .toLowerCase();
  return haystack.includes(query.toLowerCase());
}

/**
 * Searchable/filterable endpoint list (product spec sections 27 and 29).
 * Filtering happens client-side against the already-loaded ContractDetail
 * -- there's no per-endpoint backend query yet, and with a contract's
 * endpoint count in the tens to low hundreds this is simpler and just as
 * fast as a server round trip.
 */
export default function EndpointsPage() {
  const { contract } = useOutletContext<ApiLayoutContext>();

  const [search, setSearch] = useState("");
  const [method, setMethod] = useState(ALL);
  const [tag, setTag] = useState(ALL);
  const [hideDeprecated, setHideDeprecated] = useState(false);

  const endpoints = contract?.endpoints ?? [];

  const methods = useMemo(
    () => Array.from(new Set(endpoints.map((e) => e.method.toUpperCase()))).sort(),
    [endpoints]
  );
  const tags = useMemo(
    () => Array.from(new Set(endpoints.flatMap((e) => e.tags))).sort(),
    [endpoints]
  );

  const filtered = endpoints.filter((e) => {
    if (method !== ALL && e.method.toUpperCase() !== method) return false;
    if (tag !== ALL && !e.tags.includes(tag)) return false;
    if (hideDeprecated && e.deprecated) return false;
    return matchesSearch(e, search);
  });

  if (!contract) {
    return <p className="muted">No contract yet — run discovery from the Overview tab first.</p>;
  }

  return (
    <div>
      <div className="filter-bar">
        <input
          className="search-input"
          placeholder="Search path, operation, summary, tag…"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value={ALL}>All methods</option>
          {methods.map((m) => (
            <option key={m} value={m}>
              {m}
            </option>
          ))}
        </select>
        {tags.length > 0 && (
          <select value={tag} onChange={(e) => setTag(e.target.value)}>
            <option value={ALL}>All tags</option>
            {tags.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
        )}
        <label className="checkbox-label">
          <input type="checkbox" checked={hideDeprecated} onChange={(e) => setHideDeprecated(e.target.checked)} />
          Hide deprecated
        </label>
      </div>

      {filtered.length === 0 ? (
        <div className="empty-state">
          <p>No endpoints match these filters.</p>
        </div>
      ) : (
        <div className="endpoint-list">
          {filtered.map((endpoint) => (
            <Link key={endpoint.id} to={endpoint.id} className="endpoint-row">
              <MethodBadge method={endpoint.method} />
              <span className="endpoint-path mono">{endpoint.path}</span>
              <span className="endpoint-summary muted">{endpoint.summary}</span>
              {endpoint.deprecated && <span className="pill pill-muted">deprecated</span>}
              {endpoint.tags.map((t) => (
                <span key={t} className="tag-chip">
                  {t}
                </span>
              ))}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
