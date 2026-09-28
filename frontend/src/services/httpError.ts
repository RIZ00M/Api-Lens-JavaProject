import type { ApiErrorPayload } from "../types/api";

/** Shared across service modules: turns a non-ok Response into a readable Error message,
 *  using the backend's {timestamp, status, error, message, path} error shape when present. */
export async function parseErrorMessage(response: Response): Promise<string> {
  try {
    const body: ApiErrorPayload = await response.json();
    return body.message || `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}
