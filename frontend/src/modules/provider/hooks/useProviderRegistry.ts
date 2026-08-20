import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../shared/feedback/types/snackbarTypes";
import { createProviderConfiguration, deleteProviderConfiguration, listProviderConfigurations, updateProviderConfiguration } from "../api/providerRegistryApi";
import type { ProviderConfiguration, ProviderConfigurationInput, ProviderMutationResult, ProviderRegistryResult, ProviderUpdateInput, ProviderUpdateMutationResult } from "../types/providerConfigurationTypes";

export function useProviderRegistry(): { registry: ProviderRegistryResult; create: ProviderMutationResult; update: ProviderUpdateMutationResult; remove: (id: string) => void } {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const registry: ProviderRegistryResult = useQuery({ queryKey: ["providers", "configurations"], queryFn: listProviderConfigurations, retry: false });
  const create: ProviderMutationResult = useMutation({ mutationFn: createProviderConfiguration, onSuccess: (): void => { queryClient.invalidateQueries({ queryKey: ["providers"] }); show("Provider saved.", { variant: "success" }); }, onError: (error: Error): void => show(error.message, { variant: "error" }) });
  const update: ProviderUpdateMutationResult = useMutation({
    mutationFn: ({ id, input }: ProviderUpdateInput): Promise<ProviderConfiguration> => updateProviderConfiguration(id, input),
    onSuccess: (): void => { queryClient.invalidateQueries({ queryKey: ["providers"] }); show("Provider updated.", { variant: "success" }); },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });
  const remove = (id: string): void => { deleteProviderConfiguration(id).then(() => { queryClient.invalidateQueries({ queryKey: ["providers"] }); show("Provider removed.", { variant: "success" }); }).catch((error: Error) => show(error.message, { variant: "error" })); };
  return { registry, create, update, remove };
}
