import { useQueries, type UseQueryResult } from "@tanstack/react-query";
import { getProviderModelCatalog } from "../api/providerRegistryApi";
import type {
  ProviderConfiguration,
  ProviderModelCatalog,
  ProviderModelCatalogView
} from "../types/providerConfigurationTypes";

const pendingCatalog = (
  provider: ProviderConfiguration,
  status: ProviderModelCatalogView["status"],
  message: string
): ProviderModelCatalogView => ({
  providerConfigurationId: provider.id,
  providerType: provider.providerType,
  displayName: provider.displayName,
  selectedModel: provider.selectedModel,
  status,
  models: [],
  message
});

export function useProviderModelCatalogs(providers: ProviderConfiguration[]): ProviderModelCatalogView[] {
  const queries: UseQueryResult<ProviderModelCatalog, Error>[] = useQueries({
    queries: providers.map((provider: ProviderConfiguration) => ({
      queryKey: ["providers", "model-catalog", provider.id],
      queryFn: (): Promise<ProviderModelCatalog> => getProviderModelCatalog(provider.id),
      enabled: provider.enabled,
      staleTime: 30_000,
      retry: false
    }))
  });

  return providers.map((provider: ProviderConfiguration, index: number): ProviderModelCatalogView => {
    if (!provider.enabled) {
      return pendingCatalog(provider, "UNAVAILABLE", "This provider configuration is disabled");
    }

    const query: UseQueryResult<ProviderModelCatalog, Error> = queries[index];
    if (query.data) return query.data;
    if (query.isError) return pendingCatalog(provider, "UNAVAILABLE", query.error.message);
    return pendingCatalog(provider, "LOADING", "Loading available models…");
  });
}
