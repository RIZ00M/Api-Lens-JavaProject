import { useCallback, useEffect, useState } from "react";
import type { Api } from "../types/api";
import type { ContractDetail } from "../types/contract";
import { getApi } from "../services/apiService";
import { getContract, listContractsForApi } from "../services/contractService";

type LoadState = "loading" | "ready" | "no-contract" | "error";

interface Result {
  state: LoadState;
  api: Api | null;
  contract: ContractDetail | null;
  error: string | null;
  refresh: () => void;
}

/**
 * Loads an API plus its most recent parsed contract (if any). Shared by
 * the Overview/Endpoints/Schemas pages so each doesn't repeat the same
 * "list contracts, take the newest, fetch its detail" sequence. There is
 * currently no per-endpoint or per-schema fetch-by-id API (deferred from
 * Phase 5); pages that need one endpoint/schema filter the full
 * ContractDetail returned here client-side instead.
 */
export function useLatestContract(apiId: string | undefined): Result {
  const [state, setState] = useState<LoadState>("loading");
  const [api, setApi] = useState<Api | null>(null);
  const [contract, setContract] = useState<ContractDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);

  const refresh = useCallback(() => setRefreshToken((t) => t + 1), []);

  useEffect(() => {
    if (!apiId) {
      return;
    }
    let cancelled = false;
    setState("loading");

    (async () => {
      try {
        const [loadedApi, summaries] = await Promise.all([getApi(apiId), listContractsForApi(apiId)]);
        if (cancelled) return;
        setApi(loadedApi);

        if (summaries.length === 0) {
          setContract(null);
          setState("no-contract");
          return;
        }

        const latest = await getContract(summaries[0].id);
        if (cancelled) return;
        setContract(latest);
        setState("ready");
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Failed to load API");
        setState("error");
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [apiId, refreshToken]);

  return { state, api, contract, error, refresh };
}
