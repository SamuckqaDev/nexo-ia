export type ImageGenerationStatus = "QUEUED" | "GENERATING" | "COMPLETED" | "FAILED" | "CANCELLED";

export type ImageGenerationJob = {
  id: string;
  conversationId: string;
  prompt: string;
  status: ImageGenerationStatus;
  progress: number | null;
  etaSeconds: number | null;
  startedAt: string;
  errorMessage: string | null;
};

export type ImageGenerationState = {
  jobs: Record<string, ImageGenerationJob>;
  upsertJob: (job: ImageGenerationJob) => void;
  removeJob: (jobId: string) => void;
  reset: () => void;
};

export type ImageGenerationProgressProps = {
  job: ImageGenerationJob;
};
