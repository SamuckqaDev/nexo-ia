import { z } from "zod";

export const personalMemorySchema = z.object({
  id: z.uuid(),
  content: z.string(),
  sourceConversationId: z.uuid().nullable(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime()
});
