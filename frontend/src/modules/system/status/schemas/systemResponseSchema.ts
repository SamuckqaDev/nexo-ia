import { z } from "zod";

export const systemResponseSchema = z.object({
  name: z.string(),
  version: z.string(),
  status: z.string(),
  timestamp: z.iso.datetime()
});

export const systemBaseResponseSchema = z.object({
  code: z.number().int(),
  message: z.string(),
  data: z.array(systemResponseSchema)
});
