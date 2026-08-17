import { z } from "zod";
import { passwordSchema } from "../../../../shared/security/schemas/passwordSchema";

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, "Enter your current password").max(128),
  newPassword: passwordSchema,
  passwordConfirmation: z.string()
}).refine((values) => values.newPassword === values.passwordConfirmation, {
  message: "Passwords do not match",
  path: ["passwordConfirmation"]
}).refine((values) => values.currentPassword !== values.newPassword, {
  message: "The new password must be different from the current password",
  path: ["newPassword"]
});
