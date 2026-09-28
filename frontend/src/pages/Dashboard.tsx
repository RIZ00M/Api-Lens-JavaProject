import { useCallback, useEffect, useState } from "react";
import type { Api, NewApiInput } from "../types/api";
import { createApi, deleteApi, listApis } from "../services/apiService";
import ApiCard from "../components/ApiCard";
import AddApiForm from "../components/AddApiForm";

type LoadState = "loading" | "ready" | "error";

/**
 * "My APIs" dashboard (product spec section 19). Phase 2 scope: list,
 * add, and remove APIs. Endpoint/schema counts, quality %, and change
 * badges are added once discovery/analysis (Phases 3-10) exist to
 * populate them.
 */
export default function Dashboard() {
  const [apis, setApis] = useState<Api[]>([]);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const refresh = useCallback(() => {
    setLoadState("loading");
    listApis()
      .then((page) => {
        setApis(page.content);
        setLoadState("ready");
      })
      .catch((err) => {
        setLoadError(err instanceof Error ? err.message : "Failed to load APIs");
        setLoadState("error");
      });
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function handleAdd(input: NewApiInput) {
    await createApi(input);
    setShowAddForm(false);
    refresh();
  }

  async function handleDelete(id: string) {
    setDeletingId(id);
    try {
      await deleteApi(id);
      setApis((current) => current.filter((api) => api.id !== id));
    } finally {
      setDeletingId(null);
    }
  }

  return (
    <section>
      <div className="dashboard-header">
        <h1>My APIs</h1>
        {!showAddForm && (
          <button className="btn-primary" onClick={() => setShowAddForm(true)}>
            + Add API
          </button>
        )}
      </div>

      {showAddForm && (
        <div className="panel">
          <AddApiForm onSubmit={handleAdd} onCancel={() => setShowAddForm(false)} />
        </div>
      )}

      {loadState === "loading" && <p className="muted">Loading APIs…</p>}
      {loadState === "error" && <p className="error">{loadError}</p>}

      {loadState === "ready" && apis.length === 0 && (
        <div className="empty-state">
          <p>No APIs yet.</p>
          <p className="muted small">Add one to start building its contract catalogue.</p>
        </div>
      )}

      {loadState === "ready" && apis.length > 0 && (
        <div className="api-list">
          {apis.map((api) => (
            <ApiCard key={api.id} api={api} onDelete={handleDelete} deleting={deletingId === api.id} />
          ))}
        </div>
      )}
    </section>
  );
}
