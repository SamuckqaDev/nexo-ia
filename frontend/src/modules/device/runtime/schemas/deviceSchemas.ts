import { z } from "zod";

export const deviceStatusSchema = z.enum(["OFFLINE", "ONLINE", "REVOKED"]);

export const deviceSchema = z.object({
  id: z.uuid(),
  displayName: z.string(),
  platform: z.string(),
  architecture: z.string(),
  appVersion: z.string(),
  status: deviceStatusSchema,
  capabilities: z.array(z.string()),
  lastSeenAt: z.iso.datetime().nullable(),
  createdAt: z.iso.datetime()
});

export const devicePairingSchema = z.object({
  pairingCode: z.string(),
  expiresAt: z.iso.datetime()
});
