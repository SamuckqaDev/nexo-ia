import { z } from "zod";

export const skillEditorSchema = z.object({
  name: z.string().trim().min(3, "Name the repeatable method."),
  description: z.string().trim().min(10, "Explain when this Skill should be used."),
  scope: z.enum(["organization", "team", "project", "workspace", "personal", "session"]),
  activation: z.enum(["explicit", "suggested"]),
  instructions: z.string().trim().min(20, "Add enough instruction for a deterministic workflow."),
  outputContract: z.string().trim().min(5, "Describe the expected output."),
  dependencies: z.string().trim()
});
