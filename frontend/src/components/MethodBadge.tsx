interface MethodBadgeProps {
  method: string;
}

const METHOD_CLASS: Record<string, string> = {
  GET: "method-get",
  POST: "method-post",
  PUT: "method-put",
  PATCH: "method-patch",
  DELETE: "method-delete",
};

/** Small colored badge for an HTTP method, used in endpoint lists and detail headers. */
export default function MethodBadge({ method }: MethodBadgeProps) {
  const upper = method.toUpperCase();
  const className = METHOD_CLASS[upper] ?? "method-other";
  return <span className={`method-badge ${className}`}>{upper}</span>;
}
