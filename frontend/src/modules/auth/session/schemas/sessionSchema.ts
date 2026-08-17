import { z } from "zod";

export const sessionSchema = z.object({
  id: z.uuid(),
  status: z.literal("ACTIVE"),
  initialIp: z.string(),
  lastIp: z.string(),
  userAgent: z.string(),
  createdAt: z.iso.datetime(),
  lastSeenAt: z.iso.datetime(),
  accessExpiresAt: z.iso.datetime(),
  refreshExpiresAt: z.iso.datetime(),
  current: z.boolean()
});

export const sessionListSchema = z.array(sessionSchema);
