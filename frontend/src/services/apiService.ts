import type { Api, NewApiInput, Page } from "../types/api";
import type { DiscoveryResultResponse, DiscoveryAttempt } from "../types/discovery";
import { parseErrorMessage } from "./httpError";

/** Client for the /api/apis resource (see backend ApiController). */
export async function listApis(): Promise<Page<Api>> {
  const response = await fetch("/api/apis");
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}

export async function getApi(id: string): Promise<Api> {
  const response = await fetch(`/api/apis/${id}`);
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}

export async function createApi(input: NewApiInput): Promise<Api> {
  const response = await fetch("/api/apis", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}

export async function deleteApi(id: string): Promise<void> {
  const response = await fetch(`/api/apis/${id}`, { method: "DELETE" });
  if (!response.ok && response.status !== 204) {
    throw new Error(await parseErrorMessage(response));
  }
}

export async function discoverApi(id: string): Promise<DiscoveryResultResponse> {
  const response = await fetch(`/api/apis/${id}/discover`, { method: "POST" });
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}

export async function getDiscoveryAttempts(id: string): Promise<DiscoveryAttempt[]> {
  const response = await fetch(`/api/apis/${id}/discovery-attempts`);
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}
