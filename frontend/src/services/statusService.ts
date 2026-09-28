import type { StatusResponse } from "../types/status";

/**
 * Calls the backend's /api/status endpoint.
 *
 * This is the first (and, in Phase 1, only) service module. Later phases
 * add apiService, contractService, etc. alongside it under src/services,
 * each scoped to one backend resource.
 */
export async function fetchStatus(): Promise<StatusResponse> {
  const response = await fetch("/api/status");
  if (!response.ok) {
    throw new Error(`Status request failed: ${response.status}`);
  }
  return response.json();
}
