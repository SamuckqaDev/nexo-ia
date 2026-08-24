import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { personalMemorySchema } from "../schemas/personalMemorySchemas";
import type { PersonalMemory } from "../types/personalMemoryTypes";

export function listPersonalMemories(): Promise<PersonalMemory[]> {
  return apiClient.get<BaseResponse<unknown>>("/memories")
    .then(({ data }) => (data.data ?? []).map((value: unknown) => personalMemorySchema.parse(value)));
}

export function removePersonalMemory(memoryId: string): Promise<void> {
  return apiClient.delete(`/memories/${memoryId}`).then(() => undefined);
}
