import { useState } from "react";
import { useOutletContext } from "react-router-dom";
import type { ApiLayoutContext } from "./ApiLayout";
import JsonViewer from "../components/JsonViewer";

/** Lists reusable component schemas and lets the user inspect one at a time (product spec section 27). */
export default function SchemasPage() {
  const { contract } = useOutletContext<ApiLayoutContext>();
  const [expanded, setExpanded] = useState<string | null>(null);

  if (!contract) {
    return <p className="muted">No contract yet — run discovery from the Overview tab first.</p>;
  }

  if (contract.schemas.length === 0) {
    return (
      <div className="empty-state">
        <p>No reusable schemas were declared in this specification.</p>
      </div>
    );
  }

  return (
    <div className="schema-list">
      {contract.schemas.map((schema) => {
        const isOpen = expanded === schema.name;
        return (
          <div key={schema.name} className="panel schema-panel">
            <button className="link-button schema-toggle" onClick={() => setExpanded(isOpen ? null : schema.name)}>
              {isOpen ? "▾" : "▸"} <span className="mono">{schema.name}</span>
            </button>
            {isOpen && <JsonViewer value={schema.definition} />}
          </div>
        );
      })}
    </div>
  );
}
