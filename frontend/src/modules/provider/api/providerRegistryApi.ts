import { z } from "zod";
import { apiClient } from "../../../shared/api/client";
import type { BaseResponse } from "../../../shared/types/apiTypes";
import type { ProviderConfiguration, ProviderConfigurationInput, ProviderConnectionTest, ProviderConnectionTestInput, ProviderModelCatalog } from "../types/providerConfigurationTypes";
import { providerTypeSchema } from "../types/providerConfigurationTypes";

const providerModelSchema = z.object({
  name: z.string(),
  modifiedAt: z.iso.datetime().nullable(),
  size: z.number().nullable(),
  toolCallingSupported: z.boolean().nullable()
});
const responseSchema = z.object({ id: z.uuid(), providerType: providerTypeSchema, displayName: z.string(), endpoint: z.string(), selectedModel: z.string().nullable(), enabled: z.boolean(), lastConnectedAt: z.iso.datetime().nullable() });
const modelCatalogSchema = z.object({
  providerConfigurationId: z.uuid(),
  providerType: providerTypeSchema,
  displayName: z.string(),
  selectedModel: z.string().nullable(),
  status: z.enum(["AVAILABLE", "EMPTY", "UNAVAILABLE", "UNSUPPORTED"]),
  models: z.array(providerModelSchema),
  message: z.string().nullable()
});
const connectionTestSchema = z.object({
  endpoint: z.string(),
  status: z.enum(["AVAILABLE", "EMPTY", "UNAVAILABLE", "UNSUPPORTED"]),
  processingLocation: z.enum(["LOCAL", "REMOTE"]).nullable(),
  models: z.array(providerModelSchema),
  message: z.string().nullable()
});
const first = <T>(data: BaseResponse<T>): T => { const value = data.data?.[0]; if (!value) throw new Error("Nexo returned an empty provider response"); return value; };
export function listProviderConfigurations(): Promise<ProviderConfiguration[]> { return apiClient.get<BaseResponse<unknown>>("/providers/configurations").then(({ data }) => (data.data ?? []).map((item: unknown) => responseSchema.parse(item))); }
export function createProviderConfiguration(input: ProviderConfigurationInput): Promise<ProviderConfiguration> { return apiClient.post<BaseResponse<unknown>>("/providers/configurations", input).then(({ data }) => responseSchema.parse(first(data))); }
export function updateProviderConfiguration(id: string, input: ProviderConfigurationInput): Promise<ProviderConfiguration> { return apiClient.put<BaseResponse<unknown>>(`/providers/configurations/${id}`, input).then(({ data }) => responseSchema.parse(first(data))); }
export function deleteProviderConfiguration(id: string): Promise<void> { return apiClient.delete(`/providers/configurations/${id}`).then(() => undefined); }
export function getProviderModelCatalog(id: string): Promise<ProviderModelCatalog> {
  return apiClient.get<BaseResponse<unknown>>(`/providers/configurations/${id}/models`)
    .then(({ data }) => modelCatalogSchema.parse(first(data)));
}
export function testProviderConnection(input: ProviderConnectionTestInput): Promise<ProviderConnectionTest> {
  return apiClient.post<BaseResponse<unknown>>("/providers/configurations/test", input)
    .then(({ data }) => connectionTestSchema.parse(first(data)));
}
