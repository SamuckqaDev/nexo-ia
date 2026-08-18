import { z } from "zod";
import { apiClient } from "../../../shared/api/client";
import type { BaseResponse } from "../../../shared/types/apiTypes";
import type { ProviderStatus } from "../types/providerTypes";

const providerModelSchema = z.object({
  name: z.string(),
  modifiedAt: z.iso.datetime().nullable(),
  size: z.number().nullable()
});

const providerStatusSchema = z.object({
  id: z.string(),
  name: z.string(),
  kind: z.enum(["LOCAL", "REMOTE"]),
  endpoint: z.string(),
  connected: z.boolean(),
  models: z.array(providerModelSchema)
});

export function getOllamaStatus(): Promise<ProviderStatus> {
  return apiClient.get<BaseResponse<unknown>>("/providers/ollama")
    .then(({ data }) => providerStatusSchema.parse(data.data?.[0]));
}
