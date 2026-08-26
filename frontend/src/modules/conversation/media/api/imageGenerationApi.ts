import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { imageGenerationJobSchema, imageRuntimeSchema } from "../schemas/imageGenerationSchemas";
import type { ImageGenerationJob, ImageRuntime } from "../types/imageGenerationTypes";

const first = <T>(response: BaseResponse<T>): T => {
  const value: T | undefined = response.data?.[0];
  if (!value) throw new Error("Nexo returned an empty image response");
  return value;
};

const parseJob = (value: unknown): ImageGenerationJob => {
  const parsed = imageGenerationJobSchema.parse(value);
  return { ...parsed, errorMessage: parsed.errorCode };
};

export function getImageRuntime(): Promise<ImageRuntime> {
  return apiClient.get<BaseResponse<unknown>>("/media/images/runtime")
    .then(({ data }) => imageRuntimeSchema.parse(first(data)));
}

export function listImageGenerations(conversationId: string): Promise<ImageGenerationJob[]> {
  return apiClient.get<BaseResponse<unknown>>(`/media/images/conversations/${conversationId}`)
    .then(({ data }) => (data.data ?? []).map(parseJob));
}

export function createImageGeneration(
  conversationId: string,
  prompt: string
): Promise<ImageGenerationJob> {
  return apiClient.post<BaseResponse<unknown>>(
    `/media/images/conversations/${conversationId}`,
    { prompt }
  ).then(({ data }) => parseJob(first(data)));
}
