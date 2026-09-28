import type { ContractDetail, ContractSummary } from "../types/contract";
import { parseErrorMessage } from "./httpError";

/** Client for the /api/apis/{id}/contracts and /api/contracts/{id} resources. */
export async function listContractsForApi(apiId: string): Promise<ContractSummary[]> {
  const response = await fetch(`/api/apis/${apiId}/contracts`);
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}

export async function getContract(contractId: string): Promise<ContractDetail> {
  const response = await fetch(`/api/contracts/${contractId}`);
  if (!response.ok) {
    throw new Error(await parseErrorMessage(response));
  }
  return response.json();
}
