import { ArrowSquareOut, ImageSquare } from "@phosphor-icons/react";
import type { ReactElement } from "react";
import type {
  GeneratedMediaGalleryProps,
  ImageGenerationJob
} from "../../types/imageGenerationTypes";
import {
  MediaCard,
  MediaCopy,
  MediaGrid,
  MediaPreview
} from "./styles";

const completedAt = (job: ImageGenerationJob): string => new Intl.DateTimeFormat(undefined, {
  dateStyle: "medium",
  timeStyle: "short"
}).format(new Date(job.completedAt ?? job.updatedAt));

export function GeneratedMediaGallery({ jobs }: GeneratedMediaGalleryProps): ReactElement {
  return (
    <MediaGrid aria-label="Generated media">
      {jobs.filter((job: ImageGenerationJob): boolean => Boolean(job.contentUrl)).map((job: ImageGenerationJob) => (
        <MediaCard key={job.id}>
          <MediaPreview
            href={job.contentUrl ?? ""}
            target="_blank"
            rel="noreferrer"
            aria-label={`Open generated image: ${job.prompt}`}
          >
            <img
              src={`${job.contentUrl ?? ""}?v=${encodeURIComponent(job.updatedAt)}`}
              alt={job.prompt}
              loading="lazy"
            />
            <span><ArrowSquareOut size={14} weight="bold" /> Open</span>
          </MediaPreview>
          <MediaCopy>
            <strong><ImageSquare size={14} weight="duotone" />{job.prompt}</strong>
            <span>{job.model ?? job.provider}</span>
            <time dateTime={job.completedAt ?? job.updatedAt}>{completedAt(job)}</time>
          </MediaCopy>
        </MediaCard>
      ))}
    </MediaGrid>
  );
}
