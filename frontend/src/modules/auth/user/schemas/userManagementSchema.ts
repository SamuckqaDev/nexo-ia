import { z } from "zod";
import { passwordSchema } from "../../../../shared/security/schemas/passwordSchema";

export const managedUserSchema = z.object({
  id: z.uuid(), username: z.string(), email: z.email(), name: z.string(),
  role: z.enum(["OWNER", "MEMBER"]), status: z.enum(["ACTIVE", "DISABLED"]),
  createdAt: z.iso.datetime(), updatedAt: z.iso.datetime()
});

export const managedUserListSchema = z.array(managedUserSchema);

export const createMemberSchema = z.object({
  name: z.string().trim().min(2, "Enter the member name").max(120),
  username: z.string().trim().min(3).max(64)
    .regex(/^[a-zA-Z0-9._-]+$/, "Use letters, numbers, dot, underscore or hyphen"),
  email: z.email("Enter a valid email address").max(254),
  password: passwordSchema
});
