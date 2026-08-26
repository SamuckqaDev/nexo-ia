import { useEffect } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { QueryClient, UseMutationResult, UseQueryResult } from "@tanstack/react-query";
import { useSnackbarStore } from "../../../../shared/feedback/stores/useSnackbarStore";
import type { SnackbarState } from "../../../../shared/feedback/types/snackbarTypes";
import {
  createImageGeneration,
  getImageRuntime,
  listImageGenerations
} from "../api/imageGenerationApi";
import { useImageGenerationStore } from "../stores/useImageGenerationStore";
import type {
  CreateImageGenerationInput,
  ImageGenerationJob,
  ImageGenerationState,
  ImageRuntime
} from "../types/imageGenerationTypes";

type ImageGenerationResult = {
  runtime: UseQueryResult<ImageRuntime, Error>;
  jobs: UseQueryResult<ImageGenerationJob[], Error>;
  generate: UseMutationResult<ImageGenerationJob, Error, CreateImageGenerationInput>;
};

export function useImageGeneration(conversationId: string | null): ImageGenerationResult {
  const queryClient: QueryClient = useQueryClient();
  const upsertJob: ImageGenerationState["upsertJob"] = useImageGenerationStore(
    (state: ImageGenerationState) => state.upsertJob);
  const show: SnackbarState["show"] = useSnackbarStore((state: SnackbarState) => state.show);
  const runtime = useQuery({
    queryKey: ["media", "images", "runtime"],
    queryFn: getImageRuntime,
    retry: false,
    staleTime: 30_000
  });
  const jobs = useQuery({
    queryKey: ["media", "images", "conversation", conversationId],
    queryFn: (): Promise<ImageGenerationJob[]> => listImageGenerations(conversationId as string),
    enabled: Boolean(conversationId),
    retry: false,
    refetchInterval: (query): number | false => query.state.data?.some(
      (job: ImageGenerationJob): boolean => job.status === "QUEUED" || job.status === "GENERATING")
      ? 1_000
      : false
  });

  useEffect((): void => {
    jobs.data?.forEach(upsertJob);
  }, [jobs.data, upsertJob]);

  const generate = useMutation({
    mutationFn: (input: CreateImageGenerationInput): Promise<ImageGenerationJob> => {
      if (!conversationId) return Promise.reject(new Error("Select a conversation first"));
      return createImageGeneration(conversationId, input);
    },
    onSuccess: (job: ImageGenerationJob): void => {
      upsertJob(job);
      queryClient.invalidateQueries({
        queryKey: ["media", "images", "conversation", conversationId]
      });
      show("Image generation queued in ComfyUI.", { variant: "success" });
    },
    onError: (error: Error): void => show(error.message, { variant: "error" })
  });

  return { runtime, jobs, generate };
}
