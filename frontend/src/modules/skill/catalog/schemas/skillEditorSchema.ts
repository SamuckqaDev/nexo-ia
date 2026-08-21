import { z } from "zod";

export const skillEditorSchema = z.object({
  name: z.string().trim().min(3, "Name the repeatable method."),
  description: z.string().trim().min(10, "Explain when this Skill should be used."),
  scope: z.enum(["organization", "team", "project", "workspace", "personal", "session"]),
  activation: z.enum(["explicit", "suggested"]),
  instructions: z.string().trim().min(20, "Add enough instruction for a deterministic workflow."),
  outputContract: z.string().trim().min(5, "Describe the expected output."),
  dependencies: z.string().trim(),
  scopeTarget: z.string().trim().optional()
}).superRefine((values, context): void => {
  if ((values.scope === "project" || values.scope === "team") && !values.scopeTarget) {
    context.addIssue({ code: "custom", path: ["scopeTarget"], message: values.scope === "project" ? "Select the project this Skill belongs to." : "Enter the team that owns this Skill." });
  }
});
