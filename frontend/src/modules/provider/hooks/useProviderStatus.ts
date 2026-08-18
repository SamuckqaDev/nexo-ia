import { useQuery } from "@tanstack/react-query";
import type { ProviderStatusResult } from "../types/providerTypes";
import { getOllamaStatus } from "../api/providerApi";

export function useProviderStatus(): ProviderStatusResult {
  return useQuery({
    queryKey: ["providers", "ollama"],
    queryFn: getOllamaStatus,
    staleTime: 30_000,
    retry: false
  });
}
