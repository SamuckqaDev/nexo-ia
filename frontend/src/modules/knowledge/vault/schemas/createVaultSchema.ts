import { z } from "zod";

export const createVaultSchema = z.object({
  name: z.string().trim().min(3, "Give the Vault a clear name."),
  description: z.string().trim().min(8, "Explain which knowledge belongs here."),
  ownerTarget: z.string().refine(
    (value: string) => value === "personal" || /^team:[0-9a-f-]{36}$/i.test(value),
    "Choose who owns this Vault."
  ),
  scope: z.enum(["personal", "workspace"]),
  workspaceId: z.string().optional()
}).refine(
  (values) => values.scope !== "workspace" || Boolean(values.workspaceId),
  { message: "Choose a workspace for this scope.", path: ["workspaceId"] }
).refine(
  (values) => !values.ownerTarget.startsWith("team:") || values.scope === "personal",
  { message: "Team knowledge uses the shared Team scope.", path: ["scope"] }
);
