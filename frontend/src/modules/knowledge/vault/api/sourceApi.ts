import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { backendSourceSchema, sourceIngestionStatusSchema } from "../schemas/sourceSchemas";
import type { BackendSource, SourceIngestionStatus } from "../types/backendVaultTypes";

const first = <T>(response: BaseResponse<unknown>, parse: (value: unknown) => T): T => {
  const value: unknown = response.data?.[0];
  if (value === undefined) throw new Error("Nexo IA returned an empty response");
  return parse(value);
};

export const listBackendSources = (vaultId: string): Promise<BackendSource[]> =>
  apiClient.get<BaseResponse<unknown>>(`/knowledge/vaults/${vaultId}/sources`)
    .then(({ data }) => (data.data ?? []).map((item: unknown) => backendSourceSchema.parse(item)));

/**
 * Uploads bytes and a safe display name only — the browser reads the {@link File} locally, so no
 * absolute path is ever sent. See D-026.
 */
export const registerBackendSource = (vaultId: string, file: File): Promise<BackendSource> => {
  const body = new FormData();
  body.append("file", file);
  body.append("displayName", file.name);

  return apiClient.post<BaseResponse<unknown>>(`/knowledge/vaults/${vaultId}/sources`, body, {
    headers: { "Content-Type": "multipart/form-data" }
  }).then(({ data }) => first(data, (value) => backendSourceSchema.parse(value)));
};

export const getSourceIngestionStatus = (sourceId: string): Promise<SourceIngestionStatus> =>
  apiClient.get<BaseResponse<unknown>>(`/knowledge/sources/${sourceId}/ingestion`)
    .then(({ data }) => first(data, (value) => sourceIngestionStatusSchema.parse(value)));

export const archiveBackendSource = (sourceId: string): Promise<void> =>
  apiClient.delete<BaseResponse<unknown>>(`/knowledge/sources/${sourceId}`).then(() => undefined);
