import { useEffect, useState, type ReactElement } from "react";
import type { ImageGenerationProgressProps } from "../../types/imageGenerationTypes";
import { ProgressCard, ProgressHeader, ProgressMeta, ProgressTrack, Prompt } from "./styles";

const elapsedSeconds = (startedAt: string): number =>
  Math.max(0, Math.floor((Date.now() - new Date(startedAt).getTime()) / 1000));

export function ImageGenerationProgress({ job }: ImageGenerationProgressProps): ReactElement {
  const timingStart: string = job.startedAt ?? job.createdAt;
  const [elapsed, setElapsed] = useState<number>(() => elapsedSeconds(timingStart));
  const running: boolean = job.status === "QUEUED" || job.status === "GENERATING";

  useEffect((): (() => void) | void => {
    setElapsed(elapsedSeconds(timingStart));
    if (!running) return;
    const timer = window.setInterval((): void => setElapsed(elapsedSeconds(timingStart)), 1_000);
    return (): void => window.clearInterval(timer);
  }, [running, timingStart]);

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
      {job.status === "COMPLETED" && job.contentUrl && (
        <img src={`${job.contentUrl}?v=${encodeURIComponent(job.updatedAt)}`} alt={job.prompt} />
      )}
      {job.errorMessage && <Prompt role="alert">{job.errorMessage}</Prompt>}
    </ProgressCard>
  );
}
