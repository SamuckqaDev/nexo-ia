import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { backendVaultSchema } from "../schemas/vaultSchemas";
import type { BackendVault, CreateVaultInput } from "../types/backendVaultTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty response");
  return parse(value);
};

export const listBackendVaults = (): Promise<BackendVault[]> =>
  apiClient.get<BaseResponse<unknown>>("/knowledge/vaults")
    .then(({ data }) => (data.data ?? []).map((item: unknown) => backendVaultSchema.parse(item)));

export const createBackendVault = (input: CreateVaultInput): Promise<BackendVault> =>
  apiClient.post<BaseResponse<unknown>>("/knowledge/vaults", input)
    .then(({ data }) => first(data, (value) => backendVaultSchema.parse(value)));

export const updateBackendVault = (
  vaultId: string,
  input: { name: string; description?: string }
): Promise<BackendVault> =>
  apiClient.put<BaseResponse<unknown>>(`/knowledge/vaults/${vaultId}`, input)
    .then(({ data }) => first(data, (value) => backendVaultSchema.parse(value)));

export const archiveBackendVault = (vaultId: string): Promise<void> =>
  apiClient.delete<BaseResponse<unknown>>(`/knowledge/vaults/${vaultId}`).then(() => undefined);
