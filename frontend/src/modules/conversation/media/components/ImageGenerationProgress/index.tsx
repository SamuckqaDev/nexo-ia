import { useEffect, useState, type ReactElement } from "react";
import type { ImageGenerationProgressProps } from "../../types/imageGenerationTypes";
import { ProgressCard, ProgressHeader, ProgressMeta, ProgressTrack, Prompt } from "./styles";

const elapsedSeconds = (startedAt: string): number =>
  Math.max(0, Math.floor((Date.now() - new Date(startedAt).getTime()) / 1000));

export function ImageGenerationProgress({ job }: ImageGenerationProgressProps): ReactElement {
  const [elapsed, setElapsed] = useState<number>(() => elapsedSeconds(job.startedAt));
  const running: boolean = job.status === "QUEUED" || job.status === "GENERATING";

  useEffect((): (() => void) | void => {
    setElapsed(elapsedSeconds(job.startedAt));
    if (!running) return;
    const timer = window.setInterval((): void => setElapsed(elapsedSeconds(job.startedAt)), 1_000);
    return (): void => window.clearInterval(timer);
  }, [job.startedAt, running]);

  const status: string = job.status === "QUEUED" ? "Waiting for image runtime"
    : job.status === "GENERATING" ? "Generating image"
      : job.status.toLowerCase();

  return (
    <ProgressCard aria-label={`Image generation ${job.status.toLowerCase()}`}>
      <ProgressHeader>
        <strong>{status}</strong>
        <span>{job.progress === null ? "Working…" : `${Math.round(job.progress)}%`}</span>
      </ProgressHeader>
      <ProgressTrack
        aria-label="Image generation progress"
        max={100}
        {...(job.progress === null ? {} : { value: job.progress })}
      />
      <ProgressMeta>
        <span>{elapsed}s elapsed</span>
        <span>{job.etaSeconds === null ? "Estimating remaining time…" : `About ${job.etaSeconds}s remaining`}</span>
      </ProgressMeta>
      <Prompt>{job.prompt}</Prompt>
      {job.errorMessage && <Prompt role="alert">{job.errorMessage}</Prompt>}
    </ProgressCard>
  );
}
