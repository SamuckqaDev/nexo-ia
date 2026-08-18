import { z } from "zod";

export const userSchema = z.object({
  id: z.uuid(),
  username: z.string(),
  email: z.email(),
  name: z.string(),
  birthDate: z.iso.date().nullable(),
  createdAt: z.iso.datetime(),
  role: z.enum(["OWNER", "MEMBER"])
});

export const bootstrapStatusSchema = z.object({ required: z.boolean() });
