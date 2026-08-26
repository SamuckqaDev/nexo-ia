export type ImageGenerationStatus = "QUEUED" | "GENERATING" | "COMPLETED" | "FAILED" | "CANCELLED";

export type ImageGenerationJob = {
  id: string;
  conversationId: string;
  prompt: string;
  status: ImageGenerationStatus;
  progress: number | null;
  etaSeconds: number | null;
  provider: string;
  model: string | null;
  contentUrl: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string;
  errorMessage: string | null;
};

export type ImageRuntime = {
  provider: string;
  configured: boolean;
  available: boolean;
  model: string | null;
  models: string[];
  message: string;
};

export type CreateImageGenerationInput = {
  prompt: string;
  model: string;
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
