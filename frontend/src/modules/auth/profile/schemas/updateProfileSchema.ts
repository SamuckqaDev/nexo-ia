import { z } from "zod";

export const updateProfileSchema = z.object({
  name: z.string().trim().min(2, "Enter your name").max(120),
  username: z.string().trim().min(3).max(64)
    .regex(/^[a-zA-Z0-9._-]+$/, "Use letters, numbers, dot, underscore or hyphen"),
  email: z.email("Enter a valid email address").max(254),
  birthDate: z.iso.date().nullable()
});

export type UpdateProfileFormValues = z.infer<typeof updateProfileSchema>;
