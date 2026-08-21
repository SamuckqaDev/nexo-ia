import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { archiveBackendVault, createBackendVault, listBackendVaults, updateBackendVault } from "../api/vaultApi";
import type { BackendVault, CreateVaultInput } from "../types/backendVaultTypes";

export const backendVaultsKey = ["knowledge", "vaults"] as const;

/**
 * Backend-backed Knowledge Vault data, distinct from {@code useVaultCatalogStore} — that Zustand
 * store now owns only transient UI state (which source ids are attached to the current chat draft),
 * not vault/source data itself. See D-026.
 */
export function useBackendVaultCatalog(): {
  vaults: UseQueryResult<BackendVault[]>;
  create: UseMutationResult<BackendVault, Error, CreateVaultInput>;
  update: UseMutationResult<BackendVault, Error, { vaultId: string; name: string; description?: string }>;
  archive: UseMutationResult<void, Error, string>;
} {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);

  const vaults = useQuery({ queryKey: backendVaultsKey, queryFn: listBackendVaults, retry: false });

  const create = useMutation({
    mutationFn: createBackendVault,
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: backendVaultsKey });
      show("Vault created.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  const update = useMutation({
    mutationFn: ({ vaultId, name, description }: { vaultId: string; name: string; description?: string }) =>
      updateBackendVault(vaultId, { name, description }),
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: backendVaultsKey });
      show("Vault updated.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  const archive = useMutation({
    mutationFn: archiveBackendVault,
    onSuccess: (): void => {
      queryClient.invalidateQueries({ queryKey: backendVaultsKey });
      show("Vault archived.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  return { vaults, create, update, archive };
}
