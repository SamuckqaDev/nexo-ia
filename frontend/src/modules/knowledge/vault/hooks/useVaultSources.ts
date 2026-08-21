import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import { archiveBackendSource, listBackendSources, registerBackendSource } from "../api/sourceApi";
import { backendVaultsKey } from "./useBackendVaultCatalog";
import type { BackendSource } from "../types/backendVaultTypes";

export const vaultSourcesKey = (vaultId: string): readonly unknown[] => ["knowledge", "vaults", vaultId, "sources"];

/**
 * Backend-backed sources for one Knowledge Vault. Uploading a file runs the real ingestion pipeline
 * — normalize, chunk, embed — on the server; the browser only sends the bytes and a safe display name.
 */
export function useVaultSources(vaultId: string | null): {
  sources: UseQueryResult<BackendSource[]>;
  upload: UseMutationResult<BackendSource, Error, File>;
  remove: UseMutationResult<void, Error, string>;
} {
  const queryClient: QueryClient = useQueryClient();
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);

  const sources = useQuery({
    queryKey: vaultSourcesKey(vaultId ?? ""),
    queryFn: (): Promise<BackendSource[]> => listBackendSources(vaultId ?? ""),
    enabled: vaultId !== null,
    retry: false
  });

  const invalidate = (): void => {
    if (vaultId) queryClient.invalidateQueries({ queryKey: vaultSourcesKey(vaultId) });
    queryClient.invalidateQueries({ queryKey: backendVaultsKey });
  };

  const upload = useMutation({
    mutationFn: (file: File): Promise<BackendSource> => registerBackendSource(vaultId ?? "", file),
    onSuccess: (source: BackendSource): void => {
      invalidate();
      show(source.status === "READY"
        ? `Added ${source.displayName}.`
        : `${source.displayName} was stored, but its type is not embedded yet.`,
        { variant: source.status === "READY" ? "success" : "info" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  const remove = useMutation({
    mutationFn: (sourceId: string): Promise<void> => archiveBackendSource(sourceId),
    onSuccess: (): void => { invalidate(); show("Source removed.", { variant: "success" }); },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  return { sources, upload, remove };
}
