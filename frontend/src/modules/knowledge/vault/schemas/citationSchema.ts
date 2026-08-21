import { z } from "zod";

/**
 * A safe, bounded reference to a retrieved chunk — matches the backend's CitationResponse. Never a
 * raw path, secret, or the full source.
 */
export const knowledgeCitationSchema = z.object({
  vaultName: z.string(),
  sourceDisplayName: z.string(),
  chunkOrdinal: z.number().int(),
  excerpt: z.string(),
  score: z.number()
});
