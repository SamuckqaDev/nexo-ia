import { create } from "zustand";
import type { ImageGenerationJob, ImageGenerationState } from "../types/imageGenerationTypes";

export const useImageGenerationStore = create<ImageGenerationState>((set) => ({
  jobs: {},
  upsertJob: (job: ImageGenerationJob): void => set((state: ImageGenerationState): Pick<ImageGenerationState, "jobs"> => ({
    jobs: { ...state.jobs, [job.id]: job }
  })),
  removeJob: (jobId: string): void => set((state: ImageGenerationState): Pick<ImageGenerationState, "jobs"> => ({
    jobs: Object.fromEntries(Object.entries(state.jobs).filter(([id]: [string, ImageGenerationJob]): boolean => id !== jobId))
  })),
  reset: (): void => set({ jobs: {} })
}));
