import { z } from "zod";

export const PASSWORD_REQUIREMENTS =
  "Use 8 or more characters with uppercase, lowercase, number and special character";

export const passwordSchema = z.string()
  .min(8, "Use at least 8 characters")
  .max(128, "Use at most 128 characters")
  .regex(/\p{Lu}/u, "Add at least one uppercase letter")
  .regex(/\p{Ll}/u, "Add at least one lowercase letter")
  .regex(/\p{N}/u, "Add at least one number")
  .regex(/[^\p{L}\p{N}\s]/u, "Add at least one special character")
  .regex(/^\S+$/u, "Password cannot contain spaces");
