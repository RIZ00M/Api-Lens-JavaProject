interface JsonViewerProps {
  value: unknown;
}

/** Read-only, formatted JSON block -- used for schema definitions and request/response bodies. */
export default function JsonViewer({ value }: JsonViewerProps) {
  const hasContent = value !== null && value !== undefined && !(typeof value === "object" && Object.keys(value as object).length === 0);
  if (!hasContent) {
    return <p className="muted small">No schema documented.</p>;
  }
  return <pre className="json-viewer">{JSON.stringify(value, null, 2)}</pre>;
}
