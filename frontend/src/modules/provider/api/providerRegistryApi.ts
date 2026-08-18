import { z } from "zod";
import { apiClient } from "../../../shared/api/client";
import type { BaseResponse } from "../../../shared/types/apiTypes";
import type { ProviderConfiguration, ProviderConfigurationInput } from "../types/providerConfigurationTypes";
import { providerTypeSchema } from "../types/providerConfigurationTypes";

const responseSchema = z.object({ id: z.uuid(), providerType: providerTypeSchema, displayName: z.string(), endpoint: z.string(), selectedModel: z.string().nullable(), enabled: z.boolean(), lastConnectedAt: z.iso.datetime().nullable() });
const first = <T>(data: BaseResponse<T>): T => { const value = data.data?.[0]; if (!value) throw new Error("Nexo returned an empty provider response"); return value; };
export function listProviderConfigurations(): Promise<ProviderConfiguration[]> { return apiClient.get<BaseResponse<unknown>>("/providers/configurations").then(({ data }) => (data.data ?? []).map((item: unknown) => responseSchema.parse(item))); }
export function createProviderConfiguration(input: ProviderConfigurationInput): Promise<ProviderConfiguration> { return apiClient.post<BaseResponse<unknown>>("/providers/configurations", input).then(({ data }) => responseSchema.parse(first(data))); }
export function updateProviderConfiguration(id: string, input: ProviderConfigurationInput): Promise<ProviderConfiguration> { return apiClient.put<BaseResponse<unknown>>(`/providers/configurations/${id}`, input).then(({ data }) => responseSchema.parse(first(data))); }
export function deleteProviderConfiguration(id: string): Promise<void> { return apiClient.delete(`/providers/configurations/${id}`).then(() => undefined); }
