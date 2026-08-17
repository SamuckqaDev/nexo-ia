import { z } from "zod";
import { passwordSchema } from "../../../../shared/security/schemas/passwordSchema";

export const resetPasswordSchema = z.object({
  password: passwordSchema,
  passwordConfirmation: z.string()
}).refine((values) => values.password === values.passwordConfirmation, {
  message: "Passwords do not match",
  path: ["passwordConfirmation"]
});
