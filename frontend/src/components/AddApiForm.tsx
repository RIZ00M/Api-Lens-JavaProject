import { FormEvent, useState } from "react";
import type { NewApiInput } from "../types/api";

interface AddApiFormProps {
  onSubmit: (input: NewApiInput) => Promise<void>;
  onCancel: () => void;
}

/** Inline form for "Add API" (product spec section 4, "Step 1 — Create Project"). */
export default function AddApiForm({ onSubmit, onCancel }: AddApiFormProps) {
  const [name, setName] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [openApiUrl, setOpenApiUrl] = useState("");
  const [documentationUrl, setDocumentationUrl] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({
        name,
        baseUrl,
        openApiUrl: openApiUrl || undefined,
        documentationUrl: documentationUrl || undefined,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add API");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="add-api-form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="name">Name</label>
        <input id="name" value={name} onChange={(e) => setName(e.target.value)} required placeholder="GitHub API" />
      </div>

      <div className="field">
        <label htmlFor="baseUrl">Base URL</label>
        <input
          id="baseUrl"
          value={baseUrl}
          onChange={(e) => setBaseUrl(e.target.value)}
          required
          placeholder="https://api.github.com"
        />
      </div>

      <div className="field">
        <label htmlFor="openApiUrl">OpenAPI URL <span className="muted">(optional)</span></label>
        <input
          id="openApiUrl"
          value={openApiUrl}
          onChange={(e) => setOpenApiUrl(e.target.value)}
          placeholder="https://api.github.com/openapi.json"
        />
      </div>

      <div className="field">
        <label htmlFor="documentationUrl">Documentation URL <span className="muted">(optional)</span></label>
        <input
          id="documentationUrl"
          value={documentationUrl}
          onChange={(e) => setDocumentationUrl(e.target.value)}
          placeholder="https://docs.github.com"
        />
      </div>

      {error && <p className="error">{error}</p>}

      <div className="form-actions">
        <button type="button" className="btn-ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="submit" className="btn-primary" disabled={submitting}>
          {submitting ? "Adding…" : "Add API"}
        </button>
      </div>
    </form>
  );
}
