import { apiClient } from "../../../../shared/api/client";
import type { BaseResponse } from "../../../../shared/types/apiTypes";
import { imageGenerationJobSchema, imageRuntimeSchema } from "../schemas/imageGenerationSchemas";
import type {
  CreateImageGenerationInput,
  ImageGenerationJob,
  ImageRuntime
} from "../types/imageGenerationTypes";

const errorMessages: Record<string, string> = {
  COMFYUI_GENERATION_FAILED: "ComfyUI could not finish this image. Check the runtime and try again.",
  IMAGE_ARTIFACT_PERSISTENCE_FAILED: "The image was generated, but Nexo could not save the file.",
  IMAGE_MODEL_UNAVAILABLE: "The selected image model is no longer installed in ComfyUI."
};

const first = <T>(response: BaseResponse<T>): T => {
  const value: T | undefined = response.data?.[0];
  if (!value) throw new Error("Nexo returned an empty image response");
  return value;
};

const parseJob = (value: unknown): ImageGenerationJob => {
  const parsed = imageGenerationJobSchema.parse(value);
  return {
    ...parsed,
    errorMessage: parsed.errorCode ? errorMessages[parsed.errorCode] ?? parsed.errorCode : null
  };
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
  input: CreateImageGenerationInput
): Promise<ImageGenerationJob> {
  return apiClient.post<BaseResponse<unknown>>(
    `/media/images/conversations/${conversationId}`,
    input
  ).then(({ data }) => parseJob(first(data)));
}
