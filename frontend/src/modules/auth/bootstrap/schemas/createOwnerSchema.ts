import { z } from "zod";
import { passwordSchema } from "../../../../shared/security/schemas/passwordSchema";

export const createOwnerSchema = z.object({
  name: z.string().trim().min(2, "Enter your name").max(120),
  username: z.string().trim().min(3).max(64).regex(/^[a-zA-Z0-9._-]+$/, "Use letters, numbers, dot, underscore or hyphen"),
  email: z.email("Enter a valid email address").max(254),
  password: passwordSchema,
  passwordConfirmation: z.string()
}).refine((values) => values.password === values.passwordConfirmation, {
  message: "Passwords do not match",
  path: ["passwordConfirmation"]
});
